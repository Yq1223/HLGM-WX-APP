package com.wool.platform.jd;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.wool.config.JdConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 京东联盟开放平台API客户端
 * 参考文档: https://union.jd.com/openplatform/api
 *
 * 已验证可用接口:
 *   - jd.union.open.goods.jingfen.query (京粉精选商品，需 router.jd.com/api)
 *   - jd.union.open.goods.query         (商品搜索，需申请权限)
 *
 * 注意: 联盟API地址为 https://router.jd.com/api，非 https://api.jd.com/routerjson
 *       时间戳格式: yyyy-MM-dd HH:mm:ss
 */
@Component
public class JdApiClient {

    private static final Logger log = LoggerFactory.getLogger(JdApiClient.class);
    private static final String API_URL = "https://router.jd.com/api";
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdConfig jdConfig;

    public JdApiClient(JdConfig jdConfig) {
        this.jdConfig = jdConfig;
    }

    /**
     * 搜索京东联盟商品（需申请权限）
     *
     * @param keyword  搜索关键词
     * @param page     页码(从1开始)
     * @param pageSize 每页数量
     * @param sortName 排序字段: price/commissionShare/inOrderCount30Days
     * @param sort     排序方向: asc/desc
     * @return 商品列表
     */
    public List<JdGoods> searchGoods(String keyword, int page, int pageSize, String sortName, String sort) {
        JSONObject goodsReq = new JSONObject();
        goodsReq.set("keyword", keyword);
        goodsReq.set("pageIndex", page);
        goodsReq.set("pageSize", pageSize);
        if (sortName != null) {
            goodsReq.set("sortName", sortName);
            goodsReq.set("sort", sort != null ? sort : "desc");
        }
        goodsReq.set("isCoupon", 1);

        JSONObject paramJson = new JSONObject();
        paramJson.set("goodsReqDTO", goodsReq);

        JSONObject result = executeApi("jd.union.open.goods.query", paramJson.toString());
        if (result == null) {
            return Collections.emptyList();
        }

        return parseQueryResult(result, "jd.union.open.goods.query");
    }

    /**
     * 获取京粉精选商品（已验证可用）
     *
     * @param eliteId  频道ID:
     *                 1-好券商品 2-精选卖场 10-9.9包邮
     *                 22-实时热销 23-大牌闪购 24-京东好物
     *                 25-实时收益
     * @param page     页码
     * @param pageSize 每页数量
     * @return 商品列表
     */
    public List<JdGoods> getEliteGoods(int eliteId, int page, int pageSize) {
        JSONObject goodsReq = new JSONObject();
        goodsReq.set("eliteId", eliteId);
        goodsReq.set("pageIndex", page);
        goodsReq.set("pageSize", pageSize);

        JSONObject paramJson = new JSONObject();
        paramJson.set("goodsReq", goodsReq);

        JSONObject result = executeApi("jd.union.open.goods.jingfen.query", paramJson.toString());
        if (result == null) {
            return Collections.emptyList();
        }

        return parseQueryResult(result, "jd.union.open.goods.jingfen.query");
    }

    /**
     * 获取京东热销商品（通过京粉实时热销频道）
     */
    public List<JdGoods> getHotGoods(int page, int pageSize) {
        return getEliteGoods(22, page, pageSize);
    }

    /**
     * 解析查询结果（兼容 jd.union.open.goods.query 和 jingfen.query）
     */
    private List<JdGoods> parseQueryResult(JSONObject result, String apiMethod) {
        // 响应key可能是 queryResult 或 jingfenQueryResult
        JSONObject queryResult = result.getJSONObject("queryResult");
        if (queryResult == null) {
            queryResult = result.getJSONObject("jingfenQueryResult");
        }
        if (queryResult == null) {
            // 尝试从 result 字段解析（可能是JSON字符串）
            String resultStr = result.getStr("result");
            if (resultStr != null) {
                try {
                    queryResult = JSONUtil.parseObj(resultStr);
                } catch (Exception e) {
                    log.warn("[JD] 解析result字段失败: {}", apiMethod);
                }
            }
        }
        if (queryResult == null) {
            log.warn("[JD] {} 返回结果为空", apiMethod);
            return Collections.emptyList();
        }

        Integer code = queryResult.getInt("code");
        if (code != null && code != 200 && code != 0) {
            log.error("[JD] {} 业务错误: code={}, message={}", apiMethod, code, queryResult.getStr("message"));
            return Collections.emptyList();
        }

        JSONArray dataList = queryResult.getJSONArray("data");
        if (dataList == null || dataList.isEmpty()) {
            log.info("[JD] {} 返回0条数据", apiMethod);
            return Collections.emptyList();
        }

        List<JdGoods> goods = new ArrayList<>();
        for (int i = 0; i < dataList.size(); i++) {
            JSONObject item = dataList.getJSONObject(i);
            JdGoods g = parseGoods(item);
            if (g != null) {
                goods.add(g);
            }
        }
        log.info("[JD] {}: 返回{}条商品", apiMethod, goods.size());
        return goods;
    }

    /**
     * 解析单个商品JSON
     *
     * 实际响应字段:
     *   skuId (long), skuName, skuDesc
     *   imageInfo: {imageList: [{url: "..."}]}
     *   priceInfo: {price, lowestPrice, lowestCouponPrice, priceType}
     *   commissionInfo: {commission, commissionShare, couponCommission, plusCommissionShare}
     *   couponInfo: {couponList: [{discount, link, getStartTime, getEndTime, quota, bindType}]}
     *   shopInfo: {shopName, shopId}
     *   brandInfo: {brandName, brandCode}
     *   categoryInfo: {cid1, cid1Name, cid2, cid2Name, cid3, cid3Name}
     *   inOrderCount30Days, goodCommentsShare, comments
     *   owner (g-自营, p-POP)
     *   materialUrl (推广链接)
     */
    private JdGoods parseGoods(JSONObject item) {
        try {
            JdGoods goods = new JdGoods();
            // skuId可能不存在于京粉接口，用itemId或spuid替代
            String skuId = item.getStr("skuId");
            if (skuId == null || skuId.isEmpty() || "null".equals(skuId)) {
                skuId = item.getStr("itemId"); // 京粉接口用itemId
            }
            if (skuId == null || skuId.isEmpty()) {
                skuId = item.getStr("spuid", "");
            }
            goods.setSkuId(skuId);
            goods.setItemId(item.getStr("itemId"));
            goods.setSpuid(item.getStr("spuid"));
            goods.setSkuName(item.getStr("skuName"));
            goods.setSkuDesc(item.getStr("skuDesc"));
            goods.setMaterialUrl(item.getStr("materialUrl"));
            goods.setOwner(item.getStr("owner"));

            // 图片信息
            JSONObject imageInfo = item.getJSONObject("imageInfo");
            if (imageInfo != null) {
                JSONArray imageList = imageInfo.getJSONArray("imageList");
                if (imageList != null && !imageList.isEmpty()) {
                    goods.setImageUrl(imageList.getJSONObject(0).getStr("url"));
                }
            }

            // 价格信息
            JSONObject priceInfo = item.getJSONObject("priceInfo");
            if (priceInfo != null) {
                goods.setPrice(priceInfo.getBigDecimal("price"));
                goods.setLowestPrice(priceInfo.getBigDecimal("lowestPrice"));
                goods.setLowestCouponPrice(priceInfo.getBigDecimal("lowestCouponPrice"));
            }

            // 佣金信息
            JSONObject commissionInfo = item.getJSONObject("commissionInfo");
            if (commissionInfo != null) {
                goods.setCommissionShare(commissionInfo.getBigDecimal("commissionShare"));
                goods.setCouponCommission(commissionInfo.getBigDecimal("couponCommission"));
            }

            // 优惠券信息
            JSONObject couponInfo = item.getJSONObject("couponInfo");
            if (couponInfo != null) {
                JSONArray couponList = couponInfo.getJSONArray("couponList");
                if (couponList != null && !couponList.isEmpty()) {
                    // 选择最优优惠券（discount最大的）
                    JSONObject bestCoupon = null;
                    for (int j = 0; j < couponList.size(); j++) {
                        JSONObject coupon = couponList.getJSONObject(j);
                        if (bestCoupon == null || coupon.getBigDecimal("discount").compareTo(bestCoupon.getBigDecimal("discount")) > 0) {
                            bestCoupon = coupon;
                        }
                    }
                    if (bestCoupon != null) {
                        goods.setCouponDiscount(bestCoupon.getBigDecimal("discount"));
                        goods.setCouponLink(bestCoupon.getStr("link"));
                        goods.setCouponStartTime(bestCoupon.getStr("getStartTime"));
                        goods.setCouponEndTime(bestCoupon.getStr("getEndTime"));
                    }
                }
            }

            // 销量
            goods.setInOrderCount30Days(item.getInt("inOrderCount30Days"));

            // 店铺信息
            JSONObject shopInfo = item.getJSONObject("shopInfo");
            if (shopInfo != null) {
                goods.setShopName(shopInfo.getStr("shopName"));
            }

            // 品牌
            JSONObject brandInfo = item.getJSONObject("brandInfo");
            if (brandInfo != null) {
                goods.setBrandName(brandInfo.getStr("brandName"));
            }

            return goods;
        } catch (Exception e) {
            log.error("[JD] 解析商品异常: skuId={}", item.get("skuId"), e);
            return null;
        }
    }

    /**
     * 执行京东API调用（含签名）
     *
     * @param method    API方法名
     * @param paramJson 业务参数JSON字符串
     * @return 响应结果
     */
    private JSONObject executeApi(String method, String paramJson) {
        try {
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);

            // 系统参数
            Map<String, String> sysParams = new LinkedHashMap<>();
            sysParams.put("method", method);
            sysParams.put("app_key", jdConfig.getAppKey());
            sysParams.put("timestamp", timestamp);
            sysParams.put("format", "json");
            sysParams.put("v", "1.0");
            sysParams.put("sign_method", "md5");
            sysParams.put("param_json", paramJson);

            // 生成签名
            String sign = generateSign(sysParams, jdConfig.getSecretKey());
            sysParams.put("sign", sign);

            // 构建请求（转为 Map<String, Object> 以兼容 Hutool form 方法）
            Map<String, Object> formParams = new LinkedHashMap<>(sysParams);
            HttpResponse response = HttpRequest.post(API_URL)
                    .form(formParams)
                    .timeout(30000)
                    .execute();

            String responseBody = response.body();
            log.debug("[JD] API响应 [{}]: {}", method, responseBody);

            if (response.getStatus() != 200) {
                log.error("[JD] API请求失败: method={}, status={}, body={}", method, response.getStatus(), responseBody);
                return null;
            }

            JSONObject json = JSONUtil.parseObj(responseBody);

            // 查找响应key（格式: xxx_response 或 xxx_responce）
            JSONObject responseObj = null;
            for (String key : json.keySet()) {
                if (key.endsWith("_response") || key.endsWith("_responce")) {
                    responseObj = json.getJSONObject(key);
                    break;
                }
            }

            if (responseObj == null) {
                JSONObject errorResponse = json.getJSONObject("error_response");
                if (errorResponse != null) {
                    log.error("[JD] API错误: code={}, desc={}",
                            errorResponse.getInt("code"), errorResponse.getStr("zh_desc"));
                } else {
                    log.error("[JD] API未知响应格式: {}", responseBody);
                }
                return null;
            }

            // 检查外层业务码
            Integer code = responseObj.getInt("code");
            if (code != null && code != 0) {
                String msg = responseObj.getStr("zh_desc", responseObj.getStr("message", ""));
                log.error("[JD] API错误: method={}, code={}, msg={}", method, code, msg);
                return null;
            }

            return responseObj;
        } catch (Exception e) {
            log.error("[JD] API调用异常: method={}", method, e);
            return null;
        }
    }

    /**
     * 生成京东API签名
     * 签名规则: secret + 所有参数按key排序拼接(key+value) + secret, MD5后转大写
     */
    private String generateSign(Map<String, String> params, String secret) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder(secret);
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (entry.getValue() != null) {
                sb.append(entry.getKey()).append(entry.getValue());
            }
        }
        sb.append(secret);
        return DigestUtil.md5Hex(sb.toString()).toUpperCase();
    }
}
