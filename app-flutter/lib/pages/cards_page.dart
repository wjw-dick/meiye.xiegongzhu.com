import 'package:flutter/material.dart';

import '../core/api.dart';
import '../core/format.dart';
import '../core/theme.dart';
import '../widgets/common.dart';
import 'items_page.dart';

class CardsPage extends StatefulWidget {
  const CardsPage({super.key});

  @override
  State<CardsPage> createState() => _CardsPageState();
}

class _CardsPageState extends State<CardsPage> {
  List<Map<String, dynamic>> _cards = [];
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
      final list = asMapList(await Api.get<List<dynamic>>('/api/customer/cards'));
      if (mounted) setState(() => _cards = list);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _showFlows(Map<String, dynamic> card) async {
    try {
      final list = asMapList(await Api.get<List<dynamic>>('/api/customer/cards/${card['id']}/flows'));
      if (!mounted) return;
      await showModalBottomSheet<void>(
        context: context,
        backgroundColor: Colors.white,
        isScrollControlled: true,
        shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(22))),
        builder: (ctx) => DraggableScrollableSheet(
          expand: false,
          initialChildSize: 0.62,
          maxChildSize: 0.9,
          builder: (ctx, controller) => Column(
            children: [
              const SizedBox(height: 10),
              Container(
                width: 36,
                height: 4,
                decoration: BoxDecoration(color: AppColors.line, borderRadius: BorderRadius.circular(2)),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 14, 20, 8),
                child: Row(
                  children: [
                    Text(card['name']?.toString() ?? '会员卡', style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700)),
                    const Spacer(),
                    Text('余额 ${money(card['balance'])}', style: const TextStyle(fontSize: 11.5, color: AppColors.muted)),
                  ],
                ),
              ),
              const Divider(),
              Expanded(
                child: list.isEmpty
                    ? const EmptyState(text: '暂无流水记录', icon: '📋')
                    : ListView.separated(
                        controller: controller,
                        padding: const EdgeInsets.all(18),
                        itemCount: list.length,
                        separatorBuilder: (_, __) => const Divider(height: 18),
                        itemBuilder: (context, i) {
                          final f = list[i];
                          return Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      cardFlowType[f['type']?.toString()] ?? f['type']?.toString() ?? '变动',
                                      style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500),
                                    ),
                                    const SizedBox(height: 3),
                                    Text(
                                      '${dt(f['createdAt'])}  ${f['remark'] ?? ''}',
                                      style: const TextStyle(fontSize: 10.5, color: AppColors.muted),
                                    ),
                                  ],
                                ),
                              ),
                              Column(
                                crossAxisAlignment: CrossAxisAlignment.end,
                                children: [
                                  Text(
                                    '${asDouble(f['amount']) >= 0 ? '+' : ''}${money(f['amount'])}',
                                    style: TextStyle(
                                      fontSize: 13,
                                      fontWeight: FontWeight.w600,
                                      color: asDouble(f['amount']) >= 0 ? AppColors.success : AppColors.text,
                                    ),
                                  ),
                                  if (asInt(f['times']) != 0)
                                    Text(
                                      '${asInt(f['times']) > 0 ? '+' : ''}${f['times']} 次',
                                      style: const TextStyle(fontSize: 10.5, color: AppColors.muted),
                                    ),
                                  Text('余额 ${money(f['balanceAfter'])}', style: const TextStyle(fontSize: 10, color: AppColors.muted)),
                                ],
                              ),
                            ],
                          );
                        },
                      ),
              ),
            ],
          ),
        ),
      );
    } on ApiException catch (e) {
      if (mounted) showToast(context, e.message);
    }
  }

  Future<void> _recharge(Map<String, dynamic> card) async {
    final amountCtrl = TextEditingController(text: '500');
    await showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('会员卡充值'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('${card['name'] ?? '会员卡'} · 当前余额 ${money(card['balance'])}', style: const TextStyle(fontSize: 12, color: AppColors.muted)),
            const SizedBox(height: 12),
            TextField(
              controller: amountCtrl,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(labelText: '充值金额（元）', prefixText: '¥ '),
            ),
            const SizedBox(height: 8),
            const Text('演示环境充值即时到账；生产环境需接入微信/支付宝支付后回调入账。', style: TextStyle(fontSize: 10.5, color: AppColors.muted, height: 1.4)),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('取消')),
          TextButton(
            onPressed: () async {
              final amount = double.tryParse(amountCtrl.text.trim());
              if (amount == null || amount <= 0) {
                showToast(ctx, '请输入正确金额');
                return;
              }
              try {
                await Api.post<Map<String, dynamic>>(
                  '/api/customer/cards/${card['id']}/recharge',
                  query: {'amount': amount, 'bonus': 0},
                );
                if (!ctx.mounted) return;
                Navigator.pop(ctx);
                if (mounted) {
                  showToast(context, '充值成功');
                  await _load();
                }
              } on ApiException catch (e) {
                if (ctx.mounted) showToast(ctx, e.message);
              }
            },
            child: const Text('确认充值'),
          ),
        ],
      ),
    );
    amountCtrl.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('我的卡包'),
        actions: [
          TextButton(
            onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const ItemsPage(initialTab: 1))),
            child: const Text('购卡', style: TextStyle(fontSize: 12)),
          )
        ],
      ),
      body: _loading
          ? const LoadingView()
          : _error != null
              ? ErrorState(message: _error!, onRetry: _load)
              : RefreshIndicator(
                  color: AppColors.brand,
                  onRefresh: _load,
                  child: _cards.isEmpty
                      ? ListView(
                          children: const [
                            SizedBox(height: 70),
                            EmptyState(icon: '💳', text: '还没有会员卡\n开通会员卡享专属折扣与储值礼'),
                          ],
                        )
                      : ListView.separated(
                          padding: const EdgeInsets.all(16),
                          itemCount: _cards.length,
                          separatorBuilder: (_, __) => const SizedBox(height: 12),
                          itemBuilder: (context, i) => _card(_cards[i]),
                        ),
                ),
    );
  }

  Widget _card(Map<String, dynamic> card) {
    final isStored = card['type'] == 'STORED';
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: isStored
              ? const [Color(0xFF7B1739), Color(0xFFB4235A), Color(0xFFD46A92)]
              : const [Color(0xFF2F2A33), Color(0xFF4F4652)],
        ),
        borderRadius: BorderRadius.circular(18),
        boxShadow: [
          BoxShadow(
            color: (isStored ? AppColors.brand : Colors.black).withValues(alpha: 0.18),
            blurRadius: 18,
            offset: const Offset(0, 8),
          )
        ],
      ),
      padding: const EdgeInsets.fromLTRB(18, 16, 18, 14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text(
                cardTypeName(card['type']),
                style: TextStyle(fontSize: 10.5, color: Colors.white.withValues(alpha: 0.72), letterSpacing: 1),
              ),
              const Spacer(),
              if (card['discount'] != null)
                Text(
                  '${card['discount']} 折',
                  style: const TextStyle(fontSize: 11, color: Color(0xFFF3D9A4), fontWeight: FontWeight.w600),
                ),
            ],
          ),
          const SizedBox(height: 6),
          Text(
            card['name']?.toString() ?? '会员卡',
            style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.w600),
          ),
          const SizedBox(height: 16),
          Row(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      isStored ? '储值余额' : '剩余次数',
                      style: TextStyle(fontSize: 10, color: Colors.white.withValues(alpha: 0.62)),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      isStored ? money(card['balance']) : '${card['times'] ?? 0} 次',
                      style: const TextStyle(color: Colors.white, fontSize: 23, fontWeight: FontWeight.w700),
                    ),
                  ],
                ),
              ),
              if (!isStored && asDouble(card['balance']) > 0)
                Text('余额 ${money(card['balance'])}', style: TextStyle(fontSize: 10, color: Colors.white.withValues(alpha: 0.7))),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Text(
                '卡号 ${card['cardNo'] ?? ''}',
                style: TextStyle(fontSize: 10, color: Colors.white.withValues(alpha: 0.55)),
              ),
              const Spacer(),
              Text(
                card['expireAt'] == null ? '长期有效' : '有效期至 ${dateOnly(card['expireAt'])}',
                style: TextStyle(fontSize: 10, color: Colors.white.withValues(alpha: 0.62)),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Divider(color: Colors.white.withValues(alpha: 0.18), height: 1),
          const SizedBox(height: 8),
          Row(
            children: [
              TextButton(
                onPressed: () => _showFlows(card),
                style: TextButton.styleFrom(
                  foregroundColor: Colors.white,
                  padding: EdgeInsets.zero,
                  minimumSize: const Size(60, 30),
                ),
                child: const Text('查看流水', style: TextStyle(fontSize: 11.5)),
              ),
              const Spacer(),
              if (isStored)
                TextButton(
                  onPressed: () => _recharge(card),
                  style: TextButton.styleFrom(
                    foregroundColor: const Color(0xFFF3D9A4),
                    padding: EdgeInsets.zero,
                    minimumSize: const Size(60, 30),
                  ),
                  child: const Text('充值', style: TextStyle(fontSize: 11.5)),
                ),
            ],
          ),
        ],
      ),
    );
  }
}
