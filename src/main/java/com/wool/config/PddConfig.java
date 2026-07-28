package com.wool.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 拼多多（多多进宝）配置
 */
@Configuration
@ConfigurationProperties(prefix = "pdd")
public class PddConfig {

    /** 客户端ID */
    private String clientId;

    /** 客户端密钥 */
    private String clientSecret;

    /** 访问令牌（OAuth获取后填入，或手动配置） */
    private String accessToken;

    /** PID（推广位ID） */
    private String pid;

    /** 每次拉取数量 */
    private int pageSize = 50;

    /** 拉取页数 */
    private int maxPage = 5;

    /** 最低佣金比例(%)，低于此值过滤 */
    private double minCommissionRate = 1.0;

    /** 最低优惠券面额(元)，低于此值过滤 */
    private double minCouponAmount = 0.0;

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getPid() { return pid; }
    public void setPid(String pid) { this.pid = pid; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public int getMaxPage() { return maxPage; }
    public void setMaxPage(int maxPage) { this.maxPage = maxPage; }

    public double getMinCommissionRate() { return minCommissionRate; }
    public void setMinCommissionRate(double minCommissionRate) { this.minCommissionRate = minCommissionRate; }

    public double getMinCouponAmount() { return minCouponAmount; }
    public void setMinCouponAmount(double minCouponAmount) { this.minCouponAmount = minCouponAmount; }
}
