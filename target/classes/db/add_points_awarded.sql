-- ============================================================
-- 新增字段: points_awarded (积分是否已发放)
-- 执行一次即可
-- ============================================================

ALTER TABLE t_wool_info ADD COLUMN points_awarded TINYINT(1) DEFAULT 0 COMMENT '积分是否已发放: 0-未发放 1-已发放';

-- 旧数据默认标记为未发放
UPDATE t_wool_info SET points_awarded = 0 WHERE points_awarded IS NULL;
