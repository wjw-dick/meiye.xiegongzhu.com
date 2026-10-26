import 'package:flutter_test/flutter_test.dart';
import 'package:meiye_app/core/format.dart';

void main() {
  test('金额格式化', () {
    expect(money(199), '¥199.00');
    expect(money('88.5'), '¥88.50');
    expect(money(null), '-');
  });

  test('状态与品类中文映射', () {
    expect(labelOf('PAID'), '已支付');
    expect(bizOf('SKIN'), '皮肤管理');
    expect(channelName('BALANCE'), '会员卡余额');
  });

  test('手机号脱敏', () {
    expect(maskPhone('13700000001'), '137****0001');
  });

  test('日期截取', () {
    expect(dateOnly('2026-10-26T10:00:00'), '2026-10-26');
    expect(dt('2026-10-26T10:00:00'), '2026-10-26 10:00');
  });
}
