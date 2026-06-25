const { request } = require('../../utils/request');
const { formatTime } = require('../../utils/util');

Page({
  data: {
    feedbackList: [],
    currentTab: -1,
    pageNum: 1,
    pageSize: 10,
    loading: false,
    noMore: false
  },

  onShow() {
    this.loadFeedbackList(true);
  },

  onPullDownRefresh() {
    this.loadFeedbackList(true).finally(() => wx.stopPullDownRefresh());
  },

  onReachBottom() {
    if (!this.data.loading && !this.data.noMore) {
      this.loadFeedbackList(false);
    }
  },

  // 加载反馈列表
  loadFeedbackList(refresh) {
    if (this.data.loading) return Promise.resolve();
    const pageNum = refresh ? 1 : this.data.pageNum;
    this.setData({ loading: true });

    const params = { pageNum, pageSize: this.data.pageSize };
    if (this.data.currentTab >= 0) {
      params.status = this.data.currentTab;
    }

    return request({
      url: '/api/feedback/mine',
      data: params
    }).then(res => {
      const records = (res.data.records || []).map(item => ({
        ...item,
        createdAt: formatTime(item.createdAt)
      }));
      this.setData({
        feedbackList: refresh ? records : this.data.feedbackList.concat(records),
        pageNum: pageNum + 1,
        noMore: records.length < this.data.pageSize,
        loading: false
      });
    }).catch(() => {
      this.setData({ loading: false });
    });
  },

  // 切换Tab
  switchTab(e) {
    const tab = parseInt(e.currentTarget.dataset.tab);
    if (tab === this.data.currentTab) return;
    this.setData({ currentTab: tab, noMore: false });
    this.loadFeedbackList(true);
  },

  // 跳转到提交反馈页面
  goSubmit() {
    wx.navigateTo({ url: '/pages/feedback-submit/feedback-submit' });
  },

  // 跳转到反馈详情
  goDetail(e) {
    wx.navigateTo({ url: '/pages/feedback-detail/feedback-detail?id=' + e.currentTarget.dataset.id });
  }
});
