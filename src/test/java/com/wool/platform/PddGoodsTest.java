package com.wool.platform;

import com.wool.platform.pdd.PddGoods;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PddGoods 数据模型测试
 */
class PddGoodsTest {

    @Test
    void getCouponPrice_withCoupon_shouldCalculateCorrectly() {
        PddGoods goods = new PddGoods();
        goods.setMinGroupPrice(BigDecimal.valueOf(1990)); // 19.90元(分)
        goods.setCouponDiscount(BigDecimal.valueOf(500)); // 5元券(分)

        BigDecimal couponPrice = goods.getCouponPrice();

        assertEquals(0, BigDecimal.valueOf(14.90).compareTo(couponPrice));
    }

    @Test
    void getCouponPrice_noCoupon_shouldReturnGroupPrice() {
        PddGoods goods = new PddGoods();
        goods.setMinGroupPrice(BigDecimal.valueOf(1990));
        goods.setCouponDiscount(null);

        BigDecimal couponPrice = goods.getCouponPrice();

        assertEquals(0, BigDecimal.valueOf(19.90).compareTo(couponPrice));
    }

    @Test
    void getCouponPrice_couponExceedsPrice_shouldReturnZero() {
        PddGoods goods = new PddGoods();
        goods.setMinGroupPrice(BigDecimal.valueOf(500)); // 5元
        goods.setCouponDiscount(BigDecimal.valueOf(1000)); // 10元券

        BigDecimal couponPrice = goods.getCouponPrice();

        assertEquals(0, BigDecimal.ZERO.compareTo(couponPrice));
    }

    @Test
    void getOriginalPriceYuan_shouldConvertFromFen() {
        PddGoods goods = new PddGoods();
        goods.setMinNormalPrice(BigDecimal.valueOf(2990)); // 29.90元

        BigDecimal price = goods.getOriginalPriceYuan();

        assertEquals(0, BigDecimal.valueOf(29.90).compareTo(price));
    }

    @Test
    void getOriginalPriceYuan_nullPrice_shouldReturnZero() {
        PddGoods goods = new PddGoods();
        goods.setMinNormalPrice(null);
        goods.setMinGroupPrice(null);

        BigDecimal price = goods.getOriginalPriceYuan();

        assertEquals(0, BigDecimal.ZERO.compareTo(price));
    }

    @Test
    void getCommissionRatePercent_shouldConvertFromPermille() {
        PddGoods goods = new PddGoods();
        goods.setPromotionRate(250); // 250‰ = 25%

        double rate = goods.getCommissionRatePercent();

        assertEquals(25.0, rate, 0.01);
    }

    @Test
    void getCommissionRatePercent_null_shouldReturnZero() {
        PddGoods goods = new PddGoods();
        goods.setPromotionRate(null);

        assertEquals(0.0, goods.getCommissionRatePercent(), 0.01);
    }
}
