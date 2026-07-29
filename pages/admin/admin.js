const { request } = require('../../utils/request');
const { ensureLogin, isAdmin } = require('../../utils/auth');
const app = getApp();

Page({
  data: {
    filterStatus: null,
    filterPointsPending: false,
    list: [],
    pageNum: 1,
    pageSize: 20,
    loading: false,
    noMore: false,
    subscribed: false,
    migrated: false
  },

  onLoad(options) {
    if (options.status !== undefined && options.status !== '') {
      this.setData({ filterStatus: parseInt(options.status) });
    }
    this._checkSubscribeStatus();
  },

  onShow() {
    this.loadList(true);
  },

  // ========== 订阅相关 ==========

  _checkSubscribeStatus() {
    request({
      url: '/api/admin/subscribe/status',
      method: 'GET'
    }).then(res => {
      this.setData({ subscribed: res.data.subscribed });
    }).catch(() => {});
  },

  onSubscribeReview() {
    const tplId = app.globalData.reviewTplId;
    const isSubscribed = this.data.subscribed;

    if (!isSubscribed) {
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
      this._toggleSubscribe(tplId);
    }
  },

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
  },

  // ========== 列表相关 ==========

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

    const params = { pageNum, pageSize: this.data.pageSize };
    if (this.data.filterPointsPending) {
      params.status = 1;
      params.pointsPending = true;
    } else if (this.data.filterStatus !== null && this.data.filterStatus !== '' && this.data.filterStatus !== undefined) {
      params.status = this.data.filterStatus;
    }

    return request({
      url: '/api/admin/wool/list',
      data: params
    }).then(res => {
      const records = (res.data.records || []).map(item => ({
        ...item,
        createdAt: item.createdAt ? item.createdAt.replace('T', ' ').substring(0, 16) : ''
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
  },

  // ========== 操作事件 ==========

  goDetail(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id });
  },

  // 筛选Tab
  setFilter(e) {
    const status = e.currentTarget.dataset.status;
    const newStatus = (status !== 0 && !status) ? null : status;
    this.setData({ filterStatus: newStatus, filterPointsPending: false, noMore: false });
    this.loadList(true);
  },

  // 筛选待积分审批
  setFilterPointsPending() {
    this.setData({ filterPointsPending: true, filterStatus: null, noMore: false });
    this.loadList(true);
  },

  // 审核（通过/驳回）
  onAudit(e) {
    const id = e.currentTarget.dataset.id;
    const action = parseInt(e.currentTarget.dataset.action);

    if (action === 2) {
      wx.showModal({
        title: '驳回理由',
        editable: true,
        placeholderText: '请输入驳回理由',
        success: (res) => {
          if (res.confirm) {
            this._doAudit(id, action, res.content || '');
          }
        }
      });
    } else {
      this._doAudit(id, action, '');
    }
  },

  _doAudit(id, action, rejectReason) {
    request({
      url: '/api/admin/wool/audit/' + id,
      method: 'POST',
      data: { action, rejectReason }
    }).then(() => {
      wx.showToast({ title: action === 1 ? '已通过' : '已驳回', icon: 'success' });
      this.loadList(true);
    }).catch(() => {});
  },

  // 上线/下线
  onToggle(e) {
    const id = e.currentTarget.dataset.id;
    const online = e.currentTarget.dataset.online === 'true' || e.currentTarget.dataset.online === true;
    const url = '/api/admin/wool/' + (online ? 'online' : 'offline') + '/' + id;

    request({ url, method: 'PUT' }).then(() => {
      wx.showToast({ title: online ? '已上线' : '已下线', icon: 'success' });
      this.loadList(true);
    }).catch(() => {});
  },

  // 迁移旧分类
  onMigrateCategories() {
    wx.showModal({
      title: '迁移旧分类',
      content: '将旧的 pdd/jd 分类自动迁移为智能分类（会员/话费/外卖/电商等），是否执行？',
      confirmText: '开始迁移',
      confirmColor: '#FF6B35',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '迁移中...', mask: true });
          request({
            url: '/api/admin/wool/migrate-categories',
            method: 'POST'
          }).then(res => {
            wx.hideLoading();
            this.setData({ migrated: true });
            wx.showToast({ title: res.data.message || '迁移完成', icon: 'success', duration: 2000 });
            this.loadList(true);
          }).catch(err => {
            wx.hideLoading();
            wx.showToast({ title: err.msg || '迁移失败', icon: 'none' });
          });
        }
      }
    });
  },

  // 删除
  onDelete(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '确认删除',
      content: '删除后不可恢复，确定删除？',
      success: (res) => {
        if (res.confirm) {
          request({
            url: '/api/admin/wool/delete/' + id,
            method: 'DELETE'
          }).then(() => {
            wx.showToast({ title: '已删除', icon: 'success' });
            this.loadList(true);
          }).catch(() => {});
        }
      }
    });
  }
});
