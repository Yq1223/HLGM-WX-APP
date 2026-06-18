const { request } = require('../../utils/request');
const { formatTime } = require('../../utils/util');

Page({
  data: {
    list: [],
    filterStatus: null,
    pageNum: 1,
    pageSize: 10,
    loading: false,
    noMore: false
  },

  onLoad(options) {
    if (options.status !== undefined && options.status !== '') {
      this.setData({ filterStatus: parseInt(options.status) });
    }
    this.loadList(true);
  },

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

    const data = { pageNum, pageSize: this.data.pageSize };
    if (this.data.filterStatus !== null) {
      data.status = this.data.filterStatus;
    }

    return request({ url: '/api/admin/wool/list', data }).then(res => {
      const records = (res.data.records || []).map(item => ({
        ...item,
        createdAt: formatTime(item.createdAt),
        content: item.content ? item.content.replace(/<[^>]+>/g, '').substring(0, 100) : ''
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

  setFilter(e) {
    const status = e.currentTarget.dataset.status;
    const filterStatus = status === '' ? null : parseInt(status);
    if (filterStatus === this.data.filterStatus) return;
    this.setData({ filterStatus, noMore: false });
    this.loadList(true);
  },

  onAudit(e) {
    const id = e.currentTarget.dataset.id;
    const action = parseInt(e.currentTarget.dataset.action);

    if (action === 1) {
      wx.showModal({
        title: '审核通过',
        content: '确定通过该信息吗？',
        confirmColor: '#52C41A',
        success: (res) => {
          if (res.confirm) {
            request({
              url: '/api/admin/wool/audit/' + id,
              method: 'POST',
              data: { action: 1 }
            }).then(() => {
              wx.showToast({ title: '已通过', icon: 'success' });
              this.loadList(true);
            });
          }
        }
      });
    } else {
      wx.showModal({
        title: '驳回信息',
        editable: true,
        placeholderText: '请输入驳回理由（选填）',
        confirmColor: '#FF4D4F',
        success: (res) => {
          if (res.confirm) {
            request({
              url: '/api/admin/wool/audit/' + id,
              method: 'POST',
              data: { action: 2, rejectReason: res.content || '' }
            }).then(() => {
              wx.showToast({ title: '已驳回', icon: 'success' });
              this.loadList(true);
            });
          }
        }
      });
    }
  },

  onToggle(e) {
    const id = e.currentTarget.dataset.id;
    const online = e.currentTarget.dataset.online === 'true';
    const url = online ? '/api/admin/wool/online/' + id : '/api/admin/wool/offline/' + id;
    request({
      url: url,
      method: 'PUT'
    }).then(() => {
      wx.showToast({ title: online ? '已上线' : '已下线', icon: 'success' });
      this.loadList(true);
    });
  },

  onDelete(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '确认删除',
      content: '删除后不可恢复',
      confirmColor: '#FF4D4F',
      success: (res) => {
        if (res.confirm) {
          request({
            url: '/api/admin/wool/delete/' + id,
            method: 'DELETE'
          }).then(() => {
            wx.showToast({ title: '已删除', icon: 'success' });
            this.loadList(true);
          });
        }
      }
    });
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id });
  }
});
