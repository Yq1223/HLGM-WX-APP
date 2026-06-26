/**
 * 统一 HTTP 请求封装（微信云托管版本）
 * - 使用 wx.cloud.callContainer 发起请求
 * - 自动携带 Authorization header
 * - 统一错误处理
 * - 401 自动跳转登录
 */

const app = getApp();

/**
 * 标准请求（走 callContainer）
 */
function request(options) {
  const { url, method = 'GET', data, header = {}, showLoading = false, timeout } = options;

  return new Promise((resolve, reject) => {
    if (showLoading) {
      wx.showLoading({ title: '加载中...', mask: true });
    }

    // 构建请求头
    const reqHeader = {
      'Content-Type': 'application/json',
      'X-WX-SERVICE': app.globalData.serviceName,
      ...header
    };

    const token = app.getToken();
    if (token) {
      reqHeader['Authorization'] = 'Bearer ' + token;
    }

    wx.cloud.callContainer({
      config: {
        env: app.globalData.cloudEnv
      },
      path: url,
      method: method,
      data: data,
      header: reqHeader,
      timeout: timeout,
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
          const hasToken = !!app.getToken();
          app.clearLoginInfo();
          if (hasToken) {
            // 有 token 但过期了，提示用户重新登录
            if (!app.globalData.isShowingAuthError) {
              app.globalData.isShowingAuthError = true;
              wx.showModal({
                title: '提示',
                content: '登录已过期，请重新登录',
                showCancel: false,
                success() {
                  app.globalData.isShowingAuthError = false;
                  wx.switchTab({ url: '/pages/mine/mine' });
                }
              });
            }
          }
          // 没有 token 的情况不弹窗，由调用方自行处理（如 detail.js 的 ensureLogin）
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
 * 注意：wx.cloud.callContainer 不支持 multipart/form-data，
 * 因此文件上传需通过云托管公网域名走 wx.uploadFile。
 *
 * 使用前需在 app.js globalData.cloudDomain 中配置云托管公网域名，
 * 格式：https://{env}-{service}.sh.run.tcloudbase.com
 * 并在微信公众平台「开发管理 - 服务器域名」中配置 uploadFile 合法域名。
 */
function uploadFile(options) {
  const { url, filePath, name = 'file', formData = {} } = options;
  const domain = app.globalData.cloudDomain;

  return new Promise((resolve, reject) => {
    const token = app.getToken();

    // 如果配置了公网域名，走 wx.uploadFile
    if (domain) {
      wx.uploadFile({
        url: domain + url,
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
      return;
    }

    // fallback：未配置域名时，尝试读文件为 base64 走 callContainer
    // （需要后端配合改为接收 JSON base64 字段）
    wx.getFileSystemManager().readFile({
      filePath: filePath,
      encoding: 'base64',
      success(fileRes) {
        wx.cloud.callContainer({
          config: { env: app.globalData.cloudEnv },
          path: url,
          method: 'POST',
          header: {
            'Content-Type': 'application/json',
            'X-WX-SERVICE': app.globalData.serviceName,
            'Authorization': token ? 'Bearer ' + token : ''
          },
          data: {
            fileBase64: fileRes.data,
            fileName: filePath.split('/').pop(),
            ...formData
          },
          success(res) {
            if (res.statusCode === 200) {
              const body = res.data;
              if (body.code === 0) {
                resolve(body);
              } else {
                wx.showToast({ title: body.msg || '上传失败', icon: 'none' });
                reject(body);
              }
            } else {
              wx.showToast({ title: '上传失败', icon: 'none' });
              reject(body);
            }
          },
          fail(err) {
            wx.showToast({ title: '网络异常', icon: 'none' });
            reject(err);
          }
        });
      },
      fail(err) {
        wx.showToast({ title: '读取文件失败', icon: 'none' });
        reject(err);
      }
    });
  });
}

module.exports = { request, uploadFile };
