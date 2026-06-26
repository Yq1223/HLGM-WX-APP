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
          app.wxLogin().then(result => {
            if (result.needRegister) {
              // 新用户需要注册，引导去「我的」页面完成注册
              wx.showModal({
                title: '完善信息',
                content: '首次使用需要设置昵称，是否前往注册？',
                confirmText: '去注册',
                confirmColor: '#FF6B35',
                success(modalRes) {
                  if (modalRes.confirm) {
                    wx.switchTab({ url: '/pages/mine/mine' });
                  }
                  reject('需要注册');
                }
              });
            } else {
              resolve(result);
            }
          }).catch(err => {
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
