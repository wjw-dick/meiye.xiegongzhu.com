import 'package:flutter/services.dart';

class WechatAuthResult {
  WechatAuthResult({required this.code, this.state});

  final String code;
  final String? state;
}

/// 微信开放平台移动应用登录桥接。
///
/// 原生侧（Android/iOS）需接入微信 OpenSDK，并在 MethodChannel
/// `com.meiye/wechat` 上实现 `login` 方法，返回授权 code：
///   Android: WXAPIFactory.createWXAPI(context, appId) -> SendAuth.Req
///   iOS:     SendAuthReq -> [WXApi sendReq:]
/// 收到 onResp 后回传 {"code": "...", "state": "..."}。
///
/// 如使用社区插件，可将本类替换为 fluwx 的 `Fluwx.authByWechat`。
class WechatService {
  WechatService._();

  static const MethodChannel _channel = MethodChannel('com.meiye/wechat');

  /// 是否已接入原生 SDK（未接入时登录页会给出配置提示）。
  static Future<bool> get available async {
    try {
      final ok = await _channel.invokeMethod<bool>('isInstalled');
      return ok ?? false;
    } on MissingPluginException {
      return false;
    } on PlatformException {
      return false;
    }
  }

  static Future<WechatAuthResult> authorize() async {
    try {
      final result = await _channel.invokeMethod<Map<dynamic, dynamic>>('login');
      final code = result?['code']?.toString();
      if (code == null || code.isEmpty) {
        throw PlatformException(code: 'cancelled', message: '已取消微信登录');
      }
      return WechatAuthResult(code: code, state: result?['state']?.toString());
    } on MissingPluginException {
      throw PlatformException(
        code: 'not_configured',
        message: '未接入微信开放平台移动应用 SDK，请先在原生工程配置 AppID 与 Universal Link',
      );
    }
  }

  static Future<Map<String, dynamic>> appPayment({
    required String prepayId,
    required String partnerId,
    required String nonceStr,
    required String timeStamp,
    required String sign,
  }) async {
    try {
      final result = await _channel.invokeMethod<Map<dynamic, dynamic>>('pay', {
        'prepayId': prepayId,
        'partnerId': partnerId,
        'nonceStr': nonceStr,
        'timeStamp': timeStamp,
        'sign': sign,
      });
      return Map<String, dynamic>.from(result ?? {});
    } on MissingPluginException {
      throw PlatformException(
        code: 'not_configured',
        message: '未接入微信支付 SDK，当前为演示支付通道',
      );
    }
  }
}
