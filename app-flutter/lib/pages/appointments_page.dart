import 'package:flutter/material.dart';

import '../core/api.dart';
import '../core/format.dart';
import '../core/theme.dart';
import '../widgets/common.dart';
import 'booking_page.dart';

class AppointmentsPage extends StatefulWidget {
  const AppointmentsPage({super.key});

  @override
  State<AppointmentsPage> createState() => _AppointmentsPageState();
}

class _AppointmentsPageState extends State<AppointmentsPage> {
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
      final data = await Api.get<Map<String, dynamic>>('/api/customer/appointments/page', query: {'page': 1, 'size': 50});
      if (!mounted) return;
      setState(() => _list = asMapList(asMap(data)['records']));
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _cancel(Map<String, dynamic> appointment) async {
    if (!['PENDING', 'CONFIRMED'].contains(appointment['status']?.toString())) {
      showToast(context, '当前状态不可取消');
      return;
    }
    final ok = await confirmDialog(
      context,
      title: '取消预约',
      content: '确定取消预约 ${appointment['appointmentNo'] ?? ''} 吗？取消后时段将释放。',
      confirmText: '取消预约',
      danger: true,
    );
    if (!ok) return;
    try {
      await Api.post<Map<String, dynamic>>('/api/customer/appointments/${appointment['id']}/cancel', query: {'reason': '客户取消'});
      if (!mounted) return;
      showToast(context, '已取消预约');
      await _load();
    } on ApiException catch (e) {
      if (mounted) showToast(context, e.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('我的预约'),
        actions: [
          IconButton(
            onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const BookingPage())),
            icon: const Icon(Icons.add),
            tooltip: '新建预约',
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
                  child: _list.isEmpty
                      ? ListView(
                          children: [
                            const SizedBox(height: 60),
                            EmptyState(
                              icon: '📅',
                              text: '还没有预约记录\n给自己安排一次放松吧',
                              action: '去预约',
                              onAction: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const BookingPage())),
                            ),
                          ],
                        )
                      : ListView.separated(
                          padding: const EdgeInsets.all(16),
                          itemCount: _list.length,
                          separatorBuilder: (_, __) => const SizedBox(height: 10),
                          itemBuilder: (context, i) {
                            final a = _list[i];
                            final status = a['status']?.toString() ?? '';
                            final canCancel = ['PENDING', 'CONFIRMED'].contains(status);
                            return AppCard(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Row(
                                    children: [
                                      Text(
                                        dt(a['startTime']),
                                        style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w700),
                                      ),
                                      const Spacer(),
                                      StatusPill(status: status),
                                    ],
                                  ),
                                  const SizedBox(height: 6),
                                  Text(
                                    a['appointmentNo']?.toString() ?? '',
                                    style: const TextStyle(fontSize: 11, color: AppColors.muted),
                                  ),
                                  const SizedBox(height: 10),
                                  const Divider(),
                                  const SizedBox(height: 8),
                                  KVRow(label: '服务项目', value: a['itemName']?.toString() ?? '门店服务'),
                                  KVRow(label: '服务技师', value: a['technicianName']?.toString() ?? '门店安排'),
                                  KVRow(label: '预约门店', value: a['storeName']?.toString() ?? '-'),
                                  if ((a['remark']?.toString() ?? '').isNotEmpty)
                                    KVRow(label: '备注', value: a['remark'].toString()),
                                  const SizedBox(height: 8),
                                  Row(
                                    children: [
                                      TextButton(
                                        onPressed: () => _showDetail(a),
                                        style: TextButton.styleFrom(padding: EdgeInsets.zero),
                                        child: const Text('查看详情', style: TextStyle(fontSize: 12)),
                                      ),
                                      const Spacer(),
                                      if (canCancel)
                                        OutlinedButton(
                                          onPressed: () => _cancel(a),
                                          style: OutlinedButton.styleFrom(
                                            minimumSize: const Size(88, 34),
                                            padding: const EdgeInsets.symmetric(horizontal: 12),
                                            side: const BorderSide(color: AppColors.line),
                                            foregroundColor: AppColors.muted,
                                          ),
                                          child: const Text('取消预约', style: TextStyle(fontSize: 12)),
                                        ),
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

  void _showDetail(Map<String, dynamic> a) {
    showModalBottomSheet<void>(
      context: context,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (ctx) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 18, 20, 20),
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
              const Text('预约详情', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w700)),
              const SizedBox(height: 12),
              KVRow(label: '预约号', value: a['appointmentNo']?.toString() ?? '-'),
              KVRow(label: '状态', value: labelOf(a['status'])),
              KVRow(label: '到店时间', value: dt(a['startTime'])),
              KVRow(label: '结束时间', value: dt(a['endTime'])),
              KVRow(label: '项目', value: a['itemName']?.toString() ?? '-'),
              KVRow(label: '技师', value: a['technicianName']?.toString() ?? '-'),
              KVRow(label: '门店', value: a['storeName']?.toString() ?? '-'),
              KVRow(label: '金额', value: money(a['amount'])),
              KVRow(label: '来源', value: a['source']?.toString() ?? 'APP'),
              KVRow(label: '备注', value: (a['remark']?.toString() ?? '').isEmpty ? '-' : a['remark'].toString()),
              const SizedBox(height: 12),
              FilledButton(onPressed: () => Navigator.pop(ctx), child: const Text('关闭')),
            ],
          ),
        ),
      ),
    );
  }
}
