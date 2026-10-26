import 'package:flutter/material.dart';

import '../core/api.dart';
import '../core/format.dart';
import '../core/theme.dart';
import '../widgets/common.dart';
import '../widgets/cover_image.dart';
import 'booking_page.dart';
import 'item_detail_page.dart';

class ItemsPage extends StatefulWidget {
  const ItemsPage({super.key, this.initialTab = 0});

  final int initialTab;

  @override
  State<ItemsPage> createState() => _ItemsPageState();
}

class _ItemsPageState extends State<ItemsPage> with SingleTickerProviderStateMixin {
  late final TabController _tabs = TabController(length: 2, vsync: this, initialIndex: widget.initialTab);
  final _search = TextEditingController();
  List<Map<String, dynamic>> _items = [];
  List<Map<String, dynamic>> _packages = [];
  bool _loading = true;
  String? _error;
  String _biz = 'ALL';

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _tabs.dispose();
    _search.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final results = await Future.wait([
        Api.get<List<dynamic>>('/api/customer/items'),
        Api.get<List<dynamic>>('/api/customer/packages'),
      ]);
      if (!mounted) return;
      setState(() {
        _items = asMapList(results[0]);
        _packages = asMapList(results[1]);
      });
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  List<Map<String, dynamic>> get _filteredItems {
    final kw = _search.text.trim();
    return _items.where((item) {
      final okBiz = _biz == 'ALL' || item['bizType'] == _biz;
      final okKw = kw.isEmpty ||
          (item['name']?.toString().contains(kw) ?? false) ||
          (item['description']?.toString().contains(kw) ?? false);
      return okBiz && okKw;
    }).toList();
  }

  Set<String> get _bizTypes => _items.map((e) => e['bizType']?.toString() ?? '').where((e) => e.isNotEmpty).toSet();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('服务项目'),
        bottom: TabBar(
          controller: _tabs,
          labelColor: AppColors.brand,
          unselectedLabelColor: AppColors.muted,
          indicatorColor: AppColors.brand,
          indicatorSize: TabBarIndicatorSize.label,
          tabs: const [Tab(text: '单项服务'), Tab(text: '套餐礼包')],
        ),
      ),
      body: _loading
          ? const LoadingView()
          : _error != null
              ? ErrorState(message: _error!, onRetry: _load)
              : TabBarView(
                  controller: _tabs,
                  children: [_buildItems(), _buildPackages()],
                ),
    );
  }

  Widget _buildItems() {
    final list = _filteredItems;
    return RefreshIndicator(
      color: AppColors.brand,
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(16, 14, 16, 24),
        children: [
          TextField(
            controller: _search,
            onChanged: (_) => setState(() {}),
            decoration: InputDecoration(
              hintText: '搜索项目 / 功效',
              prefixIcon: const Icon(Icons.search, size: 20),
              isDense: true,
              filled: true,
              fillColor: Colors.white,
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: BorderSide.none,
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: BorderSide.none,
              ),
            ),
          ),
          const SizedBox(height: 12),
          SizedBox(
            height: 34,
            child: ListView(
              scrollDirection: Axis.horizontal,
              children: [
                _bizChip('ALL', '全部'),
                ..._bizTypes.map((b) => _bizChip(b, bizOf(b))),
              ],
            ),
          ),
          const SizedBox(height: 14),
          if (list.isEmpty)
            const EmptyState(text: '没有找到相关项目', icon: '🔍')
          else
            ...list.map((item) => Padding(
                  padding: const EdgeInsets.only(bottom: 10),
                  child: AppCard(
                    padding: const EdgeInsets.all(12),
                    onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute(builder: (_) => ItemDetailPage(itemId: asInt(item['id']))),
                    ),
                    child: Row(
                      children: [
                        CoverImage(url: item['image']?.toString(), width: 86, height: 86),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  Expanded(
                                    child: Text(
                                      item['name']?.toString() ?? '',
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                      style: const TextStyle(fontSize: 14.5, fontWeight: FontWeight.w600),
                                    ),
                                  ),
                                  Pill(text: bizOf(item['bizType'])),
                                ],
                              ),
                              const SizedBox(height: 5),
                              Text(
                                item['description']?.toString() ?? '专业服务，静候体验',
                                maxLines: 2,
                                overflow: TextOverflow.ellipsis,
                                style: const TextStyle(fontSize: 11.5, color: AppColors.muted, height: 1.4),
                              ),
                              const SizedBox(height: 10),
                              Row(
                                crossAxisAlignment: CrossAxisAlignment.end,
                                children: [
                                  PriceText(value: item['price'], size: 17),
                                  if (asDouble(item['memberPrice']) > 0) ...[
                                    const SizedBox(width: 6),
                                    Text(
                                      '会员 ${money(item['memberPrice'])}',
                                      style: const TextStyle(fontSize: 10.5, color: AppColors.gold),
                                    ),
                                  ],
                                  const Spacer(),
                                  Text(
                                    '${item['duration'] ?? 60} 分钟',
                                    style: const TextStyle(fontSize: 10.5, color: AppColors.muted),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                )),
        ],
      ),
    );
  }

  Widget _bizChip(String code, String label) {
    final selected = _biz == code;
    return Padding(
      padding: const EdgeInsets.only(right: 8),
      child: ChoiceChip(
        label: Text(label),
        selected: selected,
        showCheckmark: false,
        onSelected: (_) => setState(() => _biz = code),
        labelStyle: TextStyle(
          fontSize: 12,
          color: selected ? AppColors.brand : AppColors.muted,
          fontWeight: selected ? FontWeight.w600 : FontWeight.w400,
        ),
      ),
    );
  }

  Widget _buildPackages() {
    if (_packages.isEmpty) {
      return const EmptyState(text: '暂未上架套餐', icon: '🎁');
    }
    return RefreshIndicator(
      color: AppColors.brand,
      onRefresh: _load,
      child: ListView.separated(
        padding: const EdgeInsets.fromLTRB(16, 14, 16, 24),
        itemCount: _packages.length,
        separatorBuilder: (_, __) => const SizedBox(height: 10),
        itemBuilder: (context, i) {
          final p = _packages[i];
          final items = p['itemsJson']?.toString() ?? '';
          return AppCard(
            onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const BookingPage())),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                      decoration: BoxDecoration(
                        color: AppColors.gold.withValues(alpha: 0.14),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text(
                        p['type']?.toString() == 'TIMES' ? '次卡套餐' : '超值礼包',
                        style: const TextStyle(fontSize: 10.5, color: Color(0xFF9A7124), fontWeight: FontWeight.w500),
                      ),
                    ),
                    const Spacer(),
                    PriceText(value: p['price'], size: 18),
                  ],
                ),
                const SizedBox(height: 8),
                Text(p['name']?.toString() ?? '', style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600)),
                const SizedBox(height: 4),
                Text(
                  p['description']?.toString() ?? '含 ${p['times'] ?? 0} 次服务 · 有效期 ${p['validDays'] ?? 0} 天',
                  style: const TextStyle(fontSize: 11.5, color: AppColors.muted),
                ),
                if (items.isNotEmpty && items != 'null') ...[
                  const SizedBox(height: 6),
                  Text(
                    items,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontSize: 10.5, color: AppColors.muted, height: 1.4),
                  ),
                ],
                const SizedBox(height: 10),
                const Divider(),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Text(
                      '门市价 ${money(p['faceValue'])}',
                      style: const TextStyle(fontSize: 11, color: AppColors.muted, decoration: TextDecoration.lineThrough),
                    ),
                    const Spacer(),
                    const Text('立即了解 ›', style: TextStyle(fontSize: 12, color: AppColors.brand)),
                  ],
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}
