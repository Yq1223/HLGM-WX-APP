/**
 * 广告位占位组件
 * 
 * 使用方式：<ad-banner position="index_top" />
 * 
 * 接入广告时：将 showAd 改为 true，替换内部为真实广告组件即可。
 * position 预留了位置标识，方便后续按位置加载不同广告。
 */
Component({
  properties: {
    // 广告位置标识
    position: {
      type: String,
      value: 'default'
    }
  },
  data: {
    // 是否显示广告 — 当前为 false（占位不展示）
    // 接入广告SDK后改为 true
    showAd: false,
    adText: ''
  },
  lifetimes: {
    attached() {
      // 根据位置设置占位文案（开发调试用，showAd=true 时才显示）
      const textMap = {
        'index_top': '首页横幅广告位',
        'index_between': '信息流广告位',
        'detail_bottom': '详情页广告位',
        'mine_banner': '个人中心广告位',
        'points_banner': '积分页广告位',
        'default': '广告位'
      };
      this.setData({
        adText: textMap[this.data.position] || textMap['default']
      });
    }
  }
});
