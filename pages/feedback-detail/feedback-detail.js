const { request } = require('../../utils/request');
const { formatTime } = require('../../utils/util');
const app = getApp();

Page({
  data: {
    feedbackId: null,
    feedback: {},
    isAdmin: false,
    selectedStatus: 0,
    replyContent: '',
    submitting: false
  },

  onLoad(options) {
    this.setData({
      feedbackId: options.id,
      fromAdmin: options.from === 'admin',
      isAdmin: app.globalData.userInfo && app.globalData.userInfo.role === 1
    });
    this.loadDetail();
  },

  // 加载反馈详情
  loadDetail() {
    request({
      url: '/api/feedback/detail/' + this.data.feedbackId
    }).then(res => {
      const feedback = res.data;
      feedback.createdAt = formatTime(feedback.createdAt);
      feedback.updatedAt = formatTime(feedback.updatedAt);
      this.setData({
        feedback,
        selectedStatus: feedback.status,
        replyContent: feedback.reply || ''
      });
    });
  },

  // 选择状态
  selectStatus(e) {
    const status = parseInt(e.currentTarget.dataset.status);
    this.setData({ selectedStatus: status });
  },

  // 输入回复
  onReplyInput(e) {
    this.setData({ replyContent: e.detail.value });
  },

  // 提交处理
  onAudit() {
    const { feedbackId, selectedStatus, replyContent } = this.data;

    this.setData({ submitting: true });

    request({
      url: '/api/feedback/audit/' + feedbackId,
      method: 'PUT',
      data: {
        status: selectedStatus,
        reply: replyContent.trim()
      }
    }).then(() => {
      wx.showToast({
        title: '处理成功',
        icon: 'success',
        duration: 2000
      });
      this.loadDetail();
    }).catch(() => {}).finally(() => {
      this.setData({ submitting: false });
    });
  }
});
