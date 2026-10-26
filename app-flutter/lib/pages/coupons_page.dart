import 'package:flutter/material.dart';

import '../core/api.dart';
import '../core/format.dart';
import '../core/theme.dart';
import '../widgets/common.dart';

class CouponsPage extends StatefulWidget {
  const CouponsPage({super.key});

  @override
  State<CouponsPage> createState() => _CouponsPageState();
}

class _CouponsPageState extends State<CouponsPage> {
  String _status = 'UNUSED';
  List<Map<String, dynamic>> _list = [];
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
      final list = asMapList(await Api.get<List<dynamic>>('/api/customer/coupons', query: {'status': _status}));
      if (mounted) setState(() => _list = list);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('优惠券')),
      body: Column(
        children: [
          Container(
            padding: const EdgeInsets.fromLTRB(16, 6, 16, 8),
            color: AppColors.bg,
            child: Row(
              children: [
                _tab('UNUSED', '可使用'),
                const SizedBox(width: 10),
                _tab('USED', '已使用'),
                const SizedBox(width: 10),
                _tab('EXPIRED', '已过期'),
              ],
            ),
          ),
          Expanded(
            child: _loading
                ? const LoadingView()
                : _error != null
                    ? ErrorState(message: _error!, onRetry: _load)
                    : RefreshIndicator(
                        color: AppColors.brand,
                        onRefresh: _load,
                        child: _list.isEmpty
                            ? ListView(
                                children: const [
                                  SizedBox(height: 70),
                                  EmptyState(icon: '🎫', text: '暂无优惠券\n关注门店活动，好礼不错过'),
                                ],
                              )
                            : ListView.separated(
                                padding: const EdgeInsets.all(16),
                                itemCount: _list.length,
                                separatorBuilder: (_, __) => const SizedBox(height: 10),
                                itemBuilder: (context, i) => _coupon(_list[i]),
                              ),
                      ),
          ),
        ],
      ),
    );
  }

  Widget _tab(String key, String label) {
    final selected = _status == key;
    return Expanded(
      child: GestureDetector(
        onTap: () {
          setState(() => _status = key);
          _load();
        },
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 8),
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: selected ? AppColors.brand : Colors.white,
            borderRadius: BorderRadius.circular(9),
          ),
          child: Text(
            label,
            style: TextStyle(fontSize: 12, color: selected ? Colors.white : AppColors.muted, fontWeight: selected ? FontWeight.w600 : FontWeight.w400),
          ),
        ),
      ),
    );
  }

  Widget _coupon(Map<String, dynamic> c) {
    final isCash = c['type'] == 'CASH';
    final available = c['status'] == 'UNUSED';
    final expired = c['status'] == 'EXPIRED';
    return Opacity(
      opacity: available ? 1 : 0.62,
      child: Container(
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: AppColors.line),
        ),
        child: Row(
          children: [
            Container(
              width: 108,
              padding: const EdgeInsets.symmetric(vertical: 20),
              decoration: BoxDecoration(
                color: AppColors.brand.withValues(alpha: available ? 0.07 : 0.03),
                borderRadius: const BorderRadius.only(topLeft: Radius.circular(14), bottomLeft: Radius.circular(14)),
              ),
              child: Column(
                children: [
                  if (isCash)
                    Text(
                      money(c['amount']),
                      style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w700, color: AppColors.brand),
                    )
                  else
                    Text(
                      '${c['discount'] ?? ''} 折',
                      style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w700, color: AppColors.brand),
                    ),
                  const SizedBox(height: 4),
                  Text(
                    '满 ${money(c['minAmount'])} 可用',
                    style: const TextStyle(fontSize: 9.5, color: AppColors.muted),
                  ),
                ],
              ),
            ),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(14, 12, 12, 12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(c['name']?.toString() ?? '优惠券', style: const TextStyle(fontSize: 13.5, fontWeight: FontWeight.w600)),
                    const SizedBox(height: 4),
                    Text(
                      '有效期至 ${dateOnly(c['expireAt'])}',
                      style: const TextStyle(fontSize: 10.5, color: AppColors.muted),
                    ),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        Pill(
                          text: expired ? '已过期' : (available ? '可使用' : '已使用'),
                          color: available ? AppColors.success : AppColors.muted,
                        ),
                        const Spacer(),
                        if (available)
                          const Text('下单可用', style: TextStyle(fontSize: 10.5, color: AppColors.brand)),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
