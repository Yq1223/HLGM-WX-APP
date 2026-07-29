package com.wool.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wool.entity.WoolInfo;
import com.wool.mapper.WoolInfoMapper;
import com.wool.platform.ProductConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 启动时自动迁移旧分类数据（pdd/jd -> 智能分类）
 * 只在存在旧分类数据时执行，执行一次后不会再触发
 */
@Component
@Order(10) // 在其他初始化之后执行
public class CategoryMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CategoryMigrationRunner.class);

    private final WoolInfoMapper woolInfoMapper;

    public CategoryMigrationRunner(WoolInfoMapper woolInfoMapper) {
        this.woolInfoMapper = woolInfoMapper;
    }

    @Override
    @Transactional
    public void run(String... args) {
        LambdaQueryWrapper<WoolInfo> wrapper = new LambdaQueryWrapper<WoolInfo>()
                .in(WoolInfo::getCategory, "pdd", "jd");
        List<WoolInfo> records = woolInfoMapper.selectList(wrapper);

        if (records.isEmpty()) {
            return; // 无需迁移，静默跳过
        }

        log.info("[自动迁移] 发现 {} 条旧分类数据(pdd/jd)，开始迁移...", records.size());

        int migrated = 0;
        for (WoolInfo info : records) {
            try {
                String goodsName = info.getTitle();
                List<String> tags = new ArrayList<>();
                try {
                    cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(info.getContent());
                    if (json.containsKey("tags")) {
                        tags = json.getBeanList("tags", String.class);
                    }
                } catch (Exception ignored) {
                }

                String newCategory = ProductConverter.guessCategory(goodsName, tags);
                info.setCategory(newCategory);
                woolInfoMapper.updateById(info);
                migrated++;
            } catch (Exception e) {
                log.error("[自动迁移] id={} 迁移失败: {}", info.getId(), e.getMessage());
            }
        }

        log.info("[自动迁移] 完成，共迁移 {} 条记录", migrated);
    }
}
