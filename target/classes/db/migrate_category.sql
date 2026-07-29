-- ============================================================
-- 旧分类数据迁移: pdd/jd -> 智能分类
-- 执行一次即可，不会影响已有正确分类的数据
-- ============================================================

UPDATE t_wool_info
SET category = CASE
    -- 会员
    WHEN title LIKE '%会员%' OR title LIKE '%vip%' OR title LIKE '%VIP%'
      OR title LIKE '%视频会员%' OR title LIKE '%音乐会员%'
      OR title LIKE '%网盘%' OR title LIKE '%WPS%' OR title LIKE '%wps%'
      THEN '会员'
    -- 话费
    WHEN title LIKE '%话费%' OR title LIKE '%充值%' OR title LIKE '%流量%'
      OR title LIKE '%移动%' OR title LIKE '%联通%' OR title LIKE '%电信%'
      THEN '话费'
    -- 外卖
    WHEN title LIKE '%外卖%' OR title LIKE '%美团%' OR title LIKE '%饿了么%'
      OR title LIKE '%瑞幸%' OR title LIKE '%星巴克%' OR title LIKE '%奶茶%'
      THEN '外卖'
    -- 美食
    WHEN title LIKE '%零食%' OR title LIKE '%食品%' OR title LIKE '%坚果%'
      OR title LIKE '%饼干%' OR title LIKE '%巧克力%' OR title LIKE '%糖果%'
      OR title LIKE '%饮料%' OR title LIKE '%牛奶%' OR title LIKE '%面包%'
      OR title LIKE '%蛋糕%'
      THEN '美食'
    -- 出行
    WHEN title LIKE '%打车%' OR title LIKE '%滴滴%' OR title LIKE '%高德%'
      OR title LIKE '%机票%' OR title LIKE '%火车票%' OR title LIKE '%酒店%'
      OR title LIKE '%民宿%' OR title LIKE '%加油%'
      THEN '出行'
    -- 生活
    WHEN title LIKE '%纸巾%' OR title LIKE '%洗衣%' OR title LIKE '%牙膏%'
      OR title LIKE '%洗发%' OR title LIKE '%垃圾袋%' OR title LIKE '%保鲜膜%'
      OR title LIKE '%收纳%' OR title LIKE '%清洁%' OR title LIKE '%家政%'
      THEN '生活'
    -- 电商（兜底）
    ELSE '电商'
END
WHERE category IN ('pdd', 'jd');

-- 查看迁移结果
SELECT category, COUNT(*) AS cnt FROM t_wool_info GROUP BY category ORDER BY cnt DESC;
