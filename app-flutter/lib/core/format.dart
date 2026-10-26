import 'theme.dart';

String money(dynamic value) {
  if (value == null || value == '') return '-';
  final n = value is num ? value : num.tryParse(value.toString());
  if (n == null) return value.toString();
  return '¥${n.toStringAsFixed(2)}';
}

String dt(dynamic value) {
  if (value == null) return '';
  return value.toString().replaceFirst('T', ' ').split('.').first.substring(0, 16);
}

String dateOnly(dynamic value) {
  if (value == null) return '';
  final s = value.toString();
  return s.length >= 10 ? s.substring(0, 10) : s;
}

String todayIso() {
  final d = DateTime.now();
  return '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
}

String tomorrowIso() {
  final d = DateTime.now().add(const Duration(days: 1));
  return '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
}

const Map<String, String> statusText = {
  'PENDING': '待确认',
  'CONFIRMED': '已确认',
  'ARRIVED': '已到店',
  'SERVING': '服务中',
  'DONE': '已完成',
  'NO_SHOW': '未到店',
  'CANCELLED': '已取消',
  'PAID': '已支付',
  'UNPAID': '待支付',
  'VERIFIED': '已核销',
  'REFUNDED': '已退款',
  'PARTIAL_REFUND': '部分退款',
  'UNUSED': '未使用',
  'USED': '已使用',
  'EXPIRED': '已过期',
  'APPLIED': '审核中',
  'APPROVED': '已通过',
  'REJECTED': '已驳回',
  'PENDING_HANDLE': '待处理',
  'HANDLED': '已处理',
  'PUBLISHED': '已发布',
};

const Map<String, String> bizName = {
  'BEAUTY': '美容',
  'SKIN': '皮肤管理',
  'HAIR': '美发',
  'SPA': 'SPA',
  'NAIL': '美甲美睫',
  'DENTAL': '口腔',
  'MEDBEAUTY': '轻医美',
  'HEALTH': '大健康',
  'BODY': '身体护理',
  'YOGA': '瑜伽健身',
};

String labelOf(dynamic status) {
  final key = status?.toString() ?? '';
  return statusText[key] ?? key;
}

String bizOf(dynamic bizType) {
  final key = bizType?.toString() ?? '';
  return bizName[key] ?? (key.isEmpty ? '服务' : key);
}

String channelName(dynamic channel) {
  switch (channel?.toString()) {
    case 'WECHAT':
      return '微信支付';
    case 'ALIPAY':
      return '支付宝';
    case 'BALANCE':
    case 'CARD':
      return '会员卡余额';
    case 'CASH':
      return '现金';
    case 'UNIONPAY':
      return '银联';
    case 'HUABEI':
      return '花呗';
    default:
      return channel?.toString() ?? '-';
  }
}

String initials(dynamic name) {
  final s = (name?.toString() ?? '').trim();
  if (s.isEmpty) return '美';
  return s.substring(0, 1);
}

Map<String, dynamic> asMap(dynamic value) {
  if (value is Map<String, dynamic>) return value;
  if (value is Map) return value.map((k, v) => MapEntry(k.toString(), v));
  return <String, dynamic>{};
}

List<Map<String, dynamic>> asMapList(dynamic value) {
  if (value is List) return value.map(asMap).toList();
  return <Map<String, dynamic>>[];
}

int asInt(dynamic value, [int fallback = 0]) {
  if (value is int) return value;
  if (value is num) return value.toInt();
  return int.tryParse(value?.toString() ?? '') ?? fallback;
}

double asDouble(dynamic value, [double fallback = 0]) {
  if (value is num) return value.toDouble();
  return double.tryParse(value?.toString() ?? '') ?? fallback;
}

bool isPositiveStatus(String status) {
  return const ['PAID', 'DONE', 'VERIFIED', 'CONFIRMED', 'ARRIVED', 'SERVING', 'UNUSED', 'VALID']
      .contains(status);
}

String relativeDate(String iso) {
  if (iso == todayIso()) return '今天';
  if (iso == tomorrowIso()) return '明天';
  return iso;
}

String greeting() {
  final h = DateTime.now().hour;
  if (h < 6) return '夜深了';
  if (h < 11) return '早上好';
  if (h < 14) return '中午好';
  if (h < 18) return '下午好';
  return '晚上好';
}

String maskPhone(dynamic phone) {
  final s = phone?.toString() ?? '';
  if (s.length < 7) return s;
  return s.replaceRange(3, s.length - 4, '****');
}

String cardTypeName(dynamic type) {
  switch (type?.toString()) {
    case 'TIMES':
      return '计次卡';
    case 'TIME':
      return '时间卡';
    case 'PACKAGE':
      return '套餐卡';
    case 'STORED':
      return '储值卡';
    case 'SUB':
      return '附属卡';
    default:
      return type?.toString() ?? '会员卡';
  }
}

String? colorForStatus(String status) {
  if (const ['PAID', 'DONE', 'VERIFIED', 'CONFIRMED', 'APPROVED', 'UNUSED', 'VALID'].contains(status)) {
    return 'success';
  }
  if (const ['PENDING', 'APPLIED', 'PENDING_HANDLE', 'ARRIVED'].contains(status)) {
    return 'warning';
  }
  if (const ['CANCELLED', 'REJECTED', 'EXPIRED', 'NO_SHOW', 'REFUNDED'].contains(status)) {
    return 'danger';
  }
  if (const ['SERVING', 'SEDNING'].contains(status)) {
    return 'brand';
  }
  return 'muted';
}

const Map<String, String> cardFlowType = {
  'RECHARGE': '充值',
  'CONSUME': '消费',
  'GIFT': '赠送',
  'REFUND': '退款',
  'ADJUST': '调整',
  'EXPIRE': '过期',
  'OPEN': '开卡',
};
