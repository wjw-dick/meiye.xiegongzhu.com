import 'package:flutter/material.dart';

import '../core/api.dart';
import '../core/auth.dart';
import '../core/config.dart';
import '../core/format.dart';
import '../core/theme.dart';
import '../core/wechat.dart';
import '../widgets/common.dart';
import 'agreement_page.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _tenant = TextEditingController(text: AppConfig.defaultTenant);
  late final TextEditingController _phone = TextEditingController(text: '13700000001');
  late final TextEditingController _password = TextEditingController(text: '123456');
  bool _obscure = true;
  bool _agreed = true;
  bool _loading = false;
  String _mode = 'account';

  @override
  void dispose() {
    _tenant.dispose();
    _phone.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _login() async {
    if (!_agreed) {
      showToast(context, '请先阅读并同意用户协议与隐私政策');
      return;
    }
    if (!_formKey.currentState!.validate()) return;
    FocusScope.of(context).unfocus();
    setState(() => _loading = true);
    try {
      final data = await Api.post<Map<String, dynamic>>('/api/auth/login', body: {
        'username': _phone.text.trim(),
        'password': _password.text,
        'tenantCode': _tenant.text.trim().toUpperCase(),
        'userType': 'CUSTOMER',
      });
      await _saveLogin(data);
      if (!mounted) return;
      showToast(context, '欢迎回来');
    } on ApiException catch (e) {
      if (mounted) showToast(context, e.message);
    } catch (e) {
      if (mounted) showToast(context, '登录失败：$e');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _wechatLogin() async {
    if (!_agreed) {
      showToast(context, '请先阅读并同意用户协议与隐私政策');
      return;
    }
    setState(() => _loading = true);
    try {
      final installed = await WechatService.available;
      if (!installed) {
        if (!mounted) return;
        await showDialog<void>(
          context: context,
          builder: (ctx) => AlertDialog(
            title: const Text('微信登录未启用'),
            content: const Text(
              '当前工程尚未接入微信开放平台移动应用 SDK。\n\n'
              '上线前请在 Android/iOS 原生工程中配置 AppID、Universal Link，'
              '并实现 MethodChannel「com.meiye/wechat」的 login 方法；'
              '同时由运营方在开放平台绑定小程序与公众号，确保 UnionID 互通。\n\n'
              '可先使用手机号 + 密码登录体验完整业务。',
            ),
            actions: [
              TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('知道了')),
            ],
          ),
        );
        return;
      }
      final auth = await WechatService.authorize();
      final data = await Api.post<Map<String, dynamic>>('/api/auth/wechat/APP', body: {
        'tenantCode': _tenant.text.trim().toUpperCase(),
        'code': auth.code,
        'nickname': '微信用户',
      });
      await _saveLogin(data);
    } on ApiException catch (e) {
      if (mounted) showToast(context, e.message);
    } catch (e) {
      final text = e.toString().replaceFirst('PlatformException(', '').split(',').first;
      if (mounted) showToast(context, text.replaceAll(')', ''));
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _saveLogin(Map<String, dynamic> data) async {
    final token = data['token']?.toString() ?? '';
    final user = AuthUser.fromJson(asMap(data['user']));
    final tenant = asMap(data['tenant']);
    final code = tenant['code']?.toString() ?? _tenant.text.trim().toUpperCase();
    await AuthStore.instance.save(token: token, tenantCode: code, user: user);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SingleChildScrollView(
        child: Column(
          children: [
            Container(
              width: double.infinity,
              height: 250,
              padding: const EdgeInsets.fromLTRB(26, 70, 26, 30),
              decoration: const BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                  colors: [Color(0xFF8C1A45), Color(0xFFB4235A), Color(0xFFD46A92)],
                ),
                borderRadius: BorderRadius.only(
                  bottomLeft: Radius.circular(34),
                  bottomRight: Radius.circular(34),
                ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Container(
                        width: 46,
                        height: 46,
                        alignment: Alignment.center,
                        decoration: BoxDecoration(
                          color: Colors.white.withValues(alpha: 0.16),
                          borderRadius: BorderRadius.circular(14),
                        ),
                        child: const Text('美', style: TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.w700)),
                      ),
                      const SizedBox(width: 12),
                      const Text('美业臻选', style: TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.w600, letterSpacing: 2)),
                    ],
                  ),
                  const Spacer(),
                  const Text('让每一次护理\n都值得期待', style: TextStyle(color: Colors.white, fontSize: 26, height: 1.35, fontWeight: FontWeight.w600)),
                  const SizedBox(height: 8),
                  Text('预约 · 会员卡 · 储值 · 商城', style: TextStyle(color: Colors.white.withValues(alpha: 0.78), fontSize: 12, letterSpacing: 1)),
                ],
              ),
            ),
            Transform.translate(
              offset: const Offset(0, -22),
              child: Container(
                margin: const EdgeInsets.symmetric(horizontal: 20),
                padding: const EdgeInsets.fromLTRB(18, 16, 18, 20),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(20),
                  boxShadow: [
                    BoxShadow(
                      color: AppColors.brand.withValues(alpha: 0.08),
                      blurRadius: 24,
                      offset: const Offset(0, 10),
                    )
                  ],
                ),
                child: Form(
                  key: _formKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          _modeTab('账号登录', 'account'),
                          const SizedBox(width: 20),
                          _modeTab('微信登录', 'wechat'),
                        ],
                      ),
                      const SizedBox(height: 6),
                      if (_mode == 'account') ...[
                        const FieldLabel(text: '品牌/租户编码'),
                        TextFormField(
                          controller: _tenant,
                          textCapitalization: TextCapitalization.characters,
                          decoration: const InputDecoration(hintText: '例如 5MY', prefixIcon: Icon(Icons.storefront_outlined, size: 19)),
                          validator: (v) => (v == null || v.trim().isEmpty) ? '请输入租户编码' : null,
                        ),
                        const FieldLabel(text: '手机号'),
                        TextFormField(
                          controller: _phone,
                          keyboardType: TextInputType.phone,
                          decoration: const InputDecoration(hintText: '请输入手机号', prefixIcon: Icon(Icons.person_outline, size: 19)),
                          validator: (v) => (v == null || v.trim().length < 6) ? '请输入正确的手机号' : null,
                        ),
                        const FieldLabel(text: '密码'),
                        TextFormField(
                          controller: _password,
                          obscureText: _obscure,
                          decoration: InputDecoration(
                            hintText: '请输入密码',
                            prefixIcon: const Icon(Icons.lock_outline, size: 19),
                            suffixIcon: IconButton(
                              onPressed: () => setState(() => _obscure = !_obscure),
                              icon: Icon(_obscure ? Icons.visibility_off_outlined : Icons.visibility_outlined, size: 19),
                            ),
                          ),
                          validator: (v) => (v == null || v.length < 6) ? '密码至少 6 位' : null,
                          onFieldSubmitted: (_) => _login(),
                        ),
                        const SizedBox(height: 18),
                        FilledButton(
                          onPressed: _loading ? null : _login,
                          child: _loading
                              ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                              : const Text('登录'),
                        ),
                        const SizedBox(height: 12),
                        Center(
                          child: Text(
                            '演示账号：13700000001 / 123456',
                            style: TextStyle(fontSize: 11, color: AppColors.muted.withValues(alpha: 0.9)),
                          ),
                        ),
                      ] else ...[
                        const SizedBox(height: 10),
                        const Text(
                          '使用微信开放平台移动应用授权登录，与小程序、公众号共享 UnionID，会员权益自动同步。',
                          style: TextStyle(fontSize: 13, color: AppColors.muted, height: 1.6),
                        ),
                        const SizedBox(height: 22),
                        FilledButton.icon(
                          onPressed: _loading ? null : _wechatLogin,
                          icon: const Icon(Icons.chat_bubble_outline, size: 18),
                          label: const Text('微信一键登录'),
                          style: FilledButton.styleFrom(backgroundColor: const Color(0xFF2AAE67)),
                        ),
                        const SizedBox(height: 12),
                        Center(
                          child: Text(
                            '需在原生工程接入微信 OpenSDK',
                            style: TextStyle(fontSize: 11, color: AppColors.muted.withValues(alpha: 0.9)),
                          ),
                        ),
                      ],
                      const SizedBox(height: 16),
                      Row(
                        children: [
                          SizedBox(
                            width: 24,
                            height: 24,
                            child: Checkbox(
                              value: _agreed,
                              activeColor: AppColors.brand,
                              materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                              onChanged: (v) => setState(() => _agreed = v ?? false),
                            ),
                          ),
                          const SizedBox(width: 4),
                          const Text('我已阅读并同意', style: TextStyle(fontSize: 11.5, color: AppColors.muted)),
                          GestureDetector(
                            onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const AgreementPage(type: 'user'))),
                            child: const Text('《用户协议》', style: TextStyle(fontSize: 11.5, color: AppColors.brand)),
                          ),
                          GestureDetector(
                            onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const AgreementPage(type: 'privacy'))),
                            child: const Text('《隐私政策》', style: TextStyle(fontSize: 11.5, color: AppColors.brand)),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ),
            const SizedBox(height: 4),
            Text('v${AppConfig.version} · 美业大健康数字化平台', style: const TextStyle(fontSize: 10.5, color: AppColors.muted)),
            const SizedBox(height: 24),
          ],
        ),
      ),
    );
  }

  Widget _modeTab(String text, String mode) {
    final active = _mode == mode;
    return GestureDetector(
      onTap: () => setState(() => _mode = mode),
      child: Column(
        children: [
          Text(
            text,
            style: TextStyle(
              fontSize: 15,
              fontWeight: active ? FontWeight.w700 : FontWeight.w400,
              color: active ? AppColors.text : AppColors.muted,
            ),
          ),
          const SizedBox(height: 5),
          Container(
            width: 22,
            height: 3,
            decoration: BoxDecoration(
              color: active ? AppColors.brand : Colors.transparent,
              borderRadius: BorderRadius.circular(2),
            ),
          ),
        ],
      ),
    );
  }
}
