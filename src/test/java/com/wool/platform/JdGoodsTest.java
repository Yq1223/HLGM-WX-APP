package com.wool.platform;

import com.wool.platform.jd.JdGoods;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JdGoods 数据模型测试
 */
class JdGoodsTest {

    @Test
    void getCouponPrice_withCouponPrice_shouldReturnIt() {
        JdGoods goods = new JdGoods();
        goods.setLowestCouponPrice(BigDecimal.valueOf(29.9));
        goods.setPrice(BigDecimal.valueOf(39.9));
        goods.setCouponDiscount(BigDecimal.valueOf(10));

        BigDecimal price = goods.getCouponPrice();

        // lowestCouponPrice优先
        assertEquals(0, BigDecimal.valueOf(29.9).compareTo(price));
    }

    @Test
    void getCouponPrice_noCouponPrice_shouldCalculate() {
        JdGoods goods = new JdGoods();
        goods.setLowestCouponPrice(null);
        goods.setPrice(BigDecimal.valueOf(39.9));
        goods.setCouponDiscount(BigDecimal.valueOf(10));

        BigDecimal price = goods.getCouponPrice();

        assertEquals(0, BigDecimal.valueOf(29.9).compareTo(price));
    }

    @Test
    void getCouponPrice_couponExceedsPrice_shouldReturnZero() {
        JdGoods goods = new JdGoods();
        goods.setLowestCouponPrice(null);
        goods.setPrice(BigDecimal.valueOf(5));
        goods.setCouponDiscount(BigDecimal.valueOf(10));

        BigDecimal price = goods.getCouponPrice();

        assertEquals(0, BigDecimal.ZERO.compareTo(price));
    }

    @Test
    void getOriginalPrice_shouldReturnPrice() {
        JdGoods goods = new JdGoods();
        goods.setPrice(BigDecimal.valueOf(99.9));

        assertEquals(0, BigDecimal.valueOf(99.9).compareTo(goods.getOriginalPrice()));
    }

    @Test
    void getOriginalPrice_null_shouldReturnZero() {
        JdGoods goods = new JdGoods();
        goods.setPrice(null);

        assertEquals(0, BigDecimal.ZERO.compareTo(goods.getOriginalPrice()));
    }

    @Test
    void getCommissionRatePercent_shouldReturnValue() {
        JdGoods goods = new JdGoods();
        goods.setCommissionShare(BigDecimal.valueOf(15.5));

        assertEquals(15.5, goods.getCommissionRatePercent(), 0.01);
    }

    @Test
    void getCommissionRatePercent_null_shouldReturnZero() {
        JdGoods goods = new JdGoods();
        goods.setCommissionShare(null);

        assertEquals(0.0, goods.getCommissionRatePercent(), 0.01);
    }
}
