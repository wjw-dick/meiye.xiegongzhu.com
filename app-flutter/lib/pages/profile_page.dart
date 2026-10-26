import 'package:flutter/material.dart';

import '../core/api.dart';
import '../core/auth.dart';
import '../core/format.dart';
import '../core/theme.dart';
import '../widgets/common.dart';
import 'after_sales_page.dart';
import 'agreement_page.dart';
import 'appointments_page.dart';
import 'cards_page.dart';
import 'coupons_page.dart';
import 'messages_page.dart';
import 'password_page.dart';
import 'points_page.dart';
import 'reviews_page.dart';

class ProfilePage extends StatefulWidget {
  const ProfilePage({super.key});

  @override
  State<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends State<ProfilePage> {
  Map<String, dynamic> _customer = {};
  Map<String, dynamic> _data = {};
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final data = asMap(await Api.get<Map<String, dynamic>>('/api/customer/profile'));
      if (!mounted) return;
      setState(() {
        _data = data;
        _customer = asMap(data['customer']);
      });
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _editProfile() async {
    final name = TextEditingController(text: _customer['name']?.toString() ?? '');
    final remark = TextEditingController(text: _customer['remark']?.toString() ?? '');
    String gender = _customer['gender']?.toString() ?? 'FEMALE';
    String? birthday = _customer['birthday']?.toString();
    await showModalBottomSheet<void>(
      context: context,
      backgroundColor: Colors.white,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(22))),
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setSheet) => SafeArea(
          child: Padding(
            padding: EdgeInsets.fromLTRB(20, 18, 20, 20 + MediaQuery.viewInsetsOf(ctx).bottom),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('编辑资料', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w700)),
                const SizedBox(height: 12),
                TextField(controller: name, decoration: const InputDecoration(labelText: '姓名')),
                const SizedBox(height: 10),
                SegmentedButton<String>(
                  segments: const [
                    ButtonSegment(value: 'FEMALE', label: Text('女')),
                    ButtonSegment(value: 'MALE', label: Text('男')),
                    ButtonSegment(value: 'OTHER', label: Text('保密')),
                  ],
                  selected: {gender},
                  onSelectionChanged: (v) => setSheet(() => gender = v.first),
                  style: ButtonStyle(textStyle: WidgetStateProperty.all(const TextStyle(fontSize: 12))),
                ),
                const SizedBox(height: 10),
                InkWell(
                  onTap: () async {
                    final picked = await showDatePicker(
                      context: ctx,
                      initialDate: birthday == null ? DateTime(1995, 1, 1) : (DateTime.tryParse(birthday!) ?? DateTime(1995, 1, 1)),
                      firstDate: DateTime(1940),
                      lastDate: DateTime.now(),
                    );
                    if (picked != null) {
                      setSheet(() => birthday =
                          '${picked.year.toString().padLeft(4, '0')}-${picked.month.toString().padLeft(2, '0')}-${picked.day.toString().padLeft(2, '0')}');
                    }
                  },
                  child: InputDecorator(
                    decoration: const InputDecoration(labelText: '生日'),
                    child: Text(birthday ?? '请选择', style: const TextStyle(fontSize: 14)),
                  ),
                ),
                const SizedBox(height: 10),
                TextField(controller: remark, maxLines: 2, decoration: const InputDecoration(labelText: '个人备注')),
                const SizedBox(height: 14),
                FilledButton(
                  onPressed: () async {
                    if (name.text.trim().isEmpty) {
                      showToast(ctx, '姓名不能为空');
                      return;
                    }
                    try {
                      await Api.put<Map<String, dynamic>>('/api/customer/profile', body: {
                        'name': name.text.trim(),
                        'gender': gender,
                        if (birthday != null) 'birthday': birthday,
                        'remark': remark.text.trim(),
                      });
                      if (!ctx.mounted) return;
                      Navigator.pop(ctx);
                      if (mounted) {
                        showToast(context, '资料已更新');
                        await _load();
                      }
                    } on ApiException catch (e) {
                      if (ctx.mounted) showToast(ctx, e.message);
                    }
                  },
                  child: const Text('保存'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
    name.dispose();
    remark.dispose();
  }

  Future<void> _logout() async {
    final ok = await confirmDialog(
      context,
      title: '退出登录',
      content: '确定退出当前账号吗？',
      confirmText: '退出',
      danger: true,
    );
    if (!ok) return;
    try {
      await Api.post<dynamic>('/api/auth/logout');
    } catch (_) {}
    await AuthStore.instance.logout();
  }

  @override
  Widget build(BuildContext context) {
    final cards = asMapList(_data['cards']);
    final user = AuthStore.instance.user;
    return Scaffold(
      body: RefreshIndicator(
        color: AppColors.brand,
        onRefresh: _load,
        child: ListView(
          padding: EdgeInsets.zero,
          children: [
            Container(
              padding: const EdgeInsets.fromLTRB(20, 60, 20, 28),
              decoration: const BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                  colors: [Color(0xFF8C1A45), Color(0xFFB4235A), Color(0xFFCE5884)],
                ),
                borderRadius: BorderRadius.only(
                  bottomLeft: Radius.circular(28),
                  bottomRight: Radius.circular(28),
                ),
              ),
              child: Column(
                children: [
                  Row(
                    children: [
                      BrandAvatar(text: initials(_customer['name'] ?? user?.name), size: 58),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Flexible(
                                  child: Text(
                                    _customer['name']?.toString() ?? user?.name ?? '会员',
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: const TextStyle(color: Colors.white, fontSize: 19, fontWeight: FontWeight.w600),
                                  ),
                                ),
                                const SizedBox(width: 8),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
                                  decoration: BoxDecoration(
                                    color: const Color(0xFFF3D9A4).withValues(alpha: 0.2),
                                    borderRadius: BorderRadius.circular(4),
                                  ),
                                  child: Text(
                                    _customer['level']?.toString() ?? '普通会员',
                                    style: const TextStyle(fontSize: 9.5, color: Color(0xFFF3D9A4)),
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 4),
                            Text(
                              maskPhone(_customer['phone'] ?? user?.username),
                              style: TextStyle(fontSize: 11.5, color: Colors.white.withValues(alpha: 0.75)),
                            ),
                            const SizedBox(height: 2),
                            Text(
                              '会员编号 ${_customer['customerNo'] ?? '-'}',
                              style: TextStyle(fontSize: 10, color: Colors.white.withValues(alpha: 0.55)),
                            ),
                          ],
                        ),
                      ),
                      IconButton(
                        onPressed: _editProfile,
                        icon: const Icon(Icons.edit_outlined, color: Colors.white, size: 20),
                        tooltip: '编辑资料',
                      ),
                    ],
                  ),
                  const SizedBox(height: 20),
                  Row(
                    children: [
                      _stat('${_customer['points'] ?? 0}', '积分'),
                      _line(),
                      _stat('${_customer['growth'] ?? 0}', '成长值'),
                      _line(),
                      _stat('${cards.length}', '会员卡'),
                      _line(),
                      _stat(money(_customer['totalConsume'] ?? 0), '累计消费'),
                    ],
                  ),
                ],
              ),
            ),
            if (_loading) const LoadingView(),
            if (_error != null) ErrorState(message: _error!, onRetry: _load),
            if (!_loading) ...[
              const SizedBox(height: 14),
              _menuGroup([
                _MenuItem('我的预约', '📅', () => Navigator.push(context, MaterialPageRoute(builder: (_) => const AppointmentsPage()))),
                _MenuItem('会员卡包', '💳', () => Navigator.push(context, MaterialPageRoute(builder: (_) => const CardsPage()))),
                _MenuItem('优惠券', '◈', () => Navigator.push(context, MaterialPageRoute(builder: (_) => const CouponsPage()))),
                _MenuItem('积分明细', '✦', () => Navigator.push(context, MaterialPageRoute(builder: (_) => const PointsPage()))),
              ]),
              _menuGroup([
                _MenuItem('消息中心', '✉️', () => Navigator.push(context, MaterialPageRoute(builder: (_) => const MessagesPage()))),
                _MenuItem('服务评价', '★', () => Navigator.push(context, MaterialPageRoute(builder: (_) => const ReviewsPage()))),
                _MenuItem('售后退款', '↺', () => Navigator.push(context, MaterialPageRoute(builder: (_) => const AfterSalesPage()))),
              ]),
              _menuGroup([
                _MenuItem('修改密码', '🔒', () => Navigator.push(context, MaterialPageRoute(builder: (_) => const PasswordPage()))),
                _MenuItem('用户协议', '📄', () => Navigator.push(context, MaterialPageRoute(builder: (_) => const AgreementPage(type: 'user')))),
                _MenuItem('隐私政策', '🛡️', () => Navigator.push(context, MaterialPageRoute(builder: (_) => const AgreementPage(type: 'privacy')))),
                _MenuItem('关于我们', 'ℹ️', () => _about()),
              ]),
              _menuGroup([
                _MenuItem('退出登录', '⎋', _logout, danger: true),
              ]),
              Padding(
                padding: const EdgeInsets.only(bottom: 26, top: 4),
                child: Center(
                  child: Text(
                    '美业臻选 v1.0.0 · 租户 ${AuthStore.instance.tenantCode}',
                    style: TextStyle(fontSize: 10, color: AppColors.muted.withValues(alpha: 0.75)),
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _stat(String value, String label) {
    return Expanded(
      child: Column(
        children: [
          Text(value, maxLines: 1, style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.w700)),
          const SizedBox(height: 2),
          Text(label, style: TextStyle(fontSize: 10, color: Colors.white.withValues(alpha: 0.68))),
        ],
      ),
    );
  }

  Widget _line() => Container(width: 1, height: 22, color: Colors.white.withValues(alpha: 0.16));

  Widget _menuGroup(List<_MenuItem> items) {
    return Container(
      margin: const EdgeInsets.fromLTRB(16, 0, 16, 12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.line.withValues(alpha: 0.7)),
      ),
      child: Column(
        children: List.generate(items.length, (i) {
          final item = items[i];
          return Column(
            children: [
              ListTile(
                dense: true,
                leading: SizedBox(
                  width: 30,
                  child: Text(item.icon, style: const TextStyle(fontSize: 17)),
                ),
                title: Text(
                  item.label,
                  style: TextStyle(fontSize: 13.5, color: item.danger ? AppColors.danger : AppColors.text),
                ),
                trailing: item.danger
                    ? null
                    : const Icon(Icons.chevron_right, size: 18, color: AppColors.muted),
                onTap: item.onTap,
              ),
              if (i != items.length - 1)
                const Padding(
                  padding: EdgeInsets.only(left: 48),
                  child: Divider(height: 1),
                ),
            ],
          );
        }),
      ),
    );
  }

  void _about() {
    showAboutDialog(
      context: context,
      applicationName: '美业臻选',
      applicationVersion: '1.0.0',
      applicationLegalese: '美业大健康数字化平台 · 客户 App\n租户 ${AuthStore.instance.tenantCode}',
      children: [
        const SizedBox(height: 10),
        const Text('本应用由平台统一提供，支持多租户、品类插件化与会员跨端互通。', style: TextStyle(fontSize: 12, height: 1.5)),
      ],
    );
  }
}

class _MenuItem {
  _MenuItem(this.label, this.icon, this.onTap, {this.danger = false});

  final String label;
  final String icon;
  final VoidCallback onTap;
  final bool danger;
}
