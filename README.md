# 🐑 薅个毛吧 - 微信小程序前端

> **本来不想薅的，但它实在太便宜了。**

薅羊毛信息社区的微信小程序前端，配套 Java 后台 API 使用。

---

## 快速开始

### 前提条件

- 后台服务已启动（见主项目 README）
- 已安装 [微信开发者工具](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html)

### 第一步：修改配置

**1. 修改后台地址** — 打开 `app.js`：

```javascript
globalData: {
  baseUrl: 'http://localhost:8080'   ← 改成你的后台服务地址
}
```

**2. 修改 AppID** — 打开 `project.config.json`：

```json
"appid": "your_appid"    ← 改成你自己的小程序 AppID
```

### 第二步：导入项目

1. 打开微信开发者工具
2. 点击 **「+」号 → 导入项目**
3. 目录选择本文件夹（`hlgm-miniprogram`）
4. 点击 **确定**

### 第三步：预览

- 模拟器中直接预览
- 点击 **「预览」** 扫码在真机上体验

---

## 项目结构

```
hlgm-miniprogram/
├── app.js                         # 全局逻辑（登录、状态管理）
├── app.json                       # 全局配置（页面路由、TabBar）
├── app.wxss                       # 全局样式（主题色 #FF6B35）
├── project.config.json            # 开发者工具配置
├── images/                        # Logo 和 TabBar 图标
│   ├── logo.png                   # 原始 Logo（1920x1920）
│   ├── logo-120.png / 200 / 400   # 不同尺寸
│   ├── splash-logo.png            # 开屏页 Logo
│   └── tab-*.png                  # TabBar 图标
├── utils/
│   ├── request.js                 # HTTP 请求封装
│   ├── auth.js                    # 登录鉴权
│   └── util.js                    # 工具函数
├── components/
│   ├── empty/                     # 空状态组件
│   └── ad-banner/                 # 广告位占位组件
└── pages/
    ├── welcome/       🎬 开屏页       Logo + Slogan + 进入按钮
    ├── index/         🏠 首页          卡片列表 + 搜索 + 广告位
    ├── detail/        📄 详情页        富文本 + 权限按钮 + 广告位
    ├── publish/       ✏️ 发布页        表单 + Excel批量导入
    ├── mine/          👤 个人中心      积分 + 我的发布 + 广告位
    ├── admin/         ⚙️ 审核管理      管理员专用
    ├── exchange/      🎁 积分兑换      建设中占位
    ├── points/        📊 积分明细      积分变动记录
    └── exchange-record/ 📋 兑换记录    占位页
```

---

## 功能说明

| 页面 | 功能 | 对应 API |
|------|------|----------|
| 首页 | 分页列表、搜索、下拉刷新、上拉加载 | `GET /api/wool/list` |
| 详情 | 查看内容、编辑/删除（本人）、审核/上下线（管理员） | `GET /api/wool/detail/{id}` |
| 发布 | 填写表单提交、Excel 批量导入 | `POST /api/wool/publish` / `POST /api/wool/import` |
| 个人中心 | 积分展示、我的发布（按状态筛选） | `GET /api/wool/mine` |
| 审核管理 | 审核通过/驳回、上下线、删除 | `GET/POST/PUT/DELETE /api/admin/wool/*` |
| 积分明细 | 积分变动记录 | `GET /api/points/log` |

---

## 核心机制

### 登录流程

```
wx.login() 获取 code
    ↓
POST /api/auth/login { code }
    ↓
返回 token + 用户信息
    ↓
存入 globalData + localStorage
    ↓
后续请求自动带 Authorization: Bearer <token>
```

### 请求封装（utils/request.js）

- 自动在 Header 中携带 `Authorization: Bearer <token>`
- `code !== 0` 时自动 toast 错误信息
- 401 状态自动清除登录状态并提示重新登录
- 支持 `showLoading` 参数显示加载提示

### 广告位预留

项目中预留了 5 个广告位，当前全部隐藏（`showAd = false`）。

接入广告时只需修改 `components/ad-banner/ad-banner.js` 中的 `showAd` 为 `true`，并替换内部实现为真实广告组件。

---

## 注意事项

- `images/` 中的 TabBar 图标为占位图，建议替换为正式设计图
- 开发阶段需在开发者工具中勾选「不校验合法域名」
- 正式上线需在微信公众平台配置合法域名
- Slogan：*"本来不想薅的，但它实在太便宜了"*
