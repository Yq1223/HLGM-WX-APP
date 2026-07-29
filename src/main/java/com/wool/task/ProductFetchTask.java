package com.wool.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wool.config.JdConfig;
import com.wool.config.PddConfig;
import com.wool.entity.User;
import com.wool.entity.WoolInfo;
import com.wool.mapper.UserMapper;
import com.wool.mapper.WoolInfoMapper;
import com.wool.platform.ProductConverter;
import com.wool.platform.jd.JdApiClient;
import com.wool.platform.jd.JdGoods;
import com.wool.platform.pdd.PddApiClient;
import com.wool.platform.pdd.PddGoods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 定时拉取拼多多/京东商品任务
 */
@Component
public class ProductFetchTask {

    private static final Logger log = LoggerFactory.getLogger(ProductFetchTask.class);

    private final PddApiClient pddApiClient;
    private final JdApiClient jdApiClient;
    private final ProductConverter productConverter;
    private final WoolInfoMapper woolInfoMapper;
    private final UserMapper userMapper;
    private final PddConfig pddConfig;
    private final JdConfig jdConfig;

    /** 标记任务是否正在执行（防重入） */
    private volatile boolean pddRunning = false;
    private volatile boolean jdRunning = false;

    public ProductFetchTask(PddApiClient pddApiClient, JdApiClient jdApiClient,
                            ProductConverter productConverter, WoolInfoMapper woolInfoMapper,
                            UserMapper userMapper, PddConfig pddConfig, JdConfig jdConfig) {
        this.pddApiClient = pddApiClient;
        this.jdApiClient = jdApiClient;
        this.productConverter = productConverter;
        this.woolInfoMapper = woolInfoMapper;
        this.userMapper = userMapper;
        this.pddConfig = pddConfig;
        this.jdConfig = jdConfig;
    }

    /**
     * 定时拉取拼多多商品（每30分钟执行）
     */
    @Scheduled(cron = "${pdd.fetch-cron:0 0/30 * * * ?}")
    public void fetchPddProducts() {
        if (pddRunning) {
            log.warn("[PDD] 任务正在执行中，跳过本次");
            return;
        }
        if (pddConfig.getClientId() == null || pddConfig.getClientId().isEmpty()) {
            log.debug("[PDD] 未配置clientId，跳过拉取");
            return;
        }

        pddRunning = true;
        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;

        try {
            Long systemUserId = getSystemUserId();
            if (systemUserId == null) {
                log.error("[PDD] 未找到管理员账号，无法拉取商品");
                return;
            }

            // 获取已存在的平台商品ID（用于去重）
            Set<String> existingIds = getExistingPlatformIds("pdd");

            log.info("[PDD] 开始拉取商品, pageSize={}, maxPage={}", pddConfig.getPageSize(), pddConfig.getMaxPage());

            // 策略1: 获取推荐商品（无需PID，多种频道）
            // channel_type: 0-1.9包邮 1-今日爆款 2-品牌好货 5-实时热销 6-实时收益
            int[] channelTypes = {5, 6, 1, 0};
            for (int channelType : channelTypes) {
                try {
                    for (int offset = 0; offset < pddConfig.getMaxPage() * pddConfig.getPageSize(); offset += pddConfig.getPageSize()) {
                        List<PddGoods> goodsList = pddApiClient.getRecommendGoods(offset, pddConfig.getPageSize(), channelType);
                        if (goodsList.isEmpty()) break;

                        for (PddGoods goods : goodsList) {
                            if (goods.getCommissionRatePercent() < pddConfig.getMinCommissionRate()) {
                                skipCount++;
                                continue;
                            }
                            if (existingIds.contains(goods.getGoodsId())) {
                                skipCount++;
                                continue;
                            }
                            WoolInfo info = productConverter.convertPddGoods(goods, systemUserId);
                            if (info == null) {
                                failCount++;
                                continue;
                            }
                            if (goods.getCouponPrice().compareTo(BigDecimal.ZERO) <= 0) {
                                skipCount++;
                                continue;
                            }
                            woolInfoMapper.insert(info);
                            existingIds.add(goods.getGoodsId());
                            successCount++;
                        }
                        Thread.sleep(500);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("[PDD] 任务被中断");
                    return;
                } catch (Exception e) {
                    log.error("[PDD] 拉取推荐商品异常: channelType={}", channelType, e);
                    failCount++;
                }
            }

            // 策略2: 搜索热门关键词（需配置PID）
            if (pddConfig.getPid() != null && !pddConfig.getPid().isEmpty()) {
                String[] keywords = {"日用品", "零食", "数码配件", "美妆", "家居", "母婴", "食品", "服装"};
                for (String keyword : keywords) {
                    for (int page = 1; page <= pddConfig.getMaxPage(); page++) {
                        try {
                            List<PddGoods> goodsList = pddApiClient.searchGoods(keyword, page,
                                    pddConfig.getPageSize(), 6);
                            if (goodsList.isEmpty()) break;

                            for (PddGoods goods : goodsList) {
                                if (goods.getCommissionRatePercent() < pddConfig.getMinCommissionRate()) {
                                    skipCount++;
                                    continue;
                                }
                                if (existingIds.contains(goods.getGoodsId())) {
                                    skipCount++;
                                    continue;
                                }
                                WoolInfo info = productConverter.convertPddGoods(goods, systemUserId);
                                if (info == null) {
                                    failCount++;
                                    continue;
                                }
                                if (goods.getCouponPrice().compareTo(BigDecimal.ZERO) <= 0) {
                                    skipCount++;
                                    continue;
                                }
                                woolInfoMapper.insert(info);
                                existingIds.add(goods.getGoodsId());
                                successCount++;
                            }
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.warn("[PDD] 任务被中断");
                            return;
                        } catch (Exception e) {
                            log.error("[PDD] 搜索商品异常: keyword={}, page={}", keyword, page, e);
                            failCount++;
                        }
                    }
                }
            } else {
                log.info("[PDD] 未配置PID，跳过搜索接口。如需搜索功能，请配置pdd.pid");
            }

            log.info("[PDD] 拉取完成: 成功={}, 跳过={}, 失败={}", successCount, skipCount, failCount);
        } catch (Exception e) {
            log.error("[PDD] 定时任务异常", e);
        } finally {
            pddRunning = false;
        }
    }

    /**
     * 定时拉取京东商品（每30分钟执行，与PDD错峰）
     */
    @Scheduled(cron = "${jd.fetch-cron:0 15/30 * * * ?}")
    public void fetchJdProducts() {
        if (jdRunning) {
            log.warn("[JD] 任务正在执行中，跳过本次");
            return;
        }
        if (jdConfig.getAppKey() == null || jdConfig.getAppKey().isEmpty()) {
            log.debug("[JD] 未配置appKey，跳过拉取");
            return;
        }

        jdRunning = true;
        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;

        try {
            Long systemUserId = getSystemUserId();
            if (systemUserId == null) {
                log.error("[JD] 未找到管理员账号，无法拉取商品");
                return;
            }

            Set<String> existingIds = getExistingPlatformIds("jd");

            log.info("[JD] 开始拉取商品, pageSize={}, maxPage={}", jdConfig.getPageSize(), jdConfig.getMaxPage());

            // 策略1: 京粉精选商品（已验证可用，无需额外权限）
            // eliteId: 1-好券商品 2-精选卖场 10-9.9包邮 22-实时热销 25-实时收益
            int[] eliteIds = {1, 22, 2, 10};
            for (int eliteId : eliteIds) {
                try {
                    for (int page = 1; page <= jdConfig.getMaxPage(); page++) {
                        List<JdGoods> goodsList = jdApiClient.getEliteGoods(eliteId, page, jdConfig.getPageSize());
                        if (goodsList.isEmpty()) break;

                        for (JdGoods goods : goodsList) {
                            if (goods.getCommissionRatePercent() < jdConfig.getMinCommissionRate()) {
                                skipCount++;
                                continue;
                            }
                            if (existingIds.contains(goods.getSkuId())) {
                                skipCount++;
                                continue;
                            }
                            WoolInfo info = productConverter.convertJdGoods(goods, systemUserId);
                            if (info == null) {
                                failCount++;
                                continue;
                            }
                            if (goods.getCouponPrice().compareTo(BigDecimal.ZERO) <= 0) {
                                skipCount++;
                                continue;
                            }
                            woolInfoMapper.insert(info);
                            existingIds.add(goods.getSkuId());
                            successCount++;
                        }
                        Thread.sleep(500);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("[JD] 任务被中断");
                    return;
                } catch (Exception e) {
                    log.error("[JD] 拉取京粉精选异常: eliteId={}", eliteId, e);
                    failCount++;
                }
            }

            // 策略2: 搜索热门关键词（需申请商品查询权限）
            String[] keywords = {"日用品", "零食", "数码配件", "美妆", "家居", "母婴", "食品", "服装"};
            for (String keyword : keywords) {
                for (int page = 1; page <= jdConfig.getMaxPage(); page++) {
                    try {
                        List<JdGoods> goodsList = jdApiClient.searchGoods(keyword, page,
                                jdConfig.getPageSize(), "inOrderCount30Days", "desc");
                        if (goodsList.isEmpty()) break;

                        for (JdGoods goods : goodsList) {
                            if (goods.getCommissionRatePercent() < jdConfig.getMinCommissionRate()) {
                                skipCount++;
                                continue;
                            }
                            if (existingIds.contains(goods.getSkuId())) {
                                skipCount++;
                                continue;
                            }
                            WoolInfo info = productConverter.convertJdGoods(goods, systemUserId);
                            if (info == null) {
                                failCount++;
                                continue;
                            }
                            if (goods.getCouponPrice().compareTo(BigDecimal.ZERO) <= 0) {
                                skipCount++;
                                continue;
                            }
                            woolInfoMapper.insert(info);
                            existingIds.add(goods.getSkuId());
                            successCount++;
                        }
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("[JD] 任务被中断");
                        return;
                    } catch (Exception e) {
                        log.error("[JD] 搜索商品异常: keyword={}, page={}", keyword, page, e);
                        failCount++;
                    }
                }
            }

            log.info("[JD] 拉取完成: 成功={}, 跳过={}, 失败={}", successCount, skipCount, failCount);
        } catch (Exception e) {
            log.error("[JD] 定时任务异常", e);
        } finally {
            jdRunning = false;
        }
    }

    /**
     * 手动触发拼多多拉取
     */
    public ManualFetchResult manualFetchPdd() {
        fetchPddProducts();
        return new ManualFetchResult("pdd", pddRunning ? "正在执行" : "已完成");
    }

    /**
     * 手动触发京东拉取
     */
    public ManualFetchResult manualFetchJd() {
        fetchJdProducts();
        return new ManualFetchResult("jd", jdRunning ? "正在执行" : "已完成");
    }

    /**
     * 手动触发全部拉取
     */
    public ManualFetchResult manualFetchAll() {
        fetchPddProducts();
        fetchJdProducts();
        return new ManualFetchResult("all", "已完成");
    }

    /**
     * 获取系统管理员用户ID（role=1的第一个用户）
     */
    private Long getSystemUserId() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getRole, 1)
                .last("LIMIT 1");
        User admin = userMapper.selectOne(wrapper);
        return admin != null ? admin.getId() : null;
    }

    /**
     * 获取已存在的平台商品ID集合（用于去重）
     */
    private Set<String> getExistingPlatformIds(String platform) {
        Set<String> ids = new HashSet<>();
        try {
            LambdaQueryWrapper<WoolInfo> wrapper = new LambdaQueryWrapper<WoolInfo>()
                    .eq(WoolInfo::getCategory, platform)
                    .select(WoolInfo::getContent);
            List<WoolInfo> existingList = woolInfoMapper.selectList(wrapper);
            for (WoolInfo info : existingList) {
                String platformGoodsId = ProductConverter.extractPlatformGoodsId(info.getContent());
                if (platformGoodsId != null) {
                    ids.add(platformGoodsId);
                }
            }
            log.info("[{}] 已有{}条平台商品", platform, ids.size());
        } catch (Exception e) {
            log.error("[{}] 查询已有商品异常", platform, e);
        }
        return ids;
    }

    /**
     * 手动拉取结果
     */
    public static class ManualFetchResult {
        private final String platform;
        private final String status;
        private final long timestamp;

        public ManualFetchResult(String platform, String status) {
            this.platform = platform;
            this.status = status;
            this.timestamp = System.currentTimeMillis();
        }

        public String getPlatform() { return platform; }
        public String getStatus() { return status; }
        public long getTimestamp() { return timestamp; }
    }
}
