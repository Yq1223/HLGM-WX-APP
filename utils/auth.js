/**
 * 登录鉴权工具
 */

const app = getApp();

/**
 * 确保已登录，未登录则弹窗引导
 */
function ensureLogin() {
  return new Promise((resolve, reject) => {
    if (app.isLoggedIn()) {
      resolve(app.globalData.userInfo);
      return;
    }

    wx.showModal({
      title: '提示',
      content: '该功能需要登录，是否使用微信一键登录？',
      confirmText: '去登录',
      confirmColor: '#FF6B35',
      success(res) {
        if (res.confirm) {
          app.wxLogin().then(resolve).catch(err => {
            wx.showToast({ title: '登录失败', icon: 'none' });
            reject(err);
          });
        } else {
          reject('用户取消登录');
        }
      }
    });
  });
}

/**
 * 检查是否是管理员
 */
function isAdmin() {
  const userInfo = app.globalData.userInfo;
  return userInfo && userInfo.role === 1;
}

/**
 * 检查是否是本人
 */
function isOwner(userId) {
  const userInfo = app.globalData.userInfo;
  return userInfo && userInfo.userId === userId;
}

module.exports = { ensureLogin, isAdmin, isOwner };
