const { request } = require('../../utils/request');
const { formatTime } = require('../../utils/util');
const { ensureLogin } = require('../../utils/auth');
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
    tempNickname: '',
    isCachedUser: false,  // 是否有历史用户信息
    // 订阅状态
    subscribed: false
  },

  onShow() {
    const userInfo = app.globalData.userInfo;
    const isAdmin = userInfo && userInfo.role === 1;
    this.setData({
      userInfo: userInfo,
      isAdmin: isAdmin
    });
    if (userInfo) {
      this.loadMyList(true);
      if (isAdmin) this._checkSubscribeStatus();
    } else {
      this.setData({ myList: [], noMore: false, subscribed: false });
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
    wx.showLoading({ title: '登录中...', mask: true });

    // 调用后端检查是否已注册
    app.wxLogin().then(result => {
      wx.hideLoading();

      if (result.needRegister) {
        // 新用户，显示选择界面
        this.setData({
          showLoginDialog: true,
          tempAvatarUrl: '',
          tempNickname: '',
          isCachedUser: false,
          pendingCode: result.code  // 保存 code 用于后续注册
        });
      } else {
        // 已有用户，显示信息（禁用状态）
        const data = result.data;
        this.setData({
          showLoginDialog: true,
          tempAvatarUrl: data.avatarUrl || '',
          tempNickname: data.nickname || '',
          isCachedUser: true
        });
      }
    }).catch(err => {
      wx.hideLoading();
      wx.showToast({ title: '登录失败', icon: 'none' });
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

  confirmLogin() {
    const { isCachedUser, tempNickname, tempAvatarUrl, pendingCode } = this.data;

    if (isCachedUser) {
      // 已有用户，登录已完成，直接关闭弹窗
      this.setData({
        showLoginDialog: false,
        userInfo: app.globalData.userInfo,
        isAdmin: app.globalData.userInfo.role === 1
      });
      this.loadMyList(true);
      wx.showToast({ title: '登录成功', icon: 'success' });
    } else {
      // 新用户，调用注册接口
      const nickname = tempNickname || '微信用户';
      const avatarUrl = tempAvatarUrl || '';

      wx.showLoading({ title: '注册中...', mask: true });

      app.wxRegister(pendingCode, nickname, avatarUrl).then(data => {
        wx.hideLoading();
        this.setData({
          showLoginDialog: false,
          userInfo: app.globalData.userInfo,
          isAdmin: data.role === 1
        });
        this.loadMyList(true);
        wx.showToast({ title: '注册成功', icon: 'success' });
      }).catch(err => {
        wx.hideLoading();
        wx.showToast({ title: err || '注册失败', icon: 'none' });
      });
    }
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
    ensureLogin().then(() => {
      wx.navigateTo({ url: '/pages/points/points' });
    }).catch(() => {});
  },

  goExchange() {
    ensureLogin().then(() => {
      wx.navigateTo({ url: '/pages/exchange/exchange' });
    }).catch(() => {});
  },

  goExchangeRecord() {
    ensureLogin().then(() => {
      wx.navigateTo({ url: '/pages/exchange-record/exchange-record' });
    }).catch(() => {});
  },

  goAdmin(e) {
    const filter = e.currentTarget.dataset.filter;
    let url = '/pages/admin/admin';
    if (filter !== undefined && filter !== '') {
      url += '?status=' + filter;
    }
    wx.navigateTo({ url });
  },

  goFeedback() {
    ensureLogin().then(() => {
      wx.navigateTo({ url: '/pages/feedback/feedback' });
    }).catch(() => {});
  },

  goFeedbackAdmin() {
    wx.navigateTo({ url: '/pages/feedback-admin/feedback-admin' });
  },

  // 查询订阅状态
  _checkSubscribeStatus() {
    request({
      url: '/api/admin/subscribe/status',
      method: 'GET'
    }).then(res => {
      this.setData({ subscribed: res.data.subscribed });
    }).catch(() => {});
  },

  // 用户点击订阅按钮（切换开启/关闭）
  onSubscribeReview() {
    const tplId = app.globalData.reviewTplId;
    const isSubscribed = this.data.subscribed;

    if (!isSubscribed) {
      // 未订阅，先弹微信授权
      if (!wx.requestSubscribeMessage) {
        wx.showToast({ title: '当前微信版本不支持订阅消息', icon: 'none' });
        return;
      }
      wx.requestSubscribeMessage({
        tmplIds: [tplId],
        success: (res) => {
          if (res[tplId] === 'accept') {
            this._toggleSubscribe(tplId);
          } else if (res[tplId] === 'reject') {
            // 用户拒绝过，引导去设置手动开启
            wx.showModal({
              title: '订阅被拒绝',
              content: '您之前拒绝了订阅通知，请前往小程序设置手动开启',
              confirmText: '去设置',
              success: (modalRes) => {
                if (modalRes.confirm) {
                  wx.openSetting();
                }
              }
            });
          }
        },
        fail: () => {
          wx.showToast({ title: '订阅失败，请重试', icon: 'none' });
        }
      });
    } else {
      // 已订阅，直接关闭
      this._toggleSubscribe(tplId);
    }
  },

  // 切换订阅状态
  _toggleSubscribe(templateId) {
    request({
      url: '/api/admin/subscribe/toggle',
      method: 'POST',
      data: { templateId }
    }).then(res => {
      const subscribed = res.data.subscribed;
      this.setData({ subscribed });
      wx.showToast({
        title: subscribed ? '已开启审核通知' : '已关闭审核通知',
        icon: 'success'
      });
    }).catch(err => {
      console.error('切换订阅状态失败:', err);
      wx.showToast({ title: '操作失败，请重试', icon: 'none' });
    });
  }
});