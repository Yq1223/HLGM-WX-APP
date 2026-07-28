package com.wool.platform.pdd;

import java.math.BigDecimal;
import java.util.List;

/**
 * 拼多多商品数据模型
 */
public class PddGoods {

    private String goodsId;
    private String goodsName;
    private String goodsDesc;
    private String goodsImageUrl;
    private String goodsThumbnailUrl;
    private BigDecimal minGroupPrice;       // 最小团购价(分)
    private BigDecimal minNormalPrice;      // 最小单买价(分)
    private BigDecimal couponDiscount;      // 优惠券面额(分)
    private BigDecimal couponMinOrderAmount; // 优惠券最低使用金额(分)
    private Integer promotionRate;          // 佣金比例(千分比, 如250=25%)
    private String salesTip;                // 销量描述
    private String goodsDetailUrl;          // 商品详情URL
    private String couponId;                // 优惠券ID
    private Long couponStartTime;           // 优惠券开始时间(秒)
    private Long couponEndTime;             // 优惠券结束时间(秒)
    private Integer couponRemainQuantity;   // 优惠券剩余数量
    private List<String> serviceTags;       // 服务标签
    private String goodsSign;                // 商品签名（用于详情查询）
    private Boolean hasCoupon;               // 是否有优惠券
    private String categoryName;             // 分类名称
    private String optName;                  // 运营类目名

    // Getters & Setters
    public String getGoodsId() { return goodsId; }
    public void setGoodsId(String goodsId) { this.goodsId = goodsId; }

    public String getGoodsName() { return goodsName; }
    public void setGoodsName(String goodsName) { this.goodsName = goodsName; }

    public String getGoodsDesc() { return goodsDesc; }
    public void setGoodsDesc(String goodsDesc) { this.goodsDesc = goodsDesc; }

    public String getGoodsImageUrl() { return goodsImageUrl; }
    public void setGoodsImageUrl(String goodsImageUrl) { this.goodsImageUrl = goodsImageUrl; }

    public String getGoodsThumbnailUrl() { return goodsThumbnailUrl; }
    public void setGoodsThumbnailUrl(String goodsThumbnailUrl) { this.goodsThumbnailUrl = goodsThumbnailUrl; }

    public BigDecimal getMinGroupPrice() { return minGroupPrice; }
    public void setMinGroupPrice(BigDecimal minGroupPrice) { this.minGroupPrice = minGroupPrice; }

    public BigDecimal getMinNormalPrice() { return minNormalPrice; }
    public void setMinNormalPrice(BigDecimal minNormalPrice) { this.minNormalPrice = minNormalPrice; }

    public BigDecimal getCouponDiscount() { return couponDiscount; }
    public void setCouponDiscount(BigDecimal couponDiscount) { this.couponDiscount = couponDiscount; }

    public BigDecimal getCouponMinOrderAmount() { return couponMinOrderAmount; }
    public void setCouponMinOrderAmount(BigDecimal couponMinOrderAmount) { this.couponMinOrderAmount = couponMinOrderAmount; }

    public Integer getPromotionRate() { return promotionRate; }
    public void setPromotionRate(Integer promotionRate) { this.promotionRate = promotionRate; }

    public String getSalesTip() { return salesTip; }
    public void setSalesTip(String salesTip) { this.salesTip = salesTip; }

    public String getGoodsDetailUrl() { return goodsDetailUrl; }
    public void setGoodsDetailUrl(String goodsDetailUrl) { this.goodsDetailUrl = goodsDetailUrl; }

    public String getCouponId() { return couponId; }
    public void setCouponId(String couponId) { this.couponId = couponId; }

    public Long getCouponStartTime() { return couponStartTime; }
    public void setCouponStartTime(Long couponStartTime) { this.couponStartTime = couponStartTime; }

    public Long getCouponEndTime() { return couponEndTime; }
    public void setCouponEndTime(Long couponEndTime) { this.couponEndTime = couponEndTime; }

    public Integer getCouponRemainQuantity() { return couponRemainQuantity; }
    public void setCouponRemainQuantity(Integer couponRemainQuantity) { this.couponRemainQuantity = couponRemainQuantity; }

    public List<String> getServiceTags() { return serviceTags; }
    public void setServiceTags(List<String> serviceTags) { this.serviceTags = serviceTags; }

    public String getGoodsSign() { return goodsSign; }
    public void setGoodsSign(String goodsSign) { this.goodsSign = goodsSign; }

    public Boolean getHasCoupon() { return hasCoupon; }
    public void setHasCoupon(Boolean hasCoupon) { this.hasCoupon = hasCoupon; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getOptName() { return optName; }
    public void setOptName(String optName) { this.optName = optName; }

    /**
     * 获取券后价(元) = (团购价 - 优惠券面额) / 100
     */
    public BigDecimal getCouponPrice() {
        BigDecimal price = minGroupPrice != null ? minGroupPrice : BigDecimal.ZERO;
        BigDecimal coupon = couponDiscount != null ? couponDiscount : BigDecimal.ZERO;
        return price.subtract(coupon).max(BigDecimal.ZERO).divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 获取原价(元)
     */
    public BigDecimal getOriginalPriceYuan() {
        BigDecimal price = minNormalPrice != null ? minNormalPrice : (minGroupPrice != null ? minGroupPrice : BigDecimal.ZERO);
        return price.divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 获取佣金比例(%)
     */
    public double getCommissionRatePercent() {
        if (promotionRate == null) return 0;
        return promotionRate / 10.0; // 千分比转百分比
    }
}
