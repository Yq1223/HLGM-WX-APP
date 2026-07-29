# 拼多多 & 京东商品拉取配置说明

## 一、功能概述

定时从拼多多（多多进宝）和京东联盟拉取高佣商品，写入现有 `t_wool_info` 表，供前端展示。

## 二、字段映射规则

**不修改数据库表结构**，使用现有字段存储：

| 数据库字段 | 存储内容 | 示例 |
|---|---|---|
| `title` | 商品标题（截断至128字） | `【爆款】某品牌纸巾...` |
| `content` | JSON格式结构化数据 | 见下方示例 |
| `category` | 平台标识 | `pdd` / `jd` |
| `source_url` | 商品推广链接 | `https://u.jd.com/xxx` |
| `claim_steps` | 佣金/券信息文本 | `佣金比例: 25.0%\n优惠券面额: ¥5.00` |
| `status` | 默认 `1`（已上线） | `1` |
| `user_id` | 系统管理员ID（自动取role=1的首个用户） | `1` |
| `view_count` | 默认 `0` | `0` |

### content 字段 JSON 结构（拼多多）

```json
{
  "platform": "pdd",
  "platformGoodsId": "123456",
  "imageUrl": "https://img.pddpic.com/xxx.jpg",
  "originalPrice": 29.90,
  "couponPrice": 24.90,
  "commissionRate": 25.0,
  "couponAmount": 5.00,
  "remainQuantity": 500,
  "salesTip": "已拼10万件",
  "tags": ["包邮", "退货包运费"]
}
```

### content 字段 JSON 结构（京东）

```json
{
  "platform": "jd",
  "platformGoodsId": "987654",
  "imageUrl": "https://img14.360buyimg.com/xxx.jpg",
  "originalPrice": 39.90,
  "couponPrice": 29.90,
  "commissionRate": 15.5,
  "couponAmount": 10.00,
  "remainQuantity": 200,
  "shopName": "京东自营旗舰店",
  "brandName": "某品牌",
  "inOrderCount30Days": 5000,
  "owner": "g",
  "tags": ["京东物流", "自营"]
}
```

## 三、配置项

### 3.1 application.yml 新增配置

```yaml
# ===== 拼多多（多多进宝）配置 =====
pdd:
  client-id: ${PDD_CLIENT_ID:}          # 客户端ID（必填）
  client-secret: ${PDD_CLIENT_SECRET:}   # 客户端密钥（必填，用于签名）
  access-token: ${PDD_ACCESS_TOKEN:}     # OAuth访问令牌（如已获取）
  pid: ${PDD_PID:}                       # 推广位ID（可选）
  page-size: 50                          # 每页拉取数量
  max-page: 5                            # 每个关键词最多拉取页数
  min-commission-rate: 1.0               # 最低佣金比例(%)
  min-coupon-amount: 0.0                 # 最低优惠券面额(元)
  fetch-cron: "0 0/30 * * * ?"          # 定时拉取cron表达式

# ===== 京东联盟配置 =====
jd:
  app-key: ${JD_APP_KEY:}               # 应用Key（必填）
  secret-key: ${JD_SECRET_KEY:}          # 应用密钥（必填，用于签名）
  union-id: ${JD_UNION_ID:}             # 联盟ID（可选）
  site-id: ${JD_SITE_ID:}              # 推广位ID（可选）
  page-size: 50                          # 每页拉取数量
  max-page: 5                            # 每个关键词最多拉取页数
  min-commission-rate: 1.0               # 最低佣金比例(%)
  min-coupon-amount: 0.0                 # 最低优惠券面额(元)
  fetch-cron: "0 15/30 * * * ?"         # 定时拉取cron表达式（与PDD错峰）
```

### 3.2 环境变量

| 环境变量 | 说明 | 必填 |
|---|---|---|
| `PDD_CLIENT_ID` | 拼多多clientId | ✅ |
| `PDD_CLIENT_SECRET` | 拼多多clientSecret（签名用） | ✅ |
| `PDD_ACCESS_TOKEN` | 拼多多OAuth访问令牌 | 按需 |
| `PDD_PID` | 拼多多推广位ID | 可选 |
| `JD_APP_KEY` | 京东appKey | ✅ |
| `JD_SECRET_KEY` | 京东secretKey（签名用） | ✅ |
| `JD_UNION_ID` | 京东联盟ID | 可选 |
| `JD_SITE_ID` | 京东推广位ID | 可选 |

## 四、API 接口

### 4.1 手动触发商品拉取（需管理员权限）

```bash
# 拉取拼多多商品
curl -X POST http://localhost:8080/api/admin/product-fetch/pdd \
  -H "Authorization: Bearer <admin_token>"

# 拉取京东商品
curl -X POST http://localhost:8080/api/admin/product-fetch/jd \
  -H "Authorization: Bearer <admin_token>"

# 拉取全部
curl -X POST http://localhost:8080/api/admin/product-fetch/all \
  -H "Authorization: Bearer <admin_token>"
```

响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "platform": "all",
    "status": "已完成",
    "timestamp": 1722134400000
  }
}
```

## 五、已验证可用的接口

### 拼多多
| 接口 | 需要PID | 需要access_token | 状态 |
|---|---|---|---|
| `pdd.ddk.goods.recommend.get` | ❌ | ❌ | ✅ 已验证 |
| `pdd.ddk.goods.search` | ✅ | ❌ | 需配置PID |
| `pdd.ddk.top.goods.list.query` | - | - | ❌ 已下线 |

推荐商品频道 (channel_type): 0-1.9包邮 1-今日爆款 2-品牌好货 5-实时热销 6-实时收益

### 京东
| 接口 | API地址 | 权限 | 状态 |
|---|---|---|---|
| `jd.union.open.goods.jingfen.query` | `router.jd.com/api` | 基础 | ✅ 已验证 |
| `jd.union.open.goods.query` | `router.jd.com/api` | 需申请 | 403 |

京粉精选频道 (eliteId): 1-好券商品 2-精选卖场 10-9.9包邮 22-实时热销 25-实时收益

**注意**: 京粉接口不返回 `skuId`，使用 `itemId`（京粉ID）和 `spuid`（SPU ID）作为唯一标识。

## 六、定时任务策略

- **拼多多**: 每30分钟执行（`0 0/30 * * * ?`），拉取推荐商品（实时热销/收益/爆款/1.9包邮 4个频道）
- **京东**: 每30分钟执行，与PDD错峰15分钟（`0 15/30 * * * ?`），拉取京粉精选（好券/热销/精选/9.9包邮 4个频道）
- **去重**: 按 `平台 + 平台商品ID` 去重，已存在的跳过
- **过滤**: 佣金比例低于阈值、券后价≤0的商品自动过滤
- **防重入**: 内存锁防止多实例/多次触发重叠执行

## 七、数据拉取流程

```
定时触发 / 手动触发
        ↓
  调用平台API获取商品列表
        ↓
  过滤: 佣金比例 < 阈值 → 跳过
  过滤: 券后价 ≤ 0 → 跳过
        ↓
  查询已有商品ID（去重）
        ↓
  转换为 WoolInfo 实体
  (title → 商品标题, content → JSON, category → pdd/jd, ...)
        ↓
  INSERT INTO t_wool_info
        ↓
  记录日志: 成功N条, 跳过N条, 失败N条
```

## 八、拉取关键词

默认搜索关键词（可在代码中修改）：
- 日用品、零食、数码配件、美妆、家居、母婴、食品、服装

## 九、注意事项

1. **首次使用前**，需确保 `t_user` 表中存在至少一个 `role=1` 的管理员账号，平台拉取的商品 `user_id` 将关联到该管理员。
2. 拼多多 `pdd.ddk.goods.recommend.get` 接口无需 access_token 和 PID，可直接调用。如需搜索功能，需在多多进宝平台申请PID。
3. 京东联盟API地址为 `https://router.jd.com/api`（非 `api.jd.com/routerjson`），时间戳格式为 `yyyy-MM-dd HH:mm:ss`。
4. 京粉精选接口无需额外权限，商品搜索接口需在京东联盟后台申请。
5. 所有敏感信息通过环境变量注入，**不要硬编码**。
6. 定时任务频率可在 `application.yml` 中通过 `pdd.fetch-cron` 和 `jd.fetch-cron` 动态调整。
7. 日志级别设为 `debug` 可查看完整的API请求/响应。

## 十、新增文件清单

| 文件 | 说明 |
|---|---|
| `config/PddConfig.java` | 拼多多配置类 |
| `config/JdConfig.java` | 京东联盟配置类 |
| `config/SchedulingConfig.java` | 启用Spring定时任务 |
| `platform/pdd/PddApiClient.java` | 拼多多API客户端 |
| `platform/pdd/PddGoods.java` | 拼多多商品模型 |
| `platform/jd/JdApiClient.java` | 京东API客户端 |
| `platform/jd/JdGoods.java` | 京东商品模型 |
| `platform/ProductConverter.java` | 商品数据转换器 |
| `task/ProductFetchTask.java` | 定时拉取任务 |
| `controller/ProductFetchController.java` | 手动触发管理接口 |
| `test/.../ProductConverterTest.java` | 转换器单元测试 |
| `test/.../PddGoodsTest.java` | PDD商品模型测试 |
| `test/.../JdGoodsTest.java` | JD商品模型测试 |
