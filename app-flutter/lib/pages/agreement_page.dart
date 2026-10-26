import 'package:flutter/material.dart';

import '../core/theme.dart';

class AgreementPage extends StatelessWidget {
  const AgreementPage({super.key, required this.type});

  /// user / privacy
  final String type;

  bool get _isPrivacy => type == 'privacy';

  @override
  Widget build(BuildContext context) {
    final title = _isPrivacy ? '隐私政策' : '用户协议';
    return Scaffold(
      appBar: AppBar(title: Text(title)),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 8, 20, 30),
        children: [
          Text(
            '生效日期：2026 年 1 月 1 日',
            style: TextStyle(fontSize: 11, color: AppColors.muted.withValues(alpha: 0.9)),
          ),
          const SizedBox(height: 14),
          ...(_isPrivacy ? _privacySections : _userSections).map(_section),
          const SizedBox(height: 18),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppColors.brand.withValues(alpha: 0.05),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Text(
              '如对本协议有任何疑问，可通过门店客服电话或 App 内「我的-关于我们」联系运营方。本协议最终解释权归平台运营方所有。',
              style: TextStyle(fontSize: 11, color: AppColors.muted.withValues(alpha: 0.95), height: 1.6),
            ),
          ),
        ],
      ),
    );
  }

  List<(String, String)> get _userSections => const [
        (
          '一、服务说明',
          '本应用为美业大健康数字化平台提供的客户服务端，提供门店与项目浏览、在线预约、订单支付、会员卡与储值、积分优惠券、评价与售后等功能。具体服务内容以门店实际提供为准。'
        ),
        (
          '二、账号与实名',
          '您应使用本人手机号注册并妥善保管账号密码。通过微信授权登录时，平台会依据微信开放平台 UnionID 建立多端统一账号。因您主动泄露密码或验证码造成的损失由您自行承担。'
        ),
        (
          '三、预约与到店',
          '预约成功后请按时到店；如需取消或改约，请提前操作。多次无故未到店可能影响后续预约权益。服务开始前，门店有权基于健康与安全原因调整服务方案。'
        ),
        (
          '四、支付、储值与退款',
          '在线支付由微信支付、支付宝等持牌机构完成。储值卡、计次卡等预付类产品按购买页面载明的有效期与使用规则执行；退款、返工与投诉可通过售后入口发起，平台将在 48 小时内响应。'
        ),
        (
          '五、评价与内容规范',
          '您应对评价内容的真实性负责，不得发布违法、侮辱、诽谤或侵犯他人隐私的信息。平台有权对违规内容进行删除并保留追究责任的权利。'
        ),
        (
          '六、服务变更与终止',
          '平台可能根据业务需要调整功能或中止部分服务，并将提前通过站内消息或公告告知。若您不同意变更，可停止使用并申请处理未消费权益。'
        ),
        (
          '七、法律适用',
          '本协议适用中华人民共和国法律。争议双方应友好协商，协商不成的，可向平台运营方所在地有管辖权的人民法院提起诉讼。'
        ),
      ];

  List<(String, String)> get _privacySections => const [
        (
          '一、我们收集的信息',
          '为完成预约与会员服务，我们会收集您的手机号、姓名、性别、生日、会员等级、消费与预约记录。敏感信息（手机号、身份证号）在服务端加密存储，页面展示时进行脱敏处理。'
        ),
        (
          '二、信息使用目的',
          '用于账号登录与身份识别、预约排班与到店核销、订单支付与售后、会员权益与积分计算、服务提醒与回访，以及经您同意的营销活动。不会用于与上述目的无关的用途。'
        ),
        (
          '三、微信授权与 UnionID',
          '当您使用微信登录时，我们会获取微信 OpenID/UnionID 以绑定小程序、公众号与 App 账号。我们不会获取您的微信好友列表或聊天内容。'
        ),
        (
          '四、信息共享',
          '仅在为您提供服务所必需时，向支付机构、短信/消息通道、云存储服务商共享最少必要信息，并要求其履行保密义务。除法律法规要求外，不会向第三方出售您的个人信息。'
        ),
        (
          '五、存储与保护',
          '数据存储在中华人民共和国境内。我们采用租户级数据隔离、传输加密、访问控制、操作审计等措施保护数据安全，并定期进行备份与恢复演练。'
        ),
        (
          '六、您的权利',
          '您可以在「我的」中查看与修改个人资料、修改密码、管理授权；如需注销账号或导出个人信息，可通过客服提出申请，我们将在 15 个工作日内处理。'
        ),
        (
          '七、未成年人保护',
          '若您为未满 14 周岁的未成年人，应在监护人陪同下使用本服务。我们不会主动收集未成年人的敏感个人信息。'
        ),
        (
          '八、政策更新',
          '隐私政策更新后会在 App 内提示，重大变更将重新征得您的同意。您可随时在「我的-隐私政策」查看最新版本。'
        ),
      ];

  Widget _section((String, String) item) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(item.$1, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w700)),
          const SizedBox(height: 7),
          Text(item.$2, style: const TextStyle(fontSize: 12.5, height: 1.75, color: AppColors.text)),
        ],
      ),
    );
  }
}
