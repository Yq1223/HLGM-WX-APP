package com.wool.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 京东联盟配置
 */
@Configuration
@ConfigurationProperties(prefix = "jd")
public class JdConfig {

    /** 应用Key */
    private String appKey;

    /** 应用密钥 */
    private String secretKey;

    /** 联盟ID */
    private String unionId;

    /** 推广位ID */
    private String siteId;

    /** 每次拉取数量 */
    private int pageSize = 50;

    /** 拉取页数 */
    private int maxPage = 5;

    /** 最低佣金比例(%)，低于此值过滤 */
    private double minCommissionRate = 1.0;

    /** 最低优惠券面额(元)，低于此值过滤 */
    private double minCouponAmount = 0.0;

    public String getAppKey() { return appKey; }
    public void setAppKey(String appKey) { this.appKey = appKey; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getUnionId() { return unionId; }
    public void setUnionId(String unionId) { this.unionId = unionId; }

    public String getSiteId() { return siteId; }
    public void setSiteId(String siteId) { this.siteId = siteId; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public int getMaxPage() { return maxPage; }
    public void setMaxPage(int maxPage) { this.maxPage = maxPage; }

    public double getMinCommissionRate() { return minCommissionRate; }
    public void setMinCommissionRate(double minCommissionRate) { this.minCommissionRate = minCommissionRate; }

    public double getMinCouponAmount() { return minCouponAmount; }
    public void setMinCouponAmount(double minCouponAmount) { this.minCouponAmount = minCouponAmount; }
}
