import 'package:flutter/services.dart';

import 'api.dart';
import 'format.dart';

class PayChannel {
  PayChannel(this.code, this.name, this.icon);

  final String code;
  final String name;
  final String icon;
}

const List<PayChannel> payChannels = [
  PayChannel('WECHAT', '微信支付', '💚'),
  PayChannel('ALIPAY', '支付宝', '🔷'),
  PayChannel('BALANCE', '会员卡余额', '💳'),
];

/// 订单支付。
///
/// 演示环境：后端 /orders/{id}/pay 直接记账，返回支付成功订单。
/// 生产环境：后端应先调用微信/支付宝统一下单返回 prepay 参数，再由原生
/// SDK 唤起收银台，支付回调确认后再落账（接口契约见 docs/API.md）。
class PaymentService {
  PaymentService._();

  static Future<Map<String, dynamic>> payOrder({
    required int orderId,
    required String channel,
    int? cardId,
    int? pointsUse,
    int? couponId,
  }) async {
    // 不传 payments 明细，由后端按订单应收金额自动生成支付行，防止客户端篡改金额。
    final body = <String, dynamic>{
      'payType': channel,
      if (channel == 'BALANCE' && cardId != null) 'cardId': cardId,
      if (pointsUse != null && pointsUse > 0) 'pointsUse': pointsUse,
      if (couponId != null) 'couponId': couponId,
    };
    final data = await Api.post<Map<String, dynamic>>('/api/customer/orders/$orderId/pay', body: body);
    return asMap(data);
  }

  static Future<void> invokeNativeIfNeeded(String channel) async {
    if (channel == 'WECHAT') {
      // 真实环境此处调用 WechatService.appPayment(prepay...)。
      // 演示环境后端直接记账，无需唤起 SDK。
      return;
    }
    if (channel == 'ALIPAY') {
      try {
        await const MethodChannel('com.meiye/alipay').invokeMethod('pay', {'orderInfo': ''});
      } on MissingPluginException {
        // 演示环境忽略
      }
    }
  }
}
