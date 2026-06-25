const { request } = require('../../utils/request');
const { formatTime, getSummary } = require('../../utils/util');
const app = getApp();

Page({
  data: {
    list: [],
    keyword: '',
    category: '',  // 当前选中的分类
    categories: ['会员', '话费', '外卖', '电商', '生活', '出行', '美食', '其他'],
    pageNum: 1,
    pageSize: 10,
    loading: false,
    noMore: false,
    lastRefreshTime: 0
  },

  onLoad() {
    this.loadList(true);
  },

  onShow() {
    // 发布/编辑完成后跳转回来，自动刷新列表
    if (app.globalData.needRefreshList) {
      app.globalData.needRefreshList = false;
      this.loadList(true);
      return;
    }
    // 每次页面显示时，如果距离上次刷新超过3秒，自动刷新
    const now = Date.now();
    if (now - this.data.lastRefreshTime > 3000) {
      this.loadList(true);
    }
  },

  onPullDownRefresh() {
    this.loadList(true).finally(() => {
      wx.stopPullDownRefresh();
    });
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
      url: '/api/wool/list',
      data: {
        pageNum: pageNum,
        pageSize: this.data.pageSize,
        keyword: this.data.keyword || '',
        category: this.data.category || ''
      }
    }).then(res => {
      const records = (res.data.records || []).map(item => ({
        ...item,
        createdAt: formatTime(item.createdAt),
        content: getSummary(item.content)
      }));

      this.setData({
        list: refresh ? records : this.data.list.concat(records),
        pageNum: pageNum + 1,
        noMore: records.length < this.data.pageSize,
        loading: false,
        lastRefreshTime: Date.now()
      });
    }).catch(() => {
      this.setData({ loading: false });
    });
  },

  onSearchInput(e) {
    this.setData({ keyword: e.detail.value });
  },

  onSearch() {
    this.loadList(true);
  },

  clearSearch() {
    this.setData({ keyword: '' });
    this.loadList(true);
  },

  // 切换分类筛选
  onCategory(e) {
    const val = e.currentTarget.dataset.val;
    const category = this.data.category === val ? '' : val;
    this.setData({ category });
    this.loadList(true);
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id });
  },

  goPublish() {
    wx.switchTab({ url: '/pages/publish/publish' });
  }
});
