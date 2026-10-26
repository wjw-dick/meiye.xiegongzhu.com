import 'package:flutter/material.dart';

import '../core/api.dart';
import '../core/format.dart';
import '../core/theme.dart';
import '../widgets/common.dart';

class ReviewsPage extends StatefulWidget {
  const ReviewsPage({super.key});

  @override
  State<ReviewsPage> createState() => _ReviewsPageState();
}

class _ReviewsPageState extends State<ReviewsPage> {
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
      final list = asMapList(await Api.get<List<dynamic>>('/api/customer/reviews'));
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
      showToast(context, '暂无可评价的订单');
      return;
    }
    int? orderId = asInt(orders.first['id']);
    int score = 5;
    final content = TextEditingController();
    final selectedTags = <String>{};
    const tags = ['专业', '环境舒适', '服务贴心', '效果满意', '性价比高'];
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
                const Text('写评价', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w700)),
                const SizedBox(height: 10),
                DropdownButtonFormField<int>(
                  value: orderId,
                  isExpanded: true,
                  decoration: const InputDecoration(labelText: '选择订单', isDense: true),
                  items: orders
                      .map((o) => DropdownMenuItem<int>(
                            value: asInt(o['id']),
                            child: Text(
                              '${o['orderNo']} · ${money(o['paidAmount'] ?? o['totalAmount'])}',
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(fontSize: 12),
                            ),
                          ))
                      .toList(),
                  onChanged: (v) => setSheet(() => orderId = v),
                ),
                const SizedBox(height: 10),
                Row(
                  children: List.generate(5, (i) {
                    return GestureDetector(
                      onTap: () => setSheet(() => score = i + 1),
                      child: Padding(
                        padding: const EdgeInsets.only(right: 4),
                        child: Icon(i < score ? Icons.star_rounded : Icons.star_outline_rounded, color: AppColors.gold, size: 30),
                      ),
                    );
                  }),
                ),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 8,
                  children: tags.map((t) {
                    final selected = selectedTags.contains(t);
                    return ChoiceChip(
                      label: Text(t),
                      selected: selected,
                      showCheckmark: false,
                      onSelected: (_) => setSheet(() {
                        selected ? selectedTags.remove(t) : selectedTags.add(t);
                      }),
                      labelStyle: TextStyle(fontSize: 11.5, color: selected ? AppColors.brand : AppColors.muted),
                    );
                  }).toList(),
                ),
                const SizedBox(height: 8),
                TextField(
                  controller: content,
                  maxLines: 4,
                  maxLength: 300,
                  decoration: const InputDecoration(hintText: '分享本次服务体验'),
                ),
                const SizedBox(height: 10),
                FilledButton(
                  onPressed: () async {
                    try {
                      await Api.post<Map<String, dynamic>>('/api/customer/reviews', body: {
                        'orderId': orderId,
                        'score': score,
                        'tags': selectedTags.join(','),
                        'content': content.text.trim(),
                      });
                      if (!ctx.mounted) return;
                      Navigator.pop(ctx);
                      if (mounted) {
                        showToast(context, '评价成功');
                        await _load();
                      }
                    } on ApiException catch (e) {
                      if (ctx.mounted) showToast(ctx, e.message);
                    }
                  },
                  child: const Text('提交评价'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
    content.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('服务评价'),
        actions: [
          TextButton(onPressed: _compose, child: const Text('写评价', style: TextStyle(fontSize: 12))),
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
                              icon: '★',
                              text: '还没有评价\n完成服务后说说你的感受吧',
                              action: '写评价',
                              onAction: _compose,
                            ),
                          ],
                        )
                      : ListView.separated(
                          padding: const EdgeInsets.all(16),
                          itemCount: _list.length,
                          separatorBuilder: (_, __) => const SizedBox(height: 10),
                          itemBuilder: (context, i) {
                            final r = _list[i];
                            return AppCard(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Row(
                                    children: [
                                      ...List.generate(5, (i) {
                                        return Icon(
                                          i < asInt(r['score']) ? Icons.star_rounded : Icons.star_outline_rounded,
                                          size: 17,
                                          color: AppColors.gold,
                                        );
                                      }),
                                      const Spacer(),
                                      Text(dateOnly(r['createdAt']), style: const TextStyle(fontSize: 10, color: AppColors.muted)),
                                    ],
                                  ),
                                  const SizedBox(height: 8),
                                  Text(
                                    (r['content']?.toString() ?? '').isEmpty ? '默认好评' : r['content'].toString(),
                                    style: const TextStyle(fontSize: 13, height: 1.55),
                                  ),
                                  if ((r['tags']?.toString() ?? '').isNotEmpty) ...[
                                    const SizedBox(height: 8),
                                    Wrap(
                                      spacing: 6,
                                      runSpacing: 6,
                                      children: r['tags'].toString().split(',').map((t) => Pill(text: t)).toList(),
                                    ),
                                  ],
                                  if ((r['reply']?.toString() ?? '').isNotEmpty) ...[
                                    const SizedBox(height: 10),
                                    Container(
                                      width: double.infinity,
                                      padding: const EdgeInsets.all(10),
                                      decoration: BoxDecoration(
                                        color: AppColors.bg,
                                        borderRadius: BorderRadius.circular(8),
                                      ),
                                      child: Text(
                                        '门店回复：${r['reply']}',
                                        style: const TextStyle(fontSize: 11.5, color: AppColors.muted, height: 1.5),
                                      ),
                                    ),
                                  ],
                                ],
                              ),
                            );
                          },
                        ),
                ),
    );
  }
}
