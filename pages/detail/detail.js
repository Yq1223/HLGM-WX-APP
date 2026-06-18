const { request } = require('../../utils/request');
const { ensureLogin, isAdmin, isOwner } = require('../../utils/auth');
const { formatDateTime } = require('../../utils/util');
const app = getApp();

Page({
  data: {
    id: null,
    info: null,
    loading: true,
    isOwner: false,
    isAdmin: false
  },

  onLoad(options) {
    this.setData({ id: options.id });
    this.loadDetail();
  },

  loadDetail() {
    this.setData({ loading: true });
    request({
      url: '/api/wool/detail/' + this.data.id
    }).then(res => {
      const info = res.data;
      info.createdAt = formatDateTime(info.createdAt);
      const userInfo = app.globalData.userInfo || {};
      this.setData({
        info: info,
        loading: false,
        isOwner: isOwner(info.userId),
        isAdmin: isAdmin()
      });
    }).catch(err => {
      if (err.code === 401) {
        // 未登录，引导登录
        ensureLogin().then(() => {
          this.loadDetail();
        }).catch(() => {
          wx.navigateBack();
        });
      } else {
        this.setData({ loading: false });
        wx.showToast({ title: err.msg || '加载失败', icon: 'none' });
      }
    });
  },

  // 复制来源链接
  copySource() {
    wx.setClipboardData({
      data: this.data.info.sourceUrl,
      success() {
        wx.showToast({ title: '链接已复制', icon: 'success' });
      }
    });
  },

  // 编辑
  onEdit() {
    const info = this.data.info;
    // 通过 globalData 传递编辑数据（避免 URL 过长）
    app.globalData.editData = {
      id: info.id,
      title: info.title,
      content: info.content,
      category: info.category || '',
      sourceUrl: info.sourceUrl || '',
      claimSteps: info.claimSteps || ''
    };
    wx.navigateTo({ url: '/pages/publish/publish?mode=edit' });
  },

  // 删除
  onDelete() {
    wx.showModal({
      title: '确认删除',
      content: '删除后不可恢复，确定要删除吗？',
      confirmColor: '#FF4D4F',
      success: (res) => {
        if (res.confirm) {
          request({
            url: '/api/wool/delete/' + this.data.id,
            method: 'DELETE'
          }).then(() => {
            wx.showToast({ title: '已删除', icon: 'success' });
            setTimeout(() => wx.navigateBack(), 1000);
          });
        }
      }
    });
  },

  // 管理员审核通过
  onAuditPass() {
    wx.showModal({
      title: '审核通过',
      content: '确定通过该信息的审核吗？',
      confirmColor: '#52C41A',
      success: (res) => {
        if (res.confirm) {
          request({
            url: '/api/admin/wool/audit/' + this.data.id,
            method: 'POST',
            data: { action: 1 }
          }).then(() => {
            wx.showToast({ title: '审核通过', icon: 'success' });
            this.loadDetail();
          });
        }
      }
    });
  },

  // 管理员驳回
  onAuditReject() {
    wx.showModal({
      title: '驳回信息',
      editable: true,
      placeholderText: '请输入驳回理由（选填）',
      confirmColor: '#FF4D4F',
      success: (res) => {
        if (res.confirm) {
          request({
            url: '/api/admin/wool/audit/' + this.data.id,
            method: 'POST',
            data: { action: 2, rejectReason: res.content || '' }
          }).then(() => {
            wx.showToast({ title: '已驳回', icon: 'success' });
            this.loadDetail();
          });
        }
      }
    });
  },

  // 上线/下线
  onToggleOnline(e) {
    const online = e.currentTarget.dataset.online === 'true' || e.currentTarget.dataset.online === true;
    wx.showModal({
      title: online ? '确认上线' : '确认下线',
      content: online ? '确定将该信息上线吗？' : '确定将该信息下线吗？',
      confirmColor: '#FF6B35',
      success: (res) => {
        if (res.confirm) {
          const toggleUrl = online
            ? '/api/admin/wool/online/' + this.data.id
            : '/api/admin/wool/offline/' + this.data.id;
          request({
            url: toggleUrl,
            method: 'PUT'
          }).then(() => {
            wx.showToast({ title: online ? '已上线' : '已下线', icon: 'success' });
            this.loadDetail();
          });
        }
      }
    });
  },

  // 管理员删除
  onAdminDelete() {
    wx.showModal({
      title: '确认删除',
      content: '管理员删除不可恢复，确定吗？',
      confirmColor: '#FF4D4F',
      success: (res) => {
        if (res.confirm) {
          request({
            url: '/api/admin/wool/delete/' + this.data.id,
            method: 'DELETE'
          }).then(() => {
            wx.showToast({ title: '已删除', icon: 'success' });
            setTimeout(() => wx.navigateBack(), 1000);
          });
        }
      }
    });
  },

  onShareAppMessage() {
    return {
      title: this.data.info ? this.data.info.title : '薅了个毛',
      path: '/pages/detail/detail?id=' + this.data.id
    };
  }
});
