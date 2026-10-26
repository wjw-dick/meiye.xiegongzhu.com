import 'package:flutter/material.dart';

import '../core/api.dart';
import '../core/format.dart';
import '../core/payment.dart';
import '../core/theme.dart';
import '../widgets/common.dart';

class OrdersPage extends StatefulWidget {
  const OrdersPage({super.key});

  @override
  State<OrdersPage> createState() => _OrdersPageState();
}

class _OrdersPageState extends State<OrdersPage> {
  final _tabs = const [
    ['', '全部'],
    ['PENDING', '待支付'],
    ['PAID', '已支付'],
    ['VERIFIED', '已核销'],
    ['DONE', '已完成'],
  ];
  String _status = '';
  List<Map<String, dynamic>> _list = [];
  bool _loading = true;
  String? _error;
  int? _payingId;

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
      final data = await Api.get<Map<String, dynamic>>('/api/customer/orders/page', query: {
        'page': 1,
        'size': 50,
        if (_status.isNotEmpty) 'status': _status,
      });
      if (!mounted) return;
      final records = asMapList(asMap(data)['records']);
      setState(() => _list = records.map((r) => asMap(r['order'] ?? r)).toList());
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _openDetail(Map<String, dynamic> order) async {
    try {
      final data = asMap(await Api.get<Map<String, dynamic>>('/api/customer/orders/${order['id']}'));
      if (!mounted) return;
      final items = asMapList(data['items']);
      final payments = asMapList(data['payments']);
      await showModalBottomSheet<void>(
        context: context,
        backgroundColor: Colors.white,
        isScrollControlled: true,
        shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(22))),
        builder: (ctx) => SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 16, 20, 20),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Center(
                  child: Container(
                    width: 36,
                    height: 4,
                    decoration: BoxDecoration(color: AppColors.line, borderRadius: BorderRadius.circular(2)),
                  ),
                ),
                const SizedBox(height: 14),
                Row(
                  children: [
                    const Text('订单详情', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w700)),
                    const Spacer(),
                    StatusPill(status: order['status']?.toString() ?? ''),
                  ],
                ),
                const SizedBox(height: 10),
                KVRow(label: '订单号', value: order['orderNo']?.toString() ?? '-'),
                KVRow(label: '下单时间', value: dt(order['createdAt'])),
                KVRow(label: '订单金额', value: money(order['totalAmount'])),
                KVRow(label: '优惠金额', value: money(order['discountAmount'])),
                KVRow(label: '实付金额', value: money(order['paidAmount']), highlight: true),
                KVRow(label: '支付方式', value: channelName(order['payType'])),
                if (items.isNotEmpty) ...[
                  const SizedBox(height: 10),
                  const Text('服务明细', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600)),
                  const SizedBox(height: 4),
                  ...items.map((it) => Padding(
                        padding: const EdgeInsets.symmetric(vertical: 5),
                        child: Row(
                          children: [
                            Expanded(child: Text('${it['name'] ?? ''} × ${it['quantity'] ?? 1}', style: const TextStyle(fontSize: 12))),
                            Text(money(it['amount']), style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w500)),
                          ],
                        ),
                      )),
                ],
                if (payments.isNotEmpty) ...[
                  const SizedBox(height: 10),
                  const Text('支付流水', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600)),
                  const SizedBox(height: 4),
                  ...payments.map((p) => Padding(
                        padding: const EdgeInsets.symmetric(vertical: 5),
                        child: Row(
                          children: [
                            Expanded(child: Text(channelName(p['channel']), style: const TextStyle(fontSize: 12))),
                            Text(money(p['amount']), style: const TextStyle(fontSize: 12)),
                          ],
                        ),
                      )),
                ],
                if ((order['remark']?.toString() ?? '').isNotEmpty) KVRow(label: '备注', value: order['remark'].toString()),
                const SizedBox(height: 14),
                FilledButton(onPressed: () => Navigator.pop(ctx), child: const Text('关闭')),
              ],
            ),
          ),
        ),
      );
    } on ApiException catch (e) {
      if (mounted) showToast(context, e.message);
    }
  }

  Future<void> _pay(Map<String, dynamic> order) async {
    int? selectedCard;
    String channel = 'WECHAT';
    int pointsUse = 0;
    int? couponId;
    List<Map<String, dynamic>> cards = [];
    List<Map<String, dynamic>> coupons = [];
    int points = 0;
    try {
      final profile = asMap(await Api.get<Map<String, dynamic>>('/api/customer/profile'));
      cards = asMapList(profile['cards']);
      points = asInt(asMap(profile['customer'])['points']);
      coupons = asMapList(await Api.get<List<dynamic>>('/api/customer/coupons', query: {'status': 'UNUSED'}));
    } catch (_) {
      // 支付仍可继续，忽略附加信息加载失败
    }
    if (!mounted) return;
    final payable = asDouble(order['totalAmount']) - asDouble(order['discountAmount']);
    final ok = await showModalBottomSheet<bool>(
      context: context,
      backgroundColor: Colors.white,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(22))),
      builder: (ctx) {
        return StatefulBuilder(
          builder: (ctx, setSheet) {
            return SafeArea(
              child: Padding(
                padding: EdgeInsets.fromLTRB(20, 16, 20, 20 + MediaQuery.viewInsetsOf(ctx).bottom),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Center(
                      child: Container(
                        width: 36,
                        height: 4,
                        decoration: BoxDecoration(color: AppColors.line, borderRadius: BorderRadius.circular(2)),
                      ),
                    ),
                    const SizedBox(height: 16),
                    const Text('选择支付方式', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w700)),
                    const SizedBox(height: 6),
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        const Text('应付 ', style: TextStyle(fontSize: 12, color: AppColors.muted)),
                        PriceText(value: payable, size: 26),
                      ],
                    ),
                    const SizedBox(height: 12),
                    ...payChannels.map((c) {
                      return RadioListTile<String>(
                        value: c.code,
                        groupValue: channel,
                        onChanged: (v) => setSheet(() => channel = v ?? 'WECHAT'),
                        contentPadding: EdgeInsets.zero,
                        dense: true,
                        title: Row(
                          children: [
                            Text(c.icon, style: const TextStyle(fontSize: 17)),
                            const SizedBox(width: 8),
                            Text(c.name, style: const TextStyle(fontSize: 13.5)),
                          ],
                        ),
                        activeColor: AppColors.brand,
                      );
                    }),
                    if (channel == 'BALANCE') ...[
                      if (cards.isEmpty)
                        const Padding(
                          padding: EdgeInsets.only(left: 8, bottom: 6),
                          child: Text('暂无可用会员卡', style: TextStyle(fontSize: 12, color: AppColors.danger)),
                        )
                      else
                        DropdownButtonFormField<int>(
                          value: selectedCard,
                          isExpanded: true,
                          decoration: const InputDecoration(labelText: '选择会员卡', isDense: true),
                          items: cards
                              .map((c) => DropdownMenuItem<int>(
                                    value: asInt(c['id']),
                                    child: Text(
                                      '${c['name'] ?? '会员卡'} · 余额 ${money(c['balance'])} · ${c['times'] ?? 0} 次',
                                      overflow: TextOverflow.ellipsis,
                                      style: const TextStyle(fontSize: 12),
                                    ),
                                  ))
                              .toList(),
                          onChanged: (v) => setSheet(() => selectedCard = v),
                        ),
                    ],
                    if (coupons.isNotEmpty) ...[
                      const SizedBox(height: 6),
                      DropdownButtonFormField<int?>(
                        value: couponId,
                        isExpanded: true,
                        decoration: const InputDecoration(labelText: '优惠券（可选）', isDense: true),
                        items: [
                          const DropdownMenuItem<int?>(value: null, child: Text('不使用优惠券', style: TextStyle(fontSize: 12))),
                          ...coupons.map((c) => DropdownMenuItem<int?>(
                                value: asInt(c['id']),
                                child: Text(
                                  '${c['name'] ?? '优惠券'} · ${c['type'] == 'CASH' ? money(c['amount']) : '${c['discount']}折'}',
                                  overflow: TextOverflow.ellipsis,
                                  style: const TextStyle(fontSize: 12),
                                ),
                              )),
                        ],
                        onChanged: (v) => setSheet(() => couponId = v),
                      ),
                    ],
                    if (points > 0)
                      SwitchListTile(
                        value: pointsUse > 0,
                        onChanged: (v) => setSheet(() => pointsUse = v ? points : 0),
                        contentPadding: EdgeInsets.zero,
                        activeThumbColor: AppColors.brand,
                        dense: true,
                        title: Text('使用积分抵扣（$points 分，最多抵 ${money(points / 100)}）', style: const TextStyle(fontSize: 12)),
                        subtitle: const Text('100 积分 = 1 元', style: TextStyle(fontSize: 10.5, color: AppColors.muted)),
                      ),
                    const SizedBox(height: 14),
                    FilledButton(
                      onPressed: () {
                        if (channel == 'BALANCE' && selectedCard == null) {
                          showToast(ctx, '请选择会员卡');
                          return;
                        }
                        Navigator.pop(ctx, true);
                      },
                      child: const Text('确认支付'),
                    ),
                  ],
                ),
              ),
            );
          },
        );
      },
    );
    if (ok != true) return;
    setState(() => _payingId = asInt(order['id']));
    try {
      await PaymentService.invokeNativeIfNeeded(channel);
      await PaymentService.payOrder(
        orderId: asInt(order['id']),
        channel: channel,
        cardId: selectedCard,
        pointsUse: pointsUse,
        couponId: couponId,
      );
      if (!mounted) return;
      showToast(context, '支付成功');
      await _load();
    } on ApiException catch (e) {
      if (mounted) showToast(context, e.message);
    } finally {
      if (mounted) setState(() => _payingId = null);
    }
  }

  Future<void> _review(Map<String, dynamic> order) async {
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
                const Text('服务评价', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w700)),
                const SizedBox(height: 14),
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
                const SizedBox(height: 10),
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
                const SizedBox(height: 10),
                TextField(
                  controller: content,
                  maxLines: 4,
                  maxLength: 300,
                  decoration: const InputDecoration(hintText: '说说本次服务体验，帮助我们做得更好'),
                ),
                const SizedBox(height: 12),
                FilledButton(
                  onPressed: () async {
                    try {
                      await Api.post<Map<String, dynamic>>('/api/customer/reviews', body: {
                        'orderId': order['id'],
                        'technicianId': order['technicianId'],
                        'score': score,
                        'tags': selectedTags.join(','),
                        'content': content.text.trim(),
                      });
                      if (!ctx.mounted) return;
                      Navigator.pop(ctx);
                      if (mounted) showToast(context, '评价成功，感谢您的反馈');
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

  Future<void> _afterSale(Map<String, dynamic> order) async {
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
                const SizedBox(height: 8),
                Text('订单 ${order['orderNo'] ?? ''}', style: const TextStyle(fontSize: 11.5, color: AppColors.muted)),
                const SizedBox(height: 12),
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
                const SizedBox(height: 12),
                TextField(
                  controller: reason,
                  maxLines: 4,
                  maxLength: 300,
                  decoration: const InputDecoration(hintText: '请描述问题与诉求'),
                ),
                const SizedBox(height: 12),
                FilledButton(
                  onPressed: () async {
                    if (reason.text.trim().isEmpty) {
                      showToast(ctx, '请填写售后原因');
                      return;
                    }
                    try {
                      await Api.post<Map<String, dynamic>>('/api/customer/after-sales', body: {
                        'orderId': order['id'],
                        'type': type,
                        'reason': reason.text.trim(),
                      });
                      if (!ctx.mounted) return;
                      Navigator.pop(ctx);
                      if (mounted) showToast(context, '售后申请已提交，客服将尽快联系您');
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
      appBar: AppBar(title: const Text('我的订单')),
      body: Column(
        children: [
          SizedBox(
            height: 46,
            child: ListView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
              children: _tabs.map((t) {
                final selected = _status == t[0];
                return Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: ChoiceChip(
                    label: Text(t[1]),
                    selected: selected,
                    showCheckmark: false,
                    onSelected: (_) {
                      setState(() => _status = t[0]);
                      _load();
                    },
                    labelStyle: TextStyle(
                      fontSize: 12,
                      color: selected ? AppColors.brand : AppColors.muted,
                      fontWeight: selected ? FontWeight.w600 : FontWeight.w400,
                    ),
                  ),
                );
              }).toList(),
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
                                  EmptyState(icon: '🧾', text: '暂无相关订单\n去体验一次专业服务吧'),
                                ],
                              )
                            : ListView.separated(
                                padding: const EdgeInsets.fromLTRB(16, 6, 16, 20),
                                itemCount: _list.length,
                                separatorBuilder: (_, __) => const SizedBox(height: 10),
                                itemBuilder: (context, i) => _orderCard(_list[i]),
                              ),
                      ),
          ),
        ],
      ),
    );
  }

  Widget _orderCard(Map<String, dynamic> o) {
    final status = o['status']?.toString() ?? '';
    final canPay = status == 'PENDING';
    final canReview = ['PAID', 'VERIFIED', 'DONE'].contains(status);
    final canAfterSale = ['PAID', 'VERIFIED', 'DONE'].contains(status);
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  o['orderNo']?.toString() ?? '',
                  style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: AppColors.muted),
                ),
              ),
              StatusPill(status: status),
            ],
          ),
          const SizedBox(height: 10),
          Row(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '${bizOf(o['bizType'])}服务订单',
                      style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
                    ),
                    const SizedBox(height: 4),
                    Text(dt(o['createdAt']), style: const TextStyle(fontSize: 11, color: AppColors.muted)),
                  ],
                ),
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  PriceText(value: o['paidAmount'] ?? o['totalAmount'], size: 18),
                  const SizedBox(height: 2),
                  Text(
                    channelName(o['payType']),
                    style: const TextStyle(fontSize: 10, color: AppColors.muted),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 10),
          const Divider(),
          const SizedBox(height: 8),
          Row(
            children: [
              const Text('查看详情', style: TextStyle(fontSize: 12, color: AppColors.muted)),
              const Icon(Icons.chevron_right, size: 16, color: AppColors.muted),
              const Spacer(),
              if (canAfterSale)
                TextButton(
                  onPressed: () => _afterSale(o),
                  style: TextButton.styleFrom(padding: EdgeInsets.zero, minimumSize: const Size(48, 30)),
                  child: const Text('售后', style: TextStyle(fontSize: 12, color: AppColors.muted)),
                ),
              if (canReview)
                TextButton(
                  onPressed: () => _review(o),
                  style: TextButton.styleFrom(minimumSize: const Size(56, 30)),
                  child: const Text('评价', style: TextStyle(fontSize: 12)),
                ),
              if (canPay)
                SizedBox(
                  height: 32,
                  child: FilledButton(
                    onPressed: _payingId == asInt(o['id']) ? null : () => _pay(o),
                    style: FilledButton.styleFrom(
                      minimumSize: const Size(82, 32),
                      padding: const EdgeInsets.symmetric(horizontal: 14),
                      textStyle: const TextStyle(fontSize: 12),
                    ),
                    child: Text(_payingId == asInt(o['id']) ? '支付中' : '立即支付'),
                  ),
                ),
            ],
          ),
        ],
      ),
    );
  }
}
