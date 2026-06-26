const { request } = require('../../utils/request');
const { ensureLogin } = require('../../utils/auth');
const app = getApp();

Page({
  data: {
    form: {
      title: '',
      content: '',
      category: '',
      sourceUrl: '',
      claimSteps: ''
    },
    categories: ['会员', '话费', '外卖', '电商', '生活', '出行', '美食', '其他'],
    editId: null,
    submitting: false
  },

  onLoad(options) {
    const editData = app.globalData.editData;
    if (editData) {
      this.setData({
        editId: editData.id,
        form: {
          title: editData.title || '',
          content: editData.content || '',
          category: editData.category || '',
          sourceUrl: editData.sourceUrl || '',
          claimSteps: editData.claimSteps || ''
        }
      });
      app.globalData.editData = null;
    }
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({
      ['form.' + field]: e.detail.value
    });
  },

  onCategory(e) {
    const val = e.currentTarget.dataset.val;
    this.setData({
      'form.category': this.data.form.category === val ? '' : val
    });
  },

  onSubmit() {
    const { form, editId } = this.data;

    if (!form.title.trim()) {
      wx.showToast({ title: '请输入标题', icon: 'none' });
      return;
    }
    if (form.title.length > 128) {
      wx.showToast({ title: '标题不能超过128字', icon: 'none' });
      return;
    }
    if (!form.content.trim()) {
      wx.showToast({ title: '请输入内容', icon: 'none' });
      return;
    }

    ensureLogin().then(() => {
      this.setData({ submitting: true });

      request({
        url: '/api/wool/update/' + editId,
        method: 'PUT',
        data: {
          title: form.title.trim(),
          content: form.content.trim(),
          category: form.category,
          sourceUrl: form.sourceUrl.trim(),
          claimSteps: form.claimSteps.trim()
        }
      }).then(() => {
        wx.showToast({ title: '修改成功', icon: 'success', duration: 2000 });
        app.globalData.needRefreshList = true;
        setTimeout(() => {
          wx.navigateBack();
        }, 1500);
      }).catch(() => {}).finally(() => {
        this.setData({ submitting: false });
      });
    }).catch(() => {});
  }
});
