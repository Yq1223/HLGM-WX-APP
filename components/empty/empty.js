Component({
  properties: {
    emoji: { type: String, value: '📭' },
    title: { type: String, value: '暂无数据' },
    desc: { type: String, value: '' },
    btnText: { type: String, value: '' }
  },
  methods: {
    onBtnTap() {
      this.triggerEvent('btntap');
    }
  }
});
