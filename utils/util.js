/**
 * 工具函数
 */

/**
 * 格式化时间
 */
function formatTime(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  const now = new Date();
  const diff = now - date;

  // 1分钟内
  if (diff < 60000) return '刚刚';
  // 1小时内
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前';
  // 24小时内
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前';
  // 7天内
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前';

  const y = date.getFullYear();
  const m = (date.getMonth() + 1).toString().padStart(2, '0');
  const d = date.getDate().toString().padStart(2, '0');
  return y + '-' + m + '-' + d;
}

/**
 * 完整日期时间
 */
function formatDateTime(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  const y = date.getFullYear();
  const m = (date.getMonth() + 1).toString().padStart(2, '0');
  const d = date.getDate().toString().padStart(2, '0');
  const h = date.getHours().toString().padStart(2, '0');
  const min = date.getMinutes().toString().padStart(2, '0');
  return y + '-' + m + '-' + d + ' ' + h + ':' + min;
}

/**
 * 状态码转文字
 */
function statusText(status) {
  const map = { 0: '待审核', 1: '已上线', 2: '已驳回', 3: '已下线' };
  return map[status] || '未知';
}

/**
 * 状态码转样式类名
 */
function statusClass(status) {
  const map = { 0: 'pending', 1: 'online', 2: 'rejected', 3: 'offline' };
  return map[status] || '';
}

/**
 * 生成缩略图 URL
 * 拼多多图片支持 @{w}w_{h}h 后缀，京东图片支持 _{w}x{h}.jpg 后缀
 */
function getThumbUrl(imageUrl, platform) {
  if (!imageUrl) return '';
  // 拼多多
  if (platform === 'pdd' && imageUrl.indexOf('?') === -1) {
    return imageUrl + '@240w_240h_1e_1c';
  }
  // 京东
  if (platform === 'jd') {
    // 京东 CDN 通常支持 !w_h.jpg 或 _wxh.jpg 后缀
    if (imageUrl.indexOf('.jpg') > -1 || imageUrl.indexOf('.png') > -1) {
      return imageUrl.replace(/\.(jpg|png|jpeg)$/i, '!240x240.$1');
    }
    return imageUrl + '!240x240.jpg';
  }
  return imageUrl;
}

/**
 * 解析 content 字段
 * 如果是 JSON 格式的商品数据，返回结构化对象；否则返回纯文本摘要
 */
function parseContent(content) {
  if (!content) return { isProduct: false, text: '' };

  // 尝试解析为 JSON
  try {
    const data = JSON.parse(content);
    if (data && (data.platform || data.originalPrice !== undefined)) {
      // 计算折扣信息
      const originalPrice = parseFloat(data.originalPrice) || 0;
      const couponAmount = parseFloat(data.couponAmount) || 0;
      const couponPrice = parseFloat(data.couponPrice) || 0;
      // 实际到手价 = 原价 - 优惠券金额，如果没有 couponPrice 则用 originalPrice - couponAmount
      const finalPrice = couponPrice > 0 ? couponPrice : (originalPrice > couponAmount ? originalPrice - couponAmount : originalPrice);
      const discount = originalPrice > 0 ? Math.round((1 - finalPrice / originalPrice) * 100) : 0;

      return {
        isProduct: true,
        imageUrl: data.imageUrl || '',
        thumbUrl: getThumbUrl(data.imageUrl, data.platform),
        originalPrice: originalPrice.toFixed(2),
        finalPrice: finalPrice.toFixed(2),
        couponAmount: couponAmount.toFixed(2),
        discount: discount,
        platform: data.platform || '',
        platformGoodsId: data.platformGoodsId || '',
        remainQuantity: data.remainQuantity || 0,
        salesTip: data.salesTip || '',
        shopName: data.shopName || '',
        brandName: data.brandName || '',
        tags: data.tags || []
      };
    }
  } catch (e) {
    // 不是 JSON，按纯文本处理
  }

  // 纯文本摘要
  const text = content.replace(/<[^>]+>/g, '');
  return { isProduct: false, text: text.length > 80 ? text.substring(0, 80) + '...' : text };
}

/**
 * 根据商品数据生成简化的标题描述
 * 突出优惠力度
 */
function simplifyTitle(title, product) {
  if (!product || !product.isProduct) return title || '';

  const t = title || '';
  // 截断过长标题
  const maxLen = 40;
  return t.length > maxLen ? t.substring(0, maxLen) + '...' : t;
}

/**
 * 判断是否为新上内容（3天内创建）
 */
function isNew(createdAt) {
  if (!createdAt) return false;
  const date = new Date(createdAt);
  const now = new Date();
  return (now - date) < 3 * 24 * 3600 * 1000;
}

/**
 * 截取摘要（兼容旧调用）
 */
function getSummary(content, maxLen = 80) {
  const parsed = parseContent(content);
  if (parsed.isProduct) return '';
  return parsed.text;
}

/**
 * 防抖
 */
function debounce(fn, delay = 500) {
  let timer = null;
  return function (...args) {
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => fn.apply(this, args), delay);
  };
}

module.exports = { formatTime, formatDateTime, statusText, statusClass, getSummary, parseContent, simplifyTitle, isNew, debounce };
