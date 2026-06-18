const { request } = require('../../utils/request');
const { formatTime } = require('../../utils/util');
const app = getApp();

Page({
  data: {
    userInfo: null,
    isAdmin: false,
    myList: [],
    currentTab: -1,
    pageNum: 1,
    pageSize: 10,
    loading: false,
    noMore: false,
    // 登录弹窗
    showLoginDialog: false,
    tempAvatarUrl: '',
    tempNickname: ''
  },

  onShow() {
    const userInfo = app.globalData.userInfo;
    this.setData({
      userInfo: userInfo,
      isAdmin: userInfo && userInfo.role === 1
    });
    if (userInfo) {
      this.loadMyList(true);
    } else {
      this.setData({ myList: [], noMore: false });
    }
  },

  onPullDownRefresh() {
    if (this.data.userInfo) {
      this.loadMyList(true).finally(() => wx.stopPullDownRefresh());
    } else {
      wx.stopPullDownRefresh();
    }
  },

  onReachBottom() {
    if (this.data.userInfo && !this.data.loading && !this.data.noMore) {
      this.loadMyList(false);
    }
  },

  // ========== 登录流程 ==========

  onLogin() {
    // 弹出头像昵称选择弹窗
    this.setData({
      showLoginDialog: true,
      tempAvatarUrl: '',
      tempNickname: ''
    });
  },

  onChooseAvatar(e) {
    const avatarUrl = e.detail.avatarUrl;
    this.setData({ tempAvatarUrl: avatarUrl });
  },

  onNicknameInput(e) {
    this.setData({ tempNickname: e.detail.value });
  },

  onNicknameBlur(e) {
    this.setData({ tempNickname: e.detail.value });
  },

  closeLoginDialog() {
    this.setData({ showLoginDialog: false });
  },

  confirmLogin() {
    const { tempNickname, tempAvatarUrl } = this.data;
    const nickname = tempNickname || '微信用户';
    const avatarUrl = tempAvatarUrl || '';

    wx.showLoading({ title: '登录中...', mask: true });

    app.wxLogin(nickname, avatarUrl).then(data => {
      wx.hideLoading();
      this.setData({
        showLoginDialog: false,
        userInfo: app.globalData.userInfo,
        isAdmin: data.role === 1
      });
      this.loadMyList(true);
      wx.showToast({ title: '登录成功', icon: 'success' });
    }).catch(err => {
      wx.hideLoading();
      wx.showToast({ title: '登录失败', icon: 'none' });
    });
  },

  onChangeAvatar() {
    // 已登录用户更换头像
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const avatarUrl = res.tempFiles[0].tempFilePath;
        // 这里可以上传头像到服务器，暂时只更新本地
        const userInfo = { ...this.data.userInfo, avatarUrl };
        app.globalData.userInfo = userInfo;
        wx.setStorageSync('userInfo', userInfo);
        this.setData({ userInfo });
      }
    });
  },

  // ========== 加载我的发布 ==========

  loadMyList(refresh) {
    if (this.data.loading) return Promise.resolve();
    const pageNum = refresh ? 1 : this.data.pageNum;
    this.setData({ loading: true });

    const params = { pageNum, pageSize: this.data.pageSize };
    if (this.data.currentTab >= 0) {
      params.status = this.data.currentTab;
    }

    return request({
      url: '/api/wool/mine',
      data: params
    }).then(res => {
      const records = (res.data.records || []).map(item => ({
        ...item,
        createdAt: formatTime(item.createdAt)
      }));
      this.setData({
        myList: refresh ? records : this.data.myList.concat(records),
        pageNum: pageNum + 1,
        noMore: records.length < this.data.pageSize,
        loading: false
      });
    }).catch(() => {
      this.setData({ loading: false });
    });
  },

  // ========== 切换Tab ==========

  switchTab(e) {
    const tab = parseInt(e.currentTarget.dataset.tab);
    if (tab === this.data.currentTab) return;
    this.setData({ currentTab: tab, noMore: false });
    this.loadMyList(true);
  },

  // ========== 跳转 ==========

  goDetail(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id });
  },

  goPoints() {
    wx.navigateTo({ url: '/pages/points/points' });
  },

  goExchange() {
    wx.navigateTo({ url: '/pages/exchange/exchange' });
  },

  goExchangeRecord() {
    wx.navigateTo({ url: '/pages/exchange-record/exchange-record' });
  },

  goAdmin(e) {
    const filter = e.currentTarget.dataset.filter;
    let url = '/pages/admin/admin';
    if (filter !== undefined && filter !== '') {
      url += '?status=' + filter;
    }
    wx.navigateTo({ url });
  }
});
