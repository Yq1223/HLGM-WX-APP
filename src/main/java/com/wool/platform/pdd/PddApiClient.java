package com.wool.platform.pdd;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.wool.config.PddConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 拼多多开放平台API客户端
 * 参考文档: https://open.pinduoduo.com/
 *
 * 已验证可用接口:
 *   - pdd.ddk.goods.recommend.get  (推荐商品，无需PID，无需access_token)
 *   - pdd.ddk.goods.search         (搜索商品，需配置PID)
 *   - pdd.ddk.goods.detail         (商品详情，需goods_sign)
 *
 * 已下线接口:
 *   - pdd.ddk.top.goods.list.query (已下线)
 */
@Component
public class PddApiClient {

    private static final Logger log = LoggerFactory.getLogger(PddApiClient.class);
    private static final String API_URL = "https://gw-api.pinduoduo.com/api/router";

    private final PddConfig pddConfig;

    public PddApiClient(PddConfig pddConfig) {
        this.pddConfig = pddConfig;
    }

    /**
     * 获取推荐商品（无需PID和access_token）
     *
     * @param offset      偏移量
     * @param limit       数量(最大100)
     * @param channelType 频道类型: 0-1.9包邮 1-今日爆款 2-品牌好货 3-相似商品 4-猜你喜欢 5-实时热销 6-实时收益
     * @return 商品列表
     */
    public List<PddGoods> getRecommendGoods(int offset, int limit, int channelType) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("type", "pdd.ddk.goods.recommend.get");
        params.put("offset", String.valueOf(offset));
        params.put("limit", String.valueOf(Math.min(limit, 100)));
        params.put("channel_type", String.valueOf(channelType));

        JSONObject result = executeApi(params);
        if (result == null) {
            return Collections.emptyList();
        }

        JSONObject response = result.getJSONObject("goods_basic_detail_response");
        if (response == null) {
            log.warn("PDD推荐商品返回为空");
            return Collections.emptyList();
        }

        JSONArray goodsList = response.getJSONArray("list");
        if (goodsList == null || goodsList.isEmpty()) {
            log.info("PDD推荐商品: offset={}, 返回0条", offset);
            return Collections.emptyList();
        }

        List<PddGoods> goods = new ArrayList<>();
        for (int i = 0; i < goodsList.size(); i++) {
            JSONObject item = goodsList.getJSONObject(i);
            PddGoods g = parseRecommendGoods(item);
            if (g != null) {
                goods.add(g);
            }
        }
        log.info("PDD推荐商品: offset={}, channelType={}, 返回{}条", offset, channelType, goods.size());
        return goods;
    }

    /**
     * 搜索多多进宝商品（需要配置PID）
     *
     * @param keyword   搜索关键词
     * @param page      页码(从1开始)
     * @param pageSize  每页数量(10-100)
     * @param sortType  排序: 0-综合 1-佣金升序 2-佣金降序 3-价格升序 4-价格降序 6-销量降序
     * @return 商品列表
     */
    public List<PddGoods> searchGoods(String keyword, int page, int pageSize, int sortType) {
        if (pddConfig.getPid() == null || pddConfig.getPid().isEmpty()) {
            log.debug("PDD未配置PID，跳过搜索接口");
            return Collections.emptyList();
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("type", "pdd.ddk.goods.search");
        params.put("keyword", keyword);
        params.put("page", String.valueOf(page));
        params.put("page_size", String.valueOf(Math.max(10, Math.min(pageSize, 100))));
        params.put("sort_type", String.valueOf(sortType));
        params.put("pid", pddConfig.getPid());

        JSONObject result = executeApi(params);
        if (result == null) {
            return Collections.emptyList();
        }

        JSONObject goodsSearchResult = result.getJSONObject("goods_search_response");
        if (goodsSearchResult == null) {
            log.warn("PDD搜索商品返回为空: keyword={}", keyword);
            return Collections.emptyList();
        }

        JSONArray goodsList = goodsSearchResult.getJSONArray("goods_list");
        if (goodsList == null || goodsList.isEmpty()) {
            return Collections.emptyList();
        }

        List<PddGoods> goods = new ArrayList<>();
        for (int i = 0; i < goodsList.size(); i++) {
            JSONObject item = goodsList.getJSONObject(i);
            PddGoods g = parseSearchGoods(item);
            if (g != null) {
                goods.add(g);
            }
        }
        log.info("PDD搜索商品: keyword={}, page={}, 返回{}条", keyword, page, goods.size());
        return goods;
    }

    /**
     * 解析推荐商品JSON（pdd.ddk.goods.recommend.get 响应格式）
     *
     * 实际响应字段（扁平结构）:
     *   goods_id (int), goods_name, goods_desc, goods_image_url, goods_thumbnail_url
     *   min_group_price (int, 分), min_normal_price (int, 分)
     *   promotion_rate (int, ‰), sales_tip
     *   has_coupon (bool), coupon_discount (int, 分), coupon_remain_quantity (int)
     *   coupon_min_order_amount (int, 分), coupon_start_time, coupon_end_time
     *   goods_sign, unified_tags (string[]), category_name, cat_id
     */
    private PddGoods parseRecommendGoods(JSONObject item) {
        try {
            PddGoods goods = new PddGoods();
            // goods_id 可能是int或string
            goods.setGoodsId(String.valueOf(item.get("goods_id")));
            goods.setGoodsName(item.getStr("goods_name"));
            goods.setGoodsDesc(item.getStr("goods_desc"));
            goods.setGoodsImageUrl(item.getStr("goods_image_url"));
            goods.setGoodsThumbnailUrl(item.getStr("goods_thumbnail_url"));
            goods.setGoodsSign(item.getStr("goods_sign"));

            // 价格（分）
            goods.setMinGroupPrice(item.getBigDecimal("min_group_price"));
            goods.setMinNormalPrice(item.getBigDecimal("min_normal_price"));

            // 佣金（千分比）
            goods.setPromotionRate(item.getInt("promotion_rate"));

            // 销量
            goods.setSalesTip(item.getStr("sales_tip"));

            // 优惠券（扁平字段，非嵌套coupon_list）
            goods.setCouponDiscount(item.getBigDecimal("coupon_discount"));
            goods.setCouponRemainQuantity(item.getInt("coupon_remain_quantity"));
            goods.setCouponMinOrderAmount(item.getBigDecimal("coupon_min_order_amount"));
            goods.setCouponStartTime(item.getLong("coupon_start_time"));
            goods.setCouponEndTime(item.getLong("coupon_end_time"));
            goods.setHasCoupon(item.getBool("has_coupon", false));

            // 标签
            JSONArray unifiedTags = item.getJSONArray("unified_tags");
            if (unifiedTags != null) {
                List<String> tags = new ArrayList<>();
                for (int j = 0; j < unifiedTags.size(); j++) {
                    tags.add(unifiedTags.getStr(j));
                }
                goods.setServiceTags(tags);
            }

            // 分类
            goods.setCategoryName(item.getStr("category_name"));
            goods.setOptName(item.getStr("opt_name"));

            return goods;
        } catch (Exception e) {
            log.error("解析PDD推荐商品异常: goods_id={}", item.get("goods_id"), e);
            return null;
        }
    }

    /**
     * 解析搜索商品JSON（pdd.ddk.goods.search 响应格式）
     */
    private PddGoods parseSearchGoods(JSONObject item) {
        try {
            PddGoods goods = new PddGoods();
            goods.setGoodsId(String.valueOf(item.get("goods_id")));
            goods.setGoodsName(item.getStr("goods_name"));
            goods.setGoodsDesc(item.getStr("goods_desc"));
            goods.setGoodsImageUrl(item.getStr("goods_image_url"));
            goods.setGoodsThumbnailUrl(item.getStr("goods_thumbnail_url"));
            goods.setGoodsSign(item.getStr("goods_sign"));
            goods.setGoodsDetailUrl(item.getStr("goods_detail_url"));

            goods.setMinGroupPrice(item.getBigDecimal("min_group_price"));
            goods.setMinNormalPrice(item.getBigDecimal("min_normal_price"));
            goods.setPromotionRate(item.getInt("promotion_rate"));
            goods.setSalesTip(item.getStr("sales_tip"));

            // 搜索结果中优惠券可能在coupon_list嵌套数组中
            JSONArray couponList = item.getJSONArray("coupon_list");
            if (couponList != null && !couponList.isEmpty()) {
                JSONObject coupon = couponList.getJSONObject(0);
                goods.setCouponDiscount(coupon.getBigDecimal("coupon_discount"));
                goods.setCouponRemainQuantity(coupon.getInt("coupon_remain_quantity"));
                goods.setCouponMinOrderAmount(coupon.getBigDecimal("coupon_min_order_amount"));
                goods.setCouponStartTime(coupon.getLong("coupon_start_time"));
                goods.setCouponEndTime(coupon.getLong("coupon_end_time"));
                goods.setHasCoupon(true);
            } else {
                // 也可能在扁平字段中
                goods.setCouponDiscount(item.getBigDecimal("coupon_discount"));
                goods.setCouponRemainQuantity(item.getInt("coupon_remain_quantity"));
                goods.setCouponMinOrderAmount(item.getBigDecimal("coupon_min_order_amount"));
                goods.setHasCoupon(item.getBool("has_coupon", false));
            }

            // 标签
            JSONArray unifiedTags = item.getJSONArray("unified_tags");
            if (unifiedTags != null) {
                List<String> tags = new ArrayList<>();
                for (int j = 0; j < unifiedTags.size(); j++) {
                    tags.add(unifiedTags.getStr(j));
                }
                goods.setServiceTags(tags);
            }

            goods.setCategoryName(item.getStr("category_name"));
            goods.setOptName(item.getStr("opt_name"));

            return goods;
        } catch (Exception e) {
            log.error("解析PDD搜索商品异常: goods_id={}", item.get("goods_id"), e);
            return null;
        }
    }

    /**
     * 执行PDD API调用（含签名）
     */
    private JSONObject executeApi(Map<String, String> bizParams) {
        try {
            // 公共参数
            Map<String, String> allParams = new LinkedHashMap<>();
            allParams.put("client_id", pddConfig.getClientId());
            allParams.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
            allParams.put("data_type", "JSON");
            if (pddConfig.getAccessToken() != null && !pddConfig.getAccessToken().isEmpty()) {
                allParams.put("access_token", pddConfig.getAccessToken());
            }
            allParams.putAll(bizParams);

            // 生成签名
            String sign = generateSign(allParams, pddConfig.getClientSecret());
            allParams.put("sign", sign);

            // 发送请求
            String requestBody = JSONUtil.toJsonStr(allParams);
            log.debug("PDD API请求: {}", requestBody);

            HttpResponse response = HttpRequest.post(API_URL)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .timeout(30000)
                    .execute();

            String responseBody = response.body();
            log.debug("PDD API响应: {}", responseBody);

            if (response.getStatus() != 200) {
                log.error("PDD API请求失败: status={}, body={}", response.getStatus(), responseBody);
                return null;
            }

            JSONObject json = JSONUtil.parseObj(responseBody);
            if (json.containsKey("error_response")) {
                JSONObject error = json.getJSONObject("error_response");
                log.error("PDD API错误: errorCode={}, errorMsg={}, subMsg={}",
                        error.getInt("error_code"), error.getStr("error_msg"), error.getStr("sub_msg"));
                return null;
            }

            return json;
        } catch (Exception e) {
            log.error("PDD API调用异常", e);
            return null;
        }
    }

    /**
     * 生成PDD API签名
     * 签名规则: 将所有参数按key排序拼接, 首尾拼接client_secret, MD5后转大写
     */
    private String generateSign(Map<String, String> params, String clientSecret) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder(clientSecret);
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                sb.append(entry.getKey()).append(entry.getValue());
            }
        }
        sb.append(clientSecret);
        return DigestUtil.md5Hex(sb.toString()).toUpperCase();
    }
}
