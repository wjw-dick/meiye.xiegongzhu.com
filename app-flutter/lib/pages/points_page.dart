import 'package:flutter/material.dart';

import '../core/api.dart';
import '../core/format.dart';
import '../core/theme.dart';
import '../widgets/common.dart';

class PointsPage extends StatefulWidget {
  const PointsPage({super.key});

  @override
  State<PointsPage> createState() => _PointsPageState();
}

class _PointsPageState extends State<PointsPage> {
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
      final data = asMap(await Api.get<Map<String, dynamic>>('/api/customer/points'));
      if (mounted) setState(() => _data = data);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final records = asMapList(_data['records']);
    return Scaffold(
      appBar: AppBar(title: const Text('我的积分')),
      body: _loading
          ? const LoadingView()
          : _error != null
              ? ErrorState(message: _error!, onRetry: _load)
              : RefreshIndicator(
                  color: AppColors.brand,
                  onRefresh: _load,
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      Container(
                        padding: const EdgeInsets.fromLTRB(20, 22, 20, 20),
                        decoration: BoxDecoration(
                          gradient: const LinearGradient(colors: [Color(0xFF8C1A45), Color(0xFFB4235A)]),
                          borderRadius: BorderRadius.circular(18),
                        ),
                        child: Row(
                          children: [
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text('可用积分', style: TextStyle(fontSize: 11, color: Colors.white.withValues(alpha: 0.7))),
                                  const SizedBox(height: 4),
                                  Text(
                                    '${_data['balance'] ?? 0}',
                                    style: const TextStyle(fontSize: 32, color: Colors.white, fontWeight: FontWeight.w700),
                                  ),
                                  const SizedBox(height: 6),
                                  Text(
                                    '100 积分抵 1 元 · 消费即得积分',
                                    style: TextStyle(fontSize: 10.5, color: Colors.white.withValues(alpha: 0.68)),
                                  ),
                                ],
                              ),
                            ),
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.end,
                              children: [
                                Text('成长值', style: TextStyle(fontSize: 10.5, color: Colors.white.withValues(alpha: 0.7))),
                                Text(
                                  '${_data['growth'] ?? 0}',
                                  style: const TextStyle(fontSize: 18, color: Color(0xFFF3D9A4), fontWeight: FontWeight.w600),
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 16),
                      const SectionHeader(title: '积分明细'),
                      if (records.isEmpty)
                        const EmptyState(text: '暂无积分记录', icon: '✦')
                      else
                        AppCard(
                          child: Column(
                            children: List.generate(records.length, (i) {
                              final r = records[i];
                              final p = asInt(r['points']);
                              return Column(
                                children: [
                                  Padding(
                                    padding: const EdgeInsets.symmetric(vertical: 9),
                                    child: Row(
                                      children: [
                                        Expanded(
                                          child: Column(
                                            crossAxisAlignment: CrossAxisAlignment.start,
                                            children: [
                                              Text(
                                                r['remark']?.toString() ?? (p >= 0 ? '消费获赠' : '积分抵扣'),
                                                style: const TextStyle(fontSize: 12.5),
                                              ),
                                              const SizedBox(height: 2),
                                              Text(
                                                dt(r['createdAt']),
                                                style: const TextStyle(fontSize: 10, color: AppColors.muted),
                                              ),
                                            ],
                                          ),
                                        ),
                                        Text(
                                          '${p >= 0 ? '+' : ''}$p',
                                          style: TextStyle(
                                            fontSize: 14,
                                            fontWeight: FontWeight.w600,
                                            color: p >= 0 ? AppColors.success : AppColors.text,
                                          ),
                                        ),
                                      ],
                                    ),
                                  ),
                                  if (i != records.length - 1) const Divider(height: 1),
                                ],
                              );
                            }),
                          ),
                        ),
                    ],
                  ),
                ),
    );
  }
}
