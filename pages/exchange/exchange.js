Page({
  data: {
    notified: false
  },

  onLoad() {
    const notified = wx.getStorageSync('exchange_notify');
    if (notified) this.setData({ notified: true });
  },

  onNotify() {
    if (this.data.notified) {
      wx.showToast({ title: '您已订阅过', icon: 'none' });
      return;
    }
    wx.requestSubscribeMessage({
      tmplIds: [],
      success: () => {
        this.setData({ notified: true });
        wx.setStorageSync('exchange_notify', true);
        wx.showToast({ title: '订阅成功', icon: 'success' });
      },
      fail: () => {
        // 用户拒绝也标记本地状态
        this.setData({ notified: true });
        wx.setStorageSync('exchange_notify', true);
        wx.showToast({ title: '已记录您的关注', icon: 'none' });
      }
    });
  }
});
