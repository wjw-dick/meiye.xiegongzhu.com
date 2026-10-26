import 'package:flutter/material.dart';

import '../core/api.dart';
import '../core/format.dart';
import '../core/theme.dart';
import '../widgets/common.dart';

class MessagesPage extends StatefulWidget {
  const MessagesPage({super.key});

  @override
  State<MessagesPage> createState() => _MessagesPageState();
}

class _MessagesPageState extends State<MessagesPage> {
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
      final list = asMapList(await Api.get<List<dynamic>>('/api/customer/messages'));
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
      appBar: AppBar(title: const Text('消息中心')),
      body: _loading
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
                            EmptyState(icon: '✉️', text: '暂无消息\n预约提醒、储值到账通知会出现在这里'),
                          ],
                        )
                      : ListView.separated(
                          padding: const EdgeInsets.all(16),
                          itemCount: _list.length,
                          separatorBuilder: (_, __) => const SizedBox(height: 10),
                          itemBuilder: (context, i) {
                            final m = _list[i];
                            return AppCard(
                              onTap: () => _detail(m),
                              child: Row(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Container(
                                    width: 38,
                                    height: 38,
                                    alignment: Alignment.center,
                                    decoration: BoxDecoration(
                                      color: AppColors.brand.withValues(alpha: 0.07),
                                      borderRadius: BorderRadius.circular(11),
                                    ),
                                    child: Text(_icon(m['code']?.toString()), style: const TextStyle(fontSize: 17)),
                                  ),
                                  const SizedBox(width: 10),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Row(
                                          children: [
                                            Expanded(
                                              child: Text(
                                                m['title']?.toString() ?? '消息',
                                                maxLines: 1,
                                                overflow: TextOverflow.ellipsis,
                                                style: const TextStyle(fontSize: 13.5, fontWeight: FontWeight.w600),
                                              ),
                                            ),
                                            Text(
                                              dt(m['sentAt'] ?? m['createdAt']).length >= 16
                                                  ? dt(m['sentAt'] ?? m['createdAt']).substring(5)
                                                  : '',
                                              style: const TextStyle(fontSize: 10, color: AppColors.muted),
                                            ),
                                          ],
                                        ),
                                        const SizedBox(height: 4),
                                        Text(
                                          m['content']?.toString() ?? '',
                                          maxLines: 2,
                                          overflow: TextOverflow.ellipsis,
                                          style: const TextStyle(fontSize: 11.5, color: AppColors.muted, height: 1.45),
                                        ),
                                      ],
                                    ),
                                  ),
                                  const SizedBox(width: 4),
                                  const Icon(Icons.chevron_right, size: 18, color: AppColors.muted),
                                ],
                              ),
                            );
                          },
                        ),
                ),
    );
  }

  String _icon(String? code) {
    switch (code) {
      case 'APPOINTMENT_CONFIRM':
      case 'APPOINTMENT_REMIND':
        return '📅';
      case 'VERIFY':
        return '✅';
      case 'STORED':
        return '💰';
      case 'EXPIRY':
        return '⏰';
      case 'FOLLOWUP':
        return '💬';
      default:
        return '✉️';
    }
  }

  void _detail(Map<String, dynamic> m) {
    showModalBottomSheet<void>(
      context: context,
      backgroundColor: Colors.white,
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
              const SizedBox(height: 16),
              Text(m['title']?.toString() ?? '消息', style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w700)),
              const SizedBox(height: 6),
              Text(
                '${channelName(m['channel'])} · ${dt(m['sentAt'] ?? m['createdAt'])}',
                style: const TextStyle(fontSize: 11, color: AppColors.muted),
              ),
              const SizedBox(height: 14),
              Text(m['content']?.toString() ?? '', style: const TextStyle(fontSize: 13.5, height: 1.7)),
              const SizedBox(height: 18),
              FilledButton(onPressed: () => Navigator.pop(ctx), child: const Text('知道了')),
            ],
          ),
        ),
      ),
    );
  }
}
