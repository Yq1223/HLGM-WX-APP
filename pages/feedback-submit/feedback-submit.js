const { request } = require('../../utils/request');
const { ensureLogin } = require('../../utils/auth');

Page({
  data: {
    title: '',
    content: '',
    submitting: false
  },

  onTitleInput(e) {
    this.setData({ title: e.detail.value });
  },

  onContentInput(e) {
    this.setData({ content: e.detail.value });
  },

  onSubmit() {
    const { title, content } = this.data;

    if (!title.trim()) {
      wx.showToast({ title: '请输入反馈标题', icon: 'none' });
      return;
    }
    if (!content.trim()) {
      wx.showToast({ title: '请输入反馈内容', icon: 'none' });
      return;
    }

    ensureLogin().then(() => {
      this.setData({ submitting: true });

      request({
        url: '/api/feedback/submit',
        method: 'POST',
        data: {
          title: title.trim(),
          content: content.trim()
        }
      }).then(() => {
        wx.showToast({
          title: '提交成功',
          icon: 'success',
          duration: 2000
        });
        setTimeout(() => {
          wx.navigateBack();
        }, 1500);
      }).catch(() => {}).finally(() => {
        this.setData({ submitting: false });
      });
    }).catch(() => {});
  }
});
