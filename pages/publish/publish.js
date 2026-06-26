const {
  request,
  uploadFile
} = require('../../utils/request');
const {
  ensureLogin
} = require('../../utils/auth');
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
    isEdit: false,
    editId: null,
    submitting: false,
    importing: false,
    showImportResult: false,
    importResult: {
      successCount: 0,
      failCount: 0,
      failDetails: []
    }
  },

  onLoad(options) {
    this._checkEditData();
  },

  onShow() {
    this._checkEditData();
  },

  _checkEditData() {
    const editData = app.globalData.editData;
    if (editData) {
      this.setData({
        isEdit: true,
        editId: editData.id,
        form: {
          title: editData.title || '',
          content: editData.content || '',
          category: editData.category || '',
          sourceUrl: editData.sourceUrl || '',
          claimSteps: editData.claimSteps || ''
        }
      });
      // 用完清除
      app.globalData.editData = null;
      wx.setNavigationBarTitle({
        title: '编辑信息'
      });
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

  // ========== 批量导入 ==========
  onBatchImport() {
    ensureLogin().then(() => {
      wx.chooseMessageFile({
        count: 1,
        type: 'file',
        extension: ['xlsx'],
        success: (res) => {
          const filePath = res.tempFiles[0].path;
          const fileName = res.tempFiles[0].name;
          wx.showModal({
            title: '确认导入',
            content: '即将导入文件：' + fileName,
            confirmText: '开始导入',
            confirmColor: '#FF6B35',
            success: (modalRes) => {
              if (modalRes.confirm) {
                this.doImport(filePath);
              }
            }
          });
        }
      });
    }).catch(() => {});
  },

  doImport(filePath) {
    this.setData({
      importing: true
    });
    wx.showLoading({
      title: '导入中...',
      mask: true
    });

    uploadFile({
      url: '/api/wool/import',
      filePath: filePath,
      name: 'file'
    }).then(res => {
      wx.hideLoading();
      this.setData({
        showImportResult: true,
        importResult: res.data || {
          successCount: 0,
          failCount: 0,
          failDetails: []
        }
      });
    }).catch(err => {
      wx.hideLoading();
      wx.showToast({
        title: err.msg || '导入失败',
        icon: 'none'
      });
    }).finally(() => {
      this.setData({
        importing: false
      });
    });
  },

  closeImportResult() {
    this.setData({
      showImportResult: false
    });
  },

  // ========== 提交表单 ==========
  onSubmit() {
    const {
      form,
      isEdit,
      editId
    } = this.data;

    if (!form.title.trim()) {
      wx.showToast({
        title: '请输入标题',
        icon: 'none'
      });
      return;
    }
    if (form.title.length > 128) {
      wx.showToast({
        title: '标题不能超过128字',
        icon: 'none'
      });
      return;
    }
    if (!form.content.trim()) {
      wx.showToast({
        title: '请输入内容',
        icon: 'none'
      });
      return;
    }

    ensureLogin().then(() => {
      this.setData({
        submitting: true
      });

      const data = {
        title: form.title.trim(),
        content: form.content.trim(),
        category: form.category,
        sourceUrl: form.sourceUrl.trim(),
        claimSteps: form.claimSteps.trim()
      };

      const apiRequest = isEdit ?
        request({
          url: '/api/wool/update/' + editId,
          method: 'PUT',
          data
        }) :
        request({
          url: '/api/wool/publish',
          method: 'POST',
          data
        });

      apiRequest.then(() => {
        wx.showToast({
          title: isEdit ? '修改成功' : '发布成功',
          icon: 'success',
          duration: 2000
        });
        // 标记首页需要刷新，然后跳转
        app.globalData.needRefreshList = true;
        setTimeout(() => {
          this.setData({
            isEdit: false,
            editId: null,
            form: {
              title: '',
              content: '',
              category: '',
              sourceUrl: '',
              claimSteps: ''
            }
          });
          wx.switchTab({
            url: '/pages/index/index'
          });
        }, 1500);
      }).catch(() => {}).finally(() => {
        this.setData({
          submitting: false
        });
      });
    }).catch(() => {});
  }
});
