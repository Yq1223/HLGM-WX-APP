const { request } = require('../../utils/request');
const { formatTime } = require('../../utils/util');
const app = getApp();

Page({
  data: {
    totalPoints: 0,
    list: [],
    pageNum: 1,
    pageSize: 20,
    loading: false,
    noMore: false
  },

  onLoad() {
    this.setData({ totalPoints: (app.globalData.userInfo || {}).points || 0 });
    this.loadList(true);
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

    return request({
      url: '/api/points/log',
      data: { pageNum, pageSize: this.data.pageSize }
    }).then(res => {
      const records = (res.data.records || []).map(item => ({
        ...item,
        createdAt: formatTime(item.createdAt)
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
  }
});
