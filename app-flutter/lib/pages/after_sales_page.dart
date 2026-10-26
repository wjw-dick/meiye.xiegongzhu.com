import 'package:flutter/material.dart';

import '../core/api.dart';
import '../core/format.dart';
import '../core/theme.dart';
import '../widgets/common.dart';

class AfterSalesPage extends StatefulWidget {
  const AfterSalesPage({super.key});

  @override
  State<AfterSalesPage> createState() => _AfterSalesPageState();
}

class _AfterSalesPageState extends State<AfterSalesPage> {
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
      final list = asMapList(await Api.get<List<dynamic>>('/api/customer/after-sales'));
      if (mounted) setState(() => _list = list);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _compose() async {
    List<Map<String, dynamic>> orders = [];
    try {
      final data = await Api.get<Map<String, dynamic>>('/api/customer/orders/page', query: {'page': 1, 'size': 50});
      orders = asMapList(asMap(data)['records'])
          .map((r) => asMap(r['order'] ?? r))
          .where((o) => ['PAID', 'VERIFIED', 'DONE'].contains(o['status']?.toString()))
          .toList();
    } catch (_) {}
    if (!mounted) return;
    if (orders.isEmpty) {
      showToast(context, '暂无可申请售后的订单');
      return;
    }
    int? orderId = asInt(orders.first['id']);
    String type = 'REFUND';
    final reason = TextEditingController();
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
                const Text('申请售后', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w700)),
                const SizedBox(height: 10),
                DropdownButtonFormField<int>(
                  value: orderId,
                  isExpanded: true,
                  decoration: const InputDecoration(labelText: '关联订单', isDense: true),
                  items: orders
                      .map((o) => DropdownMenuItem<int>(
                            value: asInt(o['id']),
                            child: Text(o['orderNo']?.toString() ?? '', style: const TextStyle(fontSize: 12)),
                          ))
                      .toList(),
                  onChanged: (v) => setSheet(() => orderId = v),
                ),
                const SizedBox(height: 10),
                SegmentedButton<String>(
                  segments: const [
                    ButtonSegment(value: 'REFUND', label: Text('退款')),
                    ButtonSegment(value: 'REWORK', label: Text('返工')),
                    ButtonSegment(value: 'COMPLAINT', label: Text('投诉')),
                  ],
                  selected: {type},
                  onSelectionChanged: (v) => setSheet(() => type = v.first),
                  style: ButtonStyle(textStyle: WidgetStateProperty.all(const TextStyle(fontSize: 12))),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: reason,
                  maxLines: 4,
                  maxLength: 300,
                  decoration: const InputDecoration(hintText: '请描述问题与诉求'),
                ),
                const SizedBox(height: 10),
                FilledButton(
                  onPressed: () async {
                    if (reason.text.trim().isEmpty) {
                      showToast(ctx, '请填写原因');
                      return;
                    }
                    try {
                      await Api.post<Map<String, dynamic>>('/api/customer/after-sales', body: {
                        'orderId': orderId,
                        'type': type,
                        'reason': reason.text.trim(),
                      });
                      if (!ctx.mounted) return;
                      Navigator.pop(ctx);
                      if (mounted) {
                        showToast(context, '已提交，客服会尽快处理');
                        await _load();
                      }
                    } on ApiException catch (e) {
                      if (ctx.mounted) showToast(ctx, e.message);
                    }
                  },
                  child: const Text('提交申请'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
    reason.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('售后退款'),
        actions: [
          TextButton(onPressed: _compose, child: const Text('申请售后', style: TextStyle(fontSize: 12))),
        ],
      ),
      body: _loading
          ? const LoadingView()
          : _error != null
              ? ErrorState(message: _error!, onRetry: _load)
              : RefreshIndicator(
                  color: AppColors.brand,
                  onRefresh: _load,
                  child: _list.isEmpty
                      ? ListView(
                          children: [
                            const SizedBox(height: 60),
                            EmptyState(
                              icon: '↺',
                              text: '暂无售后记录\n服务不满意可随时发起售后',
                              action: '申请售后',
                              onAction: _compose,
                            ),
                          ],
                        )
                      : ListView.separated(
                          padding: const EdgeInsets.all(16),
                          itemCount: _list.length,
                          separatorBuilder: (_, __) => const SizedBox(height: 10),
                          itemBuilder: (context, i) {
                            final a = _list[i];
                            return AppCard(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Row(
                                    children: [
                                      Text(
                                        _typeName(a['type']),
                                        style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
                                      ),
                                      const Spacer(),
                                      StatusPill(status: a['status']?.toString() ?? ''),
                                    ],
                                  ),
                                  const SizedBox(height: 6),
                                  Text('订单 ${a['orderNo'] ?? a['orderId'] ?? '-'}', style: const TextStyle(fontSize: 11, color: AppColors.muted)),
                                  const SizedBox(height: 8),
                                  Text(
                                    a['reason']?.toString() ?? '',
                                    style: const TextStyle(fontSize: 12.5, height: 1.5),
                                  ),
                                  if ((a['result']?.toString() ?? '').isNotEmpty) ...[
                                    const SizedBox(height: 8),
                                    Container(
                                      width: double.infinity,
                                      padding: const EdgeInsets.all(10),
                                      decoration: BoxDecoration(color: AppColors.bg, borderRadius: BorderRadius.circular(8)),
                                      child: Text(
                                        '处理结果：${a['result']}',
                                        style: const TextStyle(fontSize: 11.5, color: AppColors.muted),
                                      ),
                                    ),
                                  ],
                                  const SizedBox(height: 8),
                                  Row(
                                    children: [
                                      Text(dt(a['createdAt']), style: const TextStyle(fontSize: 10, color: AppColors.muted)),
                                      const Spacer(),
                                      if (asDouble(a['compensation']) > 0)
                                        Text('补偿 ${money(a['compensation'])}', style: const TextStyle(fontSize: 11, color: AppColors.gold)),
                                    ],
                                  ),
                                ],
                              ),
                            );
                          },
                        ),
                ),
    );
  }

  String _typeName(dynamic type) {
    switch (type?.toString()) {
      case 'REFUND':
        return '退款申请';
      case 'REWORK':
        return '返工申请';
      case 'COMPLAINT':
        return '服务投诉';
      default:
        return '售后申请';
    }
  }
}
