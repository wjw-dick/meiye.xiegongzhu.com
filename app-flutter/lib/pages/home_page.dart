import 'package:flutter/material.dart';

import '../core/api.dart';
import '../core/auth.dart';
import '../core/format.dart';
import '../core/theme.dart';
import '../widgets/common.dart';
import '../widgets/cover_image.dart';
import 'appointments_page.dart';
import 'cards_page.dart';
import 'coupons_page.dart';
import 'item_detail_page.dart';
import 'items_page.dart';
import 'messages_page.dart';
import 'points_page.dart';
import 'reviews_page.dart';
import 'after_sales_page.dart';
import 'booking_page.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key, this.onOpenTab});

  final ValueChanged<int>? onOpenTab;

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
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
      final data = await Api.get<Map<String, dynamic>>('/api/customer/home');
      if (!mounted) return;
      setState(() => _data = asMap(data));
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final customer = asMap(_data['customer']);
    final cards = asMapList(_data['cards']);
    final coupons = asMapList(_data['coupons']);
    final appointments = asMapList(_data['appointments']);
    final items = asMapList(_data['items']);
    final packages = asMapList(_data['packages']);
    final notices = asMapList(_data['notices']);
    final user = AuthStore.instance.user;

    return RefreshIndicator(
      color: AppColors.brand,
      onRefresh: _load,
      child: CustomScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        slivers: [
          SliverToBoxAdapter(
            child: Container(
              padding: const EdgeInsets.fromLTRB(20, 58, 20, 52),
              decoration: const BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                  colors: [Color(0xFF8C1A45), Color(0xFFB4235A), Color(0xFFCE5884)],
                ),
                borderRadius: BorderRadius.only(
                  bottomLeft: Radius.circular(30),
                  bottomRight: Radius.circular(30),
                ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      const BrandAvatar(text: '美', size: 46),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '${greeting()}，${customer['name'] ?? user?.name ?? '会员'}',
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.w600),
                            ),
                            const SizedBox(height: 3),
                            Text(
                              '${customer['level'] ?? '普通会员'} · 成长值 ${customer['growth'] ?? 0}',
                              style: TextStyle(color: Colors.white.withValues(alpha: 0.76), fontSize: 11.5),
                            ),
                          ],
                        ),
                      ),
                      GestureDetector(
                        onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const MessagesPage())),
                        child: Stack(
                          clipBehavior: Clip.none,
                          children: [
                            const Icon(Icons.notifications_none_rounded, color: Colors.white, size: 25),
                            if (asInt(_data['unreadMessages']) > 0)
                              Positioned(
                                right: -2,
                                top: -2,
                                child: Container(
                                  width: 7,
                                  height: 7,
                                  decoration: const BoxDecoration(color: AppColors.gold, shape: BoxShape.circle),
                                ),
                              ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 22),
                  Row(
                    children: [
                      _stat('${customer['points'] ?? 0}', '积分'),
                      _divider(),
                      _stat('${cards.length}', '会员卡'),
                      _divider(),
                      _stat('${coupons.length}', '优惠券'),
                    ],
                  ),
                ],
              ),
            ),
          ),
          SliverPadding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            sliver: SliverList(
              delegate: SliverChildListDelegate([
                Transform.translate(
                  offset: const Offset(0, -26),
                  child: AppCard(
                    padding: const EdgeInsets.fromLTRB(8, 14, 8, 8),
                    child: Column(
                      children: [
                        const Row(
                          children: [
                            Padding(
                              padding: EdgeInsets.only(left: 8),
                              child: Text('快捷服务', style: TextStyle(fontSize: 14, fontWeight: FontWeight.w700)),
                            ),
                            Spacer(),
                          ],
                        ),
                        const SizedBox(height: 6),
                        GridView.count(
                          crossAxisCount: 4,
                          shrinkWrap: true,
                          physics: const NeverScrollableScrollPhysics(),
                          childAspectRatio: 0.92,
                          children: [
                            QuickAction(
                              icon: '✚',
                              label: '在线预约',
                              onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const BookingPage())),
                            ),
                            QuickAction(
                              icon: '◷',
                              label: '我的预约',
                              onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const AppointmentsPage())),
                            ),
                            QuickAction(
                              icon: '▣',
                              label: '会员卡包',
                              onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const CardsPage())),
                            ),
                            QuickAction(
                              icon: '⧉',
                              label: '我的订单',
                              onTap: () => widget.onOpenTab?.call(2),
                            ),
                            QuickAction(
                              icon: '◈',
                              label: '优惠券',
                              onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const CouponsPage())),
                            ),
                            QuickAction(
                              icon: '✦',
                              label: '我的积分',
                              onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const PointsPage())),
                            ),
                            QuickAction(
                              icon: '★',
                              label: '服务评价',
                              onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const ReviewsPage())),
                            ),
                            QuickAction(
                              icon: '↺',
                              label: '售后',
                              onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const AfterSalesPage())),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ),
                if (_error != null) ErrorState(message: _error!, onRetry: _load),
                if (_loading) const ListSkeleton(count: 2),
                if (!_loading && _error == null) ...[
                  if (appointments.isNotEmpty) ...[
                    SectionHeader(
                      title: '即将到来',
                      action: '全部预约',
                      onAction: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const AppointmentsPage())),
                    ),
                    AppCard(
                      onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const AppointmentsPage())),
                      child: Row(
                        children: [
                          Container(
                            width: 54,
                            padding: const EdgeInsets.symmetric(vertical: 8),
                            alignment: Alignment.center,
                            decoration: BoxDecoration(
                              color: AppColors.brand.withValues(alpha: 0.06),
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: Column(
                              children: [
                                Text(
                                  dt(appointments.first['startTime']).substring(5, 10),
                                  style: const TextStyle(fontSize: 11, color: AppColors.muted),
                                ),
                                Text(
                                  dt(appointments.first['startTime']).substring(11, 16),
                                  style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700, color: AppColors.brand),
                                ),
                              ],
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  appointments.first['itemName']?.toString() ?? '门店服务',
                                  style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
                                ),
                                const SizedBox(height: 4),
                                Text(
                                  '预约号 ${appointments.first['appointmentNo'] ?? ''}',
                                  style: const TextStyle(fontSize: 11.5, color: AppColors.muted),
                                ),
                              ],
                            ),
                          ),
                          StatusPill(status: appointments.first['status']?.toString() ?? ''),
                        ],
                      ),
                    ),
                    const SizedBox(height: 18),
                  ],
                  if (notices.isNotEmpty) ...[
                    SectionHeader(title: '门店公告'),
                    AppCard(
                      child: Column(
                        children: notices.take(2).map((n) {
                          return Padding(
                            padding: const EdgeInsets.symmetric(vertical: 5),
                            child: Row(
                              children: [
                                const Text('📣', style: TextStyle(fontSize: 13)),
                                const SizedBox(width: 8),
                                Expanded(
                                  child: Text(
                                    n['title']?.toString() ?? '',
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: const TextStyle(fontSize: 12.5),
                                  ),
                                ),
                                Text(
                                  dateOnly(n['publishedAt']),
                                  style: const TextStyle(fontSize: 10.5, color: AppColors.muted),
                                ),
                              ],
                            ),
                          );
                        }).toList(),
                      ),
                    ),
                    const SizedBox(height: 18),
                  ],
                  SectionHeader(
                    title: '臻选项目',
                    subtitle: '按启用品类精选',
                    action: '查看全部',
                    onAction: () => widget.onOpenTab?.call(1),
                  ),
                  if (items.isEmpty)
                    const AppCard(child: EmptyState(text: '暂无可预约项目', icon: '🌿'))
                  else
                    SizedBox(
                      height: 208,
                      child: ListView.separated(
                        scrollDirection: Axis.horizontal,
                        itemCount: items.length,
                        separatorBuilder: (_, __) => const SizedBox(width: 10),
                        itemBuilder: (context, i) {
                          final item = items[i];
                          return GestureDetector(
                            onTap: () => Navigator.push(
                              context,
                              MaterialPageRoute(builder: (_) => ItemDetailPage(itemId: asInt(item['id']))),
                            ),
                            child: Container(
                              width: 150,
                              decoration: BoxDecoration(
                                color: Colors.white,
                                borderRadius: BorderRadius.circular(16),
                                border: Border.all(color: AppColors.line.withValues(alpha: 0.6)),
                              ),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  CoverImage(url: item['image']?.toString(), width: 150, height: 105, radius: 15),
                                  Padding(
                                    padding: const EdgeInsets.fromLTRB(10, 8, 10, 8),
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text(
                                          item['name']?.toString() ?? '',
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                          style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600),
                                        ),
                                        const SizedBox(height: 4),
                                        Row(
                                          children: [
                                            PriceText(value: item['price'], size: 15),
                                            const Spacer(),
                                            Text(
                                              '${item['duration'] ?? 60}分钟',
                                              style: const TextStyle(fontSize: 10, color: AppColors.muted),
                                            ),
                                          ],
                                        ),
                                        const SizedBox(height: 4),
                                        Pill(text: bizOf(item['bizType'])),
                                      ],
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          );
                        },
                      ),
                    ),
                  const SizedBox(height: 18),
                  if (packages.isNotEmpty) ...[
                    SectionHeader(
                      title: '超值套餐',
                      action: '更多',
                      onAction: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const ItemsPage(initialTab: 1))),
                    ),
                    ...packages.take(2).map((p) {
                      return Padding(
                        padding: const EdgeInsets.only(bottom: 10),
                        child: AppCard(
                          onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const BookingPage())),
                          child: Row(
                            children: [
                              Container(
                                width: 44,
                                height: 44,
                                alignment: Alignment.center,
                                decoration: BoxDecoration(
                                  color: AppColors.gold.withValues(alpha: 0.12),
                                  borderRadius: BorderRadius.circular(12),
                                ),
                                child: const Text('🎁', style: TextStyle(fontSize: 20)),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(p['name']?.toString() ?? '', style: const TextStyle(fontSize: 13.5, fontWeight: FontWeight.w600)),
                                    const SizedBox(height: 3),
                                    Text(
                                      '${p['times'] ?? 0} 次 · 有效期 ${p['validDays'] ?? 0} 天',
                                      style: const TextStyle(fontSize: 11, color: AppColors.muted),
                                    ),
                                  ],
                                ),
                              ),
                              PriceText(value: p['price'], size: 16),
                            ],
                          ),
                        ),
                      );
                    }),
                    const SizedBox(height: 10),
                  ],
                  Center(
                    child: Text(
                      '已经到底啦 · 好好爱自己',
                      style: TextStyle(fontSize: 11, color: AppColors.muted.withValues(alpha: 0.7)),
                    ),
                  ),
                  const SizedBox(height: 24),
                ],
              ]),
            ),
          ),
        ],
      ),
    );
  }

  Widget _stat(String value, String label) {
    return Expanded(
      child: Column(
        children: [
          Text(value, style: const TextStyle(color: Colors.white, fontSize: 19, fontWeight: FontWeight.w700)),
          const SizedBox(height: 2),
          Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.72), fontSize: 10.5)),
        ],
      ),
    );
  }

  Widget _divider() => Container(width: 1, height: 26, color: Colors.white.withValues(alpha: 0.18));
}
