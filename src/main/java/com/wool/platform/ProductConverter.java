package com.wool.platform;

import cn.hutool.json.JSONUtil;
import com.wool.entity.WoolInfo;
import com.wool.platform.jd.JdGoods;
import com.wool.platform.pdd.PddGoods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 商品数据转换器
 * 将拼多多/京东API返回的商品数据转换为WoolInfo实体
 *
 * 字段映射规则（不修改数据库结构）：
 *   title       → 商品标题
 *   content     → JSON格式存储: {imageUrl, originalPrice, couponPrice, commissionRate, couponAmount, remainQuantity, platform, platformGoodsId, tags}
 *   category    → "pdd" 或 "jd"
 *   source_url  → 商品推广链接
 *   claim_steps → 活动标签/佣金信息等补充描述
 *   status      → 1 (已上线，默认审核通过)
 *   user_id     → 系统管理员ID（由调用方传入）
 */
@Component
public class ProductConverter {

    private static final Logger log = LoggerFactory.getLogger(ProductConverter.class);

    /**
     * 将拼多多商品转换为WoolInfo
     *
     * @param goods    拼多多商品
     * @param userId   系统管理员ID
     * @return WoolInfo实体，如果数据不合法返回null
     */
    public WoolInfo convertPddGoods(PddGoods goods, Long userId) {
        if (goods == null || goods.getGoodsId() == null) {
            return null;
        }

        try {
            WoolInfo info = new WoolInfo();
            info.setUserId(userId);
            info.setTitle(truncate(goods.getGoodsName(), 128));
            info.setCategory(guessCategory(goods.getGoodsName(), goods.getServiceTags()));

            // content: JSON格式存储结构化数据
            Map<String, Object> contentMap = new LinkedHashMap<>();
            contentMap.put("platform", "pdd");
            contentMap.put("platformGoodsId", goods.getGoodsId());
            contentMap.put("imageUrl", goods.getGoodsImageUrl());
            contentMap.put("originalPrice", goods.getOriginalPriceYuan());
            contentMap.put("couponPrice", goods.getCouponPrice());
            contentMap.put("commissionRate", goods.getCommissionRatePercent());
            contentMap.put("couponAmount", goods.getCouponDiscount() != null
                    ? goods.getCouponDiscount().divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP)
                    : BigDecimal.ZERO);
            contentMap.put("remainQuantity", goods.getCouponRemainQuantity());
            contentMap.put("salesTip", goods.getSalesTip());
            if (goods.getServiceTags() != null && !goods.getServiceTags().isEmpty()) {
                contentMap.put("tags", goods.getServiceTags());
            }
            info.setContent(JSONUtil.toJsonStr(contentMap));

            // source_url: 推广链接
            info.setSourceUrl(goods.getGoodsDetailUrl() != null ? goods.getGoodsDetailUrl() : "");

            // claim_steps: 领券和佣金信息
            StringBuilder steps = new StringBuilder();
            if (goods.getCouponDiscount() != null && goods.getCouponDiscount().compareTo(BigDecimal.ZERO) > 0) {
                steps.append("优惠券面额: ¥")
                     .append(goods.getCouponDiscount().divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP));
                if (goods.getCouponMinOrderAmount() != null) {
                    steps.append(" (满")
                         .append(goods.getCouponMinOrderAmount().divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP))
                         .append("可用)");
                }
                steps.append("\n");
            }
            if (goods.getPromotionRate() != null) {
                steps.append("佣金比例: ").append(String.format("%.1f%%", goods.getCommissionRatePercent())).append("\n");
            }
            if (goods.getCouponRemainQuantity() != null) {
                steps.append("剩余券数: ").append(goods.getCouponRemainQuantity()).append("\n");
            }
            info.setClaimSteps(steps.toString().trim());

            info.setStatus(1); // 默认已上线
            info.setViewCount(0);

            return info;
        } catch (Exception e) {
            log.error("PDD商品转换失败: goodsId={}", goods.getGoodsId(), e);
            return null;
        }
    }

    /**
     * 将京东商品转换为WoolInfo
     *
     * @param goods    京东商品
     * @param userId   系统管理员ID
     * @return WoolInfo实体，如果数据不合法返回null
     */
    public WoolInfo convertJdGoods(JdGoods goods, Long userId) {
        if (goods == null || goods.getSkuId() == null) {
            return null;
        }

        try {
            WoolInfo info = new WoolInfo();
            info.setUserId(userId);
            info.setTitle(truncate(goods.getSkuName(), 128));
            info.setCategory(guessCategory(goods.getSkuName(), goods.getTags()));

            // content: JSON格式存储结构化数据
            Map<String, Object> contentMap = new LinkedHashMap<>();
            contentMap.put("platform", "jd");
            contentMap.put("platformGoodsId", goods.getSkuId());
            if (goods.getItemId() != null) {
                contentMap.put("itemId", goods.getItemId());
            }
            if (goods.getSpuid() != null) {
                contentMap.put("spuid", goods.getSpuid());
            }
            contentMap.put("imageUrl", extractFirstImage(goods.getImageUrl()));
            contentMap.put("originalPrice", goods.getOriginalPrice());
            contentMap.put("couponPrice", goods.getCouponPrice());
            contentMap.put("commissionRate", goods.getCommissionRatePercent());
            contentMap.put("couponAmount", goods.getCouponDiscount() != null ? goods.getCouponDiscount() : BigDecimal.ZERO);
            contentMap.put("remainQuantity", goods.getCouponRemainNum());
            contentMap.put("shopName", goods.getShopName());
            contentMap.put("brandName", goods.getBrandName());
            contentMap.put("inOrderCount30Days", goods.getInOrderCount30Days());
            contentMap.put("owner", goods.getOwner());
            if (goods.getTags() != null && !goods.getTags().isEmpty()) {
                contentMap.put("tags", goods.getTags());
            }
            info.setContent(JSONUtil.toJsonStr(contentMap));

            // source_url: 推广链接
            info.setSourceUrl(goods.getMaterialUrl() != null ? goods.getMaterialUrl() : "");

            // claim_steps: 领券和佣金信息
            StringBuilder steps = new StringBuilder();
            if (goods.getCouponDiscount() != null && goods.getCouponDiscount().compareTo(BigDecimal.ZERO) > 0) {
                steps.append("优惠券面额: ¥").append(goods.getCouponDiscount());
                steps.append("\n");
            }
            if (goods.getCommissionShare() != null) {
                steps.append("佣金比例: ").append(String.format("%.1f%%", goods.getCommissionRatePercent())).append("\n");
            }
            if (goods.getCouponRemainNum() != null) {
                steps.append("剩余券数: ").append(goods.getCouponRemainNum()).append("\n");
            }
            if (goods.getShopName() != null && !goods.getShopName().isEmpty()) {
                steps.append("店铺: ").append(goods.getShopName()).append("\n");
            }
            info.setClaimSteps(steps.toString().trim());

            info.setStatus(1); // 默认已上线
            info.setViewCount(0);

            return info;
        } catch (Exception e) {
            log.error("JD商品转换失败: skuId={}", goods.getSkuId(), e);
            return null;
        }
    }

    /**
     * 批量转换拼多多商品
     */
    public List<WoolInfo> convertPddGoodsList(List<PddGoods> goodsList, Long userId) {
        List<WoolInfo> result = new ArrayList<>();
        for (PddGoods goods : goodsList) {
            WoolInfo info = convertPddGoods(goods, userId);
            if (info != null) {
                result.add(info);
            }
        }
        return result;
    }

    /**
     * 批量转换京东商品
     */
    public List<WoolInfo> convertJdGoodsList(List<JdGoods> goodsList, Long userId) {
        List<WoolInfo> result = new ArrayList<>();
        for (JdGoods goods : goodsList) {
            WoolInfo info = convertJdGoods(goods, userId);
            if (info != null) {
                result.add(info);
            }
        }
        return result;
    }

    /**
     * 从content JSON中提取platformGoodsId
     */
    public static String extractPlatformGoodsId(String content) {
        try {
            if (content == null || content.isEmpty()) return null;
            return cn.hutool.json.JSONUtil.parseObj(content).getStr("platformGoodsId");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从content JSON中提取platform
     */
    public static String extractPlatform(String content) {
        try {
            if (content == null || content.isEmpty()) return null;
            return cn.hutool.json.JSONUtil.parseObj(content).getStr("platform");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 根据商品名称和标签智能分类
     * 映射到用户可见的分类: 会员, 话费, 外卖, 电商, 生活, 出行, 美食, 其他
     */
    public static String guessCategory(String goodsName, List<String> tags) {
        String text = (goodsName != null ? goodsName : "") + " " + (tags != null ? String.join(" ", tags) : "");
        text = text.toLowerCase();

        // 会员
        if (text.contains("会员") || text.contains("vip") || text.contains("视频会员") || text.contains("音乐会员") || text.contains("网盘") || text.contains("wps")) {
            return "会员";
        }
        // 话费
        if (text.contains("话费") || text.contains("充值") || text.contains("流量") || text.contains("移动") || text.contains("联通") || text.contains("电信")) {
            return "话费";
        }
        // 外卖
        if (text.contains("外卖") || text.contains("美团") || text.contains("饿了么") || text.contains("瑞幸") || text.contains("星巴克") || text.contains("奶茶")) {
            return "外卖";
        }
        // 美食
        if (text.contains("零食") || text.contains("食品") || text.contains("坚果") || text.contains("饼干") || text.contains("巧克力") || text.contains("糖果") || text.contains("饮料") || text.contains("牛奶") || text.contains("面包") || text.contains("蛋糕")) {
            return "美食";
        }
        // 出行
        if (text.contains("打车") || text.contains("滴滴") || text.contains("高德") || text.contains("机票") || text.contains("火车票") || text.contains("酒店") || text.contains("民宿") || text.contains("加油")) {
            return "出行";
        }
        // 生活
        if (text.contains("纸巾") || text.contains("洗衣") || text.contains("牙膏") || text.contains("洗发") || text.contains("垃圾袋") || text.contains("保鲜膜") || text.contains("收纳") || text.contains("清洁") || text.contains("垃圾") || text.contains("家政")) {
            return "生活";
        }
        // 电商（默认电商平台商品）
        return "电商";
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen) : str;
    }

    /**
     * 从京东图片信息中提取第一张图片URL
     * 京东的imageInfo可能是JSON数组格式
     */
    private String extractFirstImage(String imageInfo) {
        if (imageInfo == null || imageInfo.isEmpty()) return "";
        try {
            if (imageInfo.startsWith("[")) {
                cn.hutool.json.JSONArray arr = cn.hutool.json.JSONUtil.parseArray(imageInfo);
                if (!arr.isEmpty()) {
                    cn.hutool.json.JSONObject first = arr.getJSONObject(0);
                    return first.getStr("url", first.getStr("imgUrl", imageInfo));
                }
            }
            return imageInfo;
        } catch (Exception e) {
            return imageInfo;
        }
    }
}
