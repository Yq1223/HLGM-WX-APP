/**
 * 统一 HTTP 请求封装
 * - 自动携带 Authorization header
 * - 统一错误处理
 * - 401 自动跳转登录
 */

const app = getApp();

function request(options) {
  const { url, method = 'GET', data, header = {}, showLoading = false } = options;

  return new Promise((resolve, reject) => {
    if (showLoading) {
      wx.showLoading({ title: '加载中...', mask: true });
    }

    // 拼接完整URL
    const fullUrl = url.startsWith('http') ? url : (app.globalData.baseUrl + url);

    // 构建请求头
    const reqHeader = {
      'Content-Type': 'application/json',
      ...header
    };

    const token = app.getToken();
    if (token) {
      reqHeader['Authorization'] = 'Bearer ' + token;
    }

    wx.request({
      url: fullUrl,
      method: method,
      data: data,
      header: reqHeader,
      success(res) {
        if (showLoading) wx.hideLoading();

        if (res.statusCode === 200) {
          const body = res.data;
          if (body.code === 0) {
            resolve(body);
          } else {
            wx.showToast({ title: body.msg || '请求失败', icon: 'none' });
            reject(body);
          }
        } else if (res.statusCode === 401) {
          // token过期或未登录
          app.clearLoginInfo();
          wx.showModal({
            title: '提示',
            content: '登录已过期，请重新登录',
            showCancel: false,
            success() {
              wx.switchTab({ url: '/pages/mine/mine' });
            }
          });
          reject({ code: 401, msg: '未登录' });
        } else {
          wx.showToast({ title: '服务器错误', icon: 'none' });
          reject({ code: res.statusCode, msg: '服务器错误' });
        }
      },
      fail(err) {
        if (showLoading) wx.hideLoading();
        wx.showToast({ title: '网络异常', icon: 'none' });
        reject({ code: -1, msg: '网络异常', detail: err });
      }
    });
  });
}

/**
 * 上传文件
 */
function uploadFile(options) {
  const { url, filePath, name = 'file', formData = {} } = options;
  const fullUrl = url.startsWith('http') ? url : (app.globalData.baseUrl + url);

  return new Promise((resolve, reject) => {
    const token = app.getToken();
    wx.uploadFile({
      url: fullUrl,
      filePath: filePath,
      name: name,
      formData: formData,
      header: {
        'Authorization': token ? 'Bearer ' + token : ''
      },
      success(res) {
        if (res.statusCode === 200) {
          const body = JSON.parse(res.data);
          if (body.code === 0) {
            resolve(body);
          } else {
            wx.showToast({ title: body.msg || '上传失败', icon: 'none' });
            reject(body);
          }
        } else if (res.statusCode === 401) {
          app.clearLoginInfo();
          wx.showToast({ title: '请先登录', icon: 'none' });
          reject({ code: 401 });
        } else {
          wx.showToast({ title: '上传失败', icon: 'none' });
          reject({ code: res.statusCode });
        }
      },
      fail(err) {
        wx.showToast({ title: '网络异常', icon: 'none' });
        reject(err);
      }
    });
  });
}

module.exports = { request, uploadFile };
