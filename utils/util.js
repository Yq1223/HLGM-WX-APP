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
 * 截取摘要
 */
function getSummary(content, maxLen = 80) {
  if (!content) return '';
  // 去除HTML标签
  const text = content.replace(/<[^>]+>/g, '');
  return text.length > maxLen ? text.substring(0, maxLen) + '...' : text;
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

module.exports = { formatTime, formatDateTime, statusText, statusClass, getSummary, debounce };
