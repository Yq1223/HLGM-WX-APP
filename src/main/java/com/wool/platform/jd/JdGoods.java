package com.wool.platform.jd;

import java.math.BigDecimal;
import java.util.List;

/**
 * 京东商品数据模型
 */
public class JdGoods {

    private String skuId;
    private String skuName;
    private String skuDesc;
    private String imageUrl;
    private String materialUrl;         // 推广链接
    private BigDecimal price;           // 价格(元)
    private BigDecimal lowestPrice;     // 最低价(元)
    private BigDecimal lowestCouponPrice; // 最低券后价(元)
    private BigDecimal commissionShare;  // 佣金比例(%)
    private BigDecimal couponCommission; // 券佣金(元)
    private String couponId;             // 优惠券ID
    private BigDecimal couponDiscount;   // 优惠券面额(元)
    private String couponLink;           // 领券链接
    private String couponStartTime;
    private String couponEndTime;
    private Integer couponRemainNum;     // 优惠券剩余数量
    private Integer inOrderCount30Days;  // 30天引单数
    private String shopName;             // 店铺名称
    private String brandName;            // 品牌名称
    private List<String> tags;           // 标签
    private String owner;                // 商品owner: g-自营, p-pop
    private String itemId;               // 京粉接口的商品ID
    private String spuid;                // SPU ID

    // Getters & Setters
    public String getSkuId() { return skuId; }
    public void setSkuId(String skuId) { this.skuId = skuId; }

    public String getSkuName() { return skuName; }
    public void setSkuName(String skuName) { this.skuName = skuName; }

    public String getSkuDesc() { return skuDesc; }
    public void setSkuDesc(String skuDesc) { this.skuDesc = skuDesc; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getMaterialUrl() { return materialUrl; }
    public void setMaterialUrl(String materialUrl) { this.materialUrl = materialUrl; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getLowestPrice() { return lowestPrice; }
    public void setLowestPrice(BigDecimal lowestPrice) { this.lowestPrice = lowestPrice; }

    public BigDecimal getLowestCouponPrice() { return lowestCouponPrice; }
    public void setLowestCouponPrice(BigDecimal lowestCouponPrice) { this.lowestCouponPrice = lowestCouponPrice; }

    public BigDecimal getCommissionShare() { return commissionShare; }
    public void setCommissionShare(BigDecimal commissionShare) { this.commissionShare = commissionShare; }

    public BigDecimal getCouponCommission() { return couponCommission; }
    public void setCouponCommission(BigDecimal couponCommission) { this.couponCommission = couponCommission; }

    public String getCouponId() { return couponId; }
    public void setCouponId(String couponId) { this.couponId = couponId; }

    public BigDecimal getCouponDiscount() { return couponDiscount; }
    public void setCouponDiscount(BigDecimal couponDiscount) { this.couponDiscount = couponDiscount; }

    public String getCouponLink() { return couponLink; }
    public void setCouponLink(String couponLink) { this.couponLink = couponLink; }

    public String getCouponStartTime() { return couponStartTime; }
    public void setCouponStartTime(String couponStartTime) { this.couponStartTime = couponStartTime; }

    public String getCouponEndTime() { return couponEndTime; }
    public void setCouponEndTime(String couponEndTime) { this.couponEndTime = couponEndTime; }

    public Integer getCouponRemainNum() { return couponRemainNum; }
    public void setCouponRemainNum(Integer couponRemainNum) { this.couponRemainNum = couponRemainNum; }

    public Integer getInOrderCount30Days() { return inOrderCount30Days; }
    public void setInOrderCount30Days(Integer inOrderCount30Days) { this.inOrderCount30Days = inOrderCount30Days; }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getSpuid() { return spuid; }
    public void setSpuid(String spuid) { this.spuid = spuid; }

    /**
     * 获取券后价(元)
     */
    public BigDecimal getCouponPrice() {
        if (lowestCouponPrice != null) {
            return lowestCouponPrice;
        }
        BigDecimal p = price != null ? price : BigDecimal.ZERO;
        BigDecimal c = couponDiscount != null ? couponDiscount : BigDecimal.ZERO;
        return p.subtract(c).max(BigDecimal.ZERO);
    }

    /**
     * 获取原价(元)
     */
    public BigDecimal getOriginalPrice() {
        return price != null ? price : BigDecimal.ZERO;
    }

    /**
     * 获取佣金比例(%)
     */
    public double getCommissionRatePercent() {
        if (commissionShare == null) return 0;
        return commissionShare.doubleValue();
    }
}
