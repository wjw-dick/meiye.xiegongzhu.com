import 'package:flutter/material.dart';

import '../core/api.dart';
import '../core/format.dart';
import '../core/theme.dart';
import '../widgets/common.dart';
import '../widgets/cover_image.dart';
import 'booking_page.dart';

class ItemDetailPage extends StatefulWidget {
  const ItemDetailPage({super.key, required this.itemId});

  final int itemId;

  @override
  State<ItemDetailPage> createState() => _ItemDetailPageState();
}

class _ItemDetailPageState extends State<ItemDetailPage> {
  Map<String, dynamic>? _item;
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
      final list = asMapList(await Api.get<List<dynamic>>('/api/customer/items'));
      final found = list.where((e) => asInt(e['id']) == widget.itemId).toList();
      if (found.isEmpty) throw ApiException('项目已下架或不存在');
      if (!mounted) return;
      setState(() => _item = found.first);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final item = _item;
    return Scaffold(
      appBar: AppBar(title: const Text('项目详情')),
      body: _loading
          ? const LoadingView()
          : _error != null
              ? ErrorState(message: _error!, onRetry: _load)
              : ListView(
                  padding: const EdgeInsets.fromLTRB(16, 4, 16, 100),
                  children: [
                    CoverImage(url: item?['image']?.toString(), width: double.infinity, height: 210, radius: 18, placeholder: '美'),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            item?['name']?.toString() ?? '',
                            style: const TextStyle(fontSize: 21, fontWeight: FontWeight.w700),
                          ),
                        ),
                        Pill(text: bizOf(item?['bizType'])),
                      ],
                    ),
                    const SizedBox(height: 10),
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        PriceText(value: item?['price'], size: 26),
                        const SizedBox(width: 8),
                        if (asDouble(item?['memberPrice']) > 0)
                          Padding(
                            padding: const EdgeInsets.only(bottom: 3),
                            child: Text(
                              '会员价 ${money(item?['memberPrice'])}',
                              style: const TextStyle(fontSize: 12, color: AppColors.gold),
                            ),
                          ),
                        const Spacer(),
                        Text(
                          '约 ${item?['duration'] ?? 60} 分钟',
                          style: const TextStyle(fontSize: 12, color: AppColors.muted),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                    AppCard(
                      child: Column(
                        children: [
                          const Row(
                            children: [
                              Text('服务说明', style: TextStyle(fontSize: 14, fontWeight: FontWeight.w700)),
                            ],
                          ),
                          const SizedBox(height: 10),
                          Text(
                            (item?['description']?.toString().isNotEmpty ?? false)
                                ? item!['description'].toString()
                                : '由专业技师一对一服务，使用门店甄选产品，服务前进行肤质/身体状况评估，服务后提供居家护理建议。',
                            style: const TextStyle(fontSize: 13, height: 1.75, color: AppColors.text),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 12),
                    AppCard(
                      child: Column(
                        children: [
                          _step('01', '在线预约', '选择门店、技师与到店时间'),
                          _step('02', '到店服务', '前台核销，专业技师全程服务'),
                          _step('03', '评价有礼', '服务完成后评价，积分自动到账'),
                        ],
                      ),
                    ),
                    const SizedBox(height: 12),
                    AppCard(
                      child: Column(
                        children: [
                          const KVRow(label: '项目分类', value: '专业护理'),
                          KVRow(label: '适用门店', value: '全部门店通用'),
                          const KVRow(label: '预约提醒', value: '服务前 2 小时消息提醒'),
                          const KVRow(label: '售后保障', value: '不满意可申请售后'),
                        ],
                      ),
                    ),
                  ],
                ),
      bottomNavigationBar: _loading || _error != null
          ? null
          : SafeArea(
              child: Container(
                padding: const EdgeInsets.fromLTRB(16, 10, 16, 12),
                decoration: const BoxDecoration(
                  color: Colors.white,
                  border: Border(top: BorderSide(color: AppColors.line)),
                ),
                child: Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Text('到店支付 / 会员卡抵扣', style: TextStyle(fontSize: 10.5, color: AppColors.muted)),
                          PriceText(value: item?['price'], size: 19),
                        ],
                      ),
                    ),
                    SizedBox(
                      width: 168,
                      child: FilledButton(
                        onPressed: () => Navigator.push(
                          context,
                          MaterialPageRoute(builder: (_) => BookingPage(initialItemId: widget.itemId)),
                        ),
                        child: const Text('立即预约'),
                      ),
                    ),
                  ],
                ),
              ),
            ),
    );
  }

  Widget _step(String no, String title, String desc) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 26,
            height: 26,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: AppColors.brand.withValues(alpha: 0.08),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Text(no, style: const TextStyle(fontSize: 10, color: AppColors.brand, fontWeight: FontWeight.w700)),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: const TextStyle(fontSize: 12.5, fontWeight: FontWeight.w600)),
                const SizedBox(height: 2),
                Text(desc, style: const TextStyle(fontSize: 11, color: AppColors.muted)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
