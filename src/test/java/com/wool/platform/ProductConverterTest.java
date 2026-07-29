package com.wool.platform;

import cn.hutool.json.JSONUtil;
import com.wool.entity.WoolInfo;
import com.wool.platform.jd.JdGoods;
import com.wool.platform.pdd.PddGoods;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProductConverter 单元测试
 */
class ProductConverterTest {

    private ProductConverter converter;

    @BeforeEach
    void setUp() {
        converter = new ProductConverter();
    }

    // ==================== PDD 转换测试 ====================

    @Test
    void convertPddGoods_normalInput_shouldConvert() {
        PddGoods goods = createSamplePddGoods();

        WoolInfo result = converter.convertPddGoods(goods, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals("pdd", result.getCategory());
        assertEquals(1, result.getStatus());
        assertEquals(0, result.getViewCount());

        // 验证title截断
        assertTrue(result.getTitle().length() <= 128);

        // 验证content是合法JSON
        assertNotNull(result.getContent());
        Map<String, Object> contentMap = JSONUtil.parseObj(result.getContent());
        assertEquals("pdd", contentMap.get("platform"));
        assertEquals("123456", contentMap.get("platformGoodsId"));
        assertNotNull(contentMap.get("imageUrl"));
        assertNotNull(contentMap.get("originalPrice"));
        assertNotNull(contentMap.get("couponPrice"));
        assertNotNull(contentMap.get("commissionRate"));

        // 验证source_url
        assertEquals("https://example.com/goods/123456", result.getSourceUrl());

        // 验证claim_steps包含佣金信息
        assertNotNull(result.getClaimSteps());
        assertTrue(result.getClaimSteps().contains("佣金比例"));
    }

    @Test
    void convertPddGoods_nullGoods_shouldReturnNull() {
        assertNull(converter.convertPddGoods(null, 1L));
    }

    @Test
    void convertPddGoods_nullGoodsId_shouldReturnNull() {
        PddGoods goods = new PddGoods();
        goods.setGoodsId(null);
        assertNull(converter.convertPddGoods(goods, 1L));
    }

    @Test
    void convertPddGoods_longTitle_shouldTruncate() {
        PddGoods goods = createSamplePddGoods();
        StringBuilder longTitle = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longTitle.append("测试");
        }
        goods.setGoodsName(longTitle.toString());

        WoolInfo result = converter.convertPddGoods(goods, 1L);

        assertNotNull(result);
        assertTrue(result.getTitle().length() <= 128);
    }

    @Test
    void convertPddGoods_withCoupon_shouldHaveCouponInfo() {
        PddGoods goods = createSamplePddGoods();
        goods.setCouponDiscount(BigDecimal.valueOf(500)); // 5元券
        goods.setCouponMinOrderAmount(BigDecimal.valueOf(2000)); // 满20
        goods.setCouponRemainQuantity(100);

        WoolInfo result = converter.convertPddGoods(goods, 1L);

        assertNotNull(result);
        assertTrue(result.getClaimSteps().contains("优惠券面额"));
        assertTrue(result.getClaimSteps().contains("剩余券数"));

        Map<String, Object> contentMap = JSONUtil.parseObj(result.getContent());
        assertNotNull(contentMap.get("couponAmount"));
        assertEquals(100, contentMap.get("remainQuantity"));
    }

    @Test
    void convertPddGoods_batchConvert_shouldConvertAll() {
        List<PddGoods> goodsList = Arrays.asList(
                createSamplePddGoods(),
                createSamplePddGoods(),
                createSamplePddGoods()
        );
        goodsList.get(1).setGoodsId("789");
        goodsList.get(2).setGoodsId("101112");

        List<WoolInfo> result = converter.convertPddGoodsList(goodsList, 1L);

        assertEquals(3, result.size());
        for (WoolInfo info : result) {
            assertEquals("pdd", info.getCategory());
            assertNotNull(info.getContent());
        }
    }

    // ==================== JD 转换测试 ====================

    @Test
    void convertJdGoods_normalInput_shouldConvert() {
        JdGoods goods = createSampleJdGoods();

        WoolInfo result = converter.convertJdGoods(goods, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals("jd", result.getCategory());
        assertEquals(1, result.getStatus());

        // 验证content
        Map<String, Object> contentMap = JSONUtil.parseObj(result.getContent());
        assertEquals("jd", contentMap.get("platform"));
        assertEquals("987654", contentMap.get("platformGoodsId"));
        assertNotNull(contentMap.get("originalPrice"));
        assertNotNull(contentMap.get("couponPrice"));
        assertNotNull(contentMap.get("commissionRate"));
        assertEquals("京东自营旗舰店", contentMap.get("shopName"));

        // 验证claim_steps
        assertNotNull(result.getClaimSteps());
        assertTrue(result.getClaimSteps().contains("佣金比例"));
        assertTrue(result.getClaimSteps().contains("店铺"));
    }

    @Test
    void convertJdGoods_nullGoods_shouldReturnNull() {
        assertNull(converter.convertJdGoods(null, 1L));
    }

    @Test
    void convertJdGoods_nullSkuId_shouldReturnNull() {
        JdGoods goods = new JdGoods();
        goods.setSkuId(null);
        assertNull(converter.convertJdGoods(goods, 1L));
    }

    @Test
    void convertJdGoods_withCoupon_shouldHaveCouponInfo() {
        JdGoods goods = createSampleJdGoods();
        goods.setCouponDiscount(BigDecimal.valueOf(10));
        goods.setCouponRemainNum(50);

        WoolInfo result = converter.convertJdGoods(goods, 1L);

        assertNotNull(result);
        assertTrue(result.getClaimSteps().contains("优惠券面额"));
        assertTrue(result.getClaimSteps().contains("剩余券数"));

        Map<String, Object> contentMap = JSONUtil.parseObj(result.getContent());
        assertEquals(BigDecimal.valueOf(10), contentMap.get("couponAmount"));
        assertEquals(50, contentMap.get("remainQuantity"));
    }

    @Test
    void convertJdGoods_batchConvert_shouldConvertAll() {
        List<JdGoods> goodsList = Arrays.asList(
                createSampleJdGoods(),
                createSampleJdGoods()
        );
        goodsList.get(1).setSkuId("111222");

        List<WoolInfo> result = converter.convertJdGoodsList(goodsList, 1L);

        assertEquals(2, result.size());
        for (WoolInfo info : result) {
            assertEquals("jd", info.getCategory());
        }
    }

    // ==================== 静态工具方法测试 ====================

    @Test
    void extractPlatformGoodsId_validContent_shouldExtract() {
        String content = "{\"platform\":\"pdd\",\"platformGoodsId\":\"123456\"}";
        assertEquals("123456", ProductConverter.extractPlatformGoodsId(content));
    }

    @Test
    void extractPlatformGoodsId_invalidContent_shouldReturnNull() {
        assertNull(ProductConverter.extractPlatformGoodsId(null));
        assertNull(ProductConverter.extractPlatformGoodsId(""));
        assertNull(ProductConverter.extractPlatformGoodsId("not json"));
    }

    @Test
    void extractPlatform_validContent_shouldExtract() {
        String content = "{\"platform\":\"jd\",\"platformGoodsId\":\"987654\"}";
        assertEquals("jd", ProductConverter.extractPlatform(content));
    }

    // ==================== 辅助方法 ====================

    private PddGoods createSamplePddGoods() {
        PddGoods goods = new PddGoods();
        goods.setGoodsId("123456");
        goods.setGoodsName("测试商品-超长商品名称测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试");
        goods.setGoodsDesc("测试描述");
        goods.setGoodsImageUrl("https://img.pddpic.com/test.jpg");
        goods.setMinGroupPrice(BigDecimal.valueOf(1990)); // 19.90元
        goods.setMinNormalPrice(BigDecimal.valueOf(2990)); // 29.90元
        goods.setCouponDiscount(BigDecimal.valueOf(500)); // 5元券
        goods.setCouponMinOrderAmount(BigDecimal.valueOf(1500));
        goods.setPromotionRate(250); // 25%
        goods.setSalesTip("已拼10万件");
        goods.setGoodsDetailUrl("https://example.com/goods/123456");
        goods.setCouponId("coupon_001");
        goods.setCouponRemainQuantity(500);
        goods.setServiceTags(Arrays.asList("包邮", "退货包运费"));
        return goods;
    }

    private JdGoods createSampleJdGoods() {
        JdGoods goods = new JdGoods();
        goods.setSkuId("987654");
        goods.setSkuName("京东测试商品-高品质生活用品");
        goods.setSkuDesc("测试描述");
        goods.setImageUrl("[{\"url\":\"https://img14.360buyimg.com/test.jpg\"}]");
        goods.setMaterialUrl("https://u.jd.com/test");
        goods.setPrice(BigDecimal.valueOf(39.9));
        goods.setLowestPrice(BigDecimal.valueOf(35.9));
        goods.setLowestCouponPrice(BigDecimal.valueOf(29.9));
        goods.setCommissionShare(BigDecimal.valueOf(15.5));
        goods.setCouponDiscount(BigDecimal.valueOf(10));
        goods.setCouponRemainNum(200);
        goods.setInOrderCount30Days(5000);
        goods.setShopName("京东自营旗舰店");
        goods.setBrandName("测试品牌");
        goods.setTags(Arrays.asList("京东物流", "自营"));
        goods.setOwner("g");
        return goods;
    }
}
