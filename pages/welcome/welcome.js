const app = getApp();

Page({
  data: {},

  onLoad() {
    // 检查是否已看过开屏页
    const seen = wx.getStorageSync('seenWelcome');
    if (seen) {
      // 已看过，直接跳转首页
      wx.switchTab({ url: '/pages/index/index' });
      return;
    }
  },

  enterApp() {
    wx.setStorageSync('seenWelcome', true);
    wx.switchTab({ url: '/pages/index/index' });
  }
});
