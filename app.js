App({
  globalData: {
    userInfo: null,
    token: '',
    // ========== 微信云托管配置 ==========
    cloudEnv: 'prod-d9gtitzfjf25274f9',     // 环境ID
    serviceName: 'springboot-99oi',          // 服务名称
    // 云托管公网域名（仅用于文件上传，因 callContainer 不支持 multipart）
    cloudDomain: '',
    reviewTplId: 'B9vbbAbrIXKs-bixV75Lm9lcCO1pfZZSbr0LZyHDMwo'
  },

  onLaunch() {
    // 初始化微信云开发能力
    if (wx.cloud) {
      wx.cloud.init({
        env: this.globalData.cloudEnv
      });
    }

    // 从本地缓存恢复登录状态
    const token = wx.getStorageSync('token');
    const userInfo = wx.getStorageSync('userInfo');
    if (token && userInfo) {
      this.globalData.token = token;
      this.globalData.userInfo = userInfo;

      // 用 token 向后端刷新最新用户信息（角色、积分等）
      const { request } = require('./utils/request');
      request({
        url: '/api/auth/me',
        method: 'GET'
      }).then(res => {
        const latest = res.data;
        this.globalData.userInfo = latest;
        wx.setStorageSync('userInfo', latest);
      }).catch(() => {
        // token 过期或无效，清除登录状态
        this.clearLoginInfo();
      });
    }
  },

  // 检查是否已登录
  isLoggedIn() {
    return !!this.globalData.token;
  },

  // 获取token
  getToken() {
    return this.globalData.token;
  },

  // 保存登录信息
  setLoginInfo(token, userInfo) {
    this.globalData.token = token;
    this.globalData.userInfo = userInfo;
    wx.setStorageSync('token', token);
    wx.setStorageSync('userInfo', userInfo);
  },

  // 清除登录信息
  clearLoginInfo() {
    this.globalData.token = '';
    this.globalData.userInfo = null;
    wx.removeStorageSync('token');
    wx.removeStorageSync('userInfo');
  },

  // 微信登录（支持传入昵称和头像）
  wxLogin(nickname, avatarUrl) {
    return new Promise((resolve, reject) => {
      wx.login({
        success: (loginRes) => {
          if (loginRes.code) {
            const { request } = require('./utils/request');
            request({
              url: '/api/auth/login',
              method: 'POST',
              data: {
                code: loginRes.code,
                nickname: nickname || '',
                avatarUrl: avatarUrl || ''
              }
            }).then(res => {
              if (res.code === 0) {
                const data = res.data;
                this.setLoginInfo(data.token, {
                  userId: data.userId,
                  nickname: data.nickname,
                  avatarUrl: data.avatarUrl,
                  role: data.role,
                  points: data.points
                });
                resolve(data);
              } else {
                reject(res.msg);
              }
            }).catch(reject);
          } else {
            reject('wx.login失败');
          }
        },
        fail: reject
      });
    });
  }
});
