const { request } = require('../../utils/request');
const { ensureLogin, isAdmin } = require('../../utils/auth');
const app = getApp();

Page({
  data: {
    status: '',
    list: [],
    pageNum: 1,
    pageSize: 20,
    loading: false,
    noMore: false
  },

  onLoad(options) {
    if (options.status !== undefined && options.status !== '') {
      this.setData({ status: parseInt(options.status) });
    }
    // 管理员订阅审核通知
    this._subscribeReview();
  },

  onShow() {
    this.loadList(true);
  },

  // 订阅审核通知（管理员进入管理页时触发）
  _subscribeReview() {
    if (!wx.requestSubscribeMessage) return;
    wx.requestSubscribeMessage({
      tmplIds: [app.globalData.reviewTplId],
      success(res) {
        console.log('订阅审核通知成功:', res);
      },
      fail(err) {
        console.log('订阅审核通知失败:', err);
      }
    });
  },

  onPullDownRefresh() {
    this.loadList(true).finally(() => wx.stopPullDownRefresh());
  },

  onReachBottom() {
    if (!this.data.loading && !this.data.noMore) {
      this.loadList(false);
    }
  },

  loadList(refresh) {
    if (this.data.loading) return Promise.resolve();
    const pageNum = refresh ? 1 : this.data.pageNum;
    this.setData({ loading: true });

    const params = { pageNum, pageSize: this.data.pageSize };
    if (this.data.status !== '' && this.data.status !== undefined) {
      params.status = this.data.status;
    }

    return request({
      url: '/api/admin/wool/list',
      data: params
    }).then(res => {
      const records = (res.data.records || []).map(item => ({
        ...item,
        createdAt: item.createdAt ? item.createdAt.replace('T', ' ').substring(0, 16) : ''
      }));
      this.setData({
        list: refresh ? records : this.data.list.concat(records),
        pageNum: pageNum + 1,
        noMore: records.length < this.data.pageSize,
        loading: false
      });
    }).catch(() => {
      this.setData({ loading: false });
    });
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id });
  },

  switchStatus(e) {
    const status = e.currentTarget.dataset.status;
    this.setData({ status: status === this.data.status ? '' : status, noMore: false });
    this.loadList(true);
  }
});