import 'package:flutter/material.dart';

import '../core/api.dart';
import '../core/format.dart';
import '../core/theme.dart';
import '../widgets/common.dart';
import 'appointments_page.dart';

class BookingPage extends StatefulWidget {
  const BookingPage({super.key, this.initialItemId});

  final int? initialItemId;

  @override
  State<BookingPage> createState() => _BookingPageState();
}

class _BookingPageState extends State<BookingPage> {
  List<Map<String, dynamic>> _items = [];
  List<Map<String, dynamic>> _stores = [];
  List<Map<String, dynamic>> _techs = [];
  List<Map<String, dynamic>> _slots = [];
  int? _itemId;
  int? _storeId;
  int? _techId;
  String _date = tomorrowIso();
  Map<String, dynamic>? _slot;
  final _remark = TextEditingController();

  bool _loading = true;
  bool _loadingSlots = false;
  bool _submitting = false;
  String? _error;
  Map<String, dynamic>? _success;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _remark.dispose();
    super.dispose();
  }

  Map<String, dynamic>? get _item {
    for (final item in _items) {
      if (asInt(item['id']) == _itemId) return item;
    }
    return null;
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final results = await Future.wait([
        Api.get<List<dynamic>>('/api/customer/items'),
        Api.get<List<dynamic>>('/api/customer/stores'),
      ]);
      if (!mounted) return;
      final items = asMapList(results[0]);
      final stores = asMapList(results[1]);
      setState(() {
        _items = items;
        _stores = stores;
        _itemId = widget.initialItemId ?? (items.isNotEmpty ? asInt(items.first['id']) : null);
        _storeId = stores.isNotEmpty ? asInt(stores.first['id']) : null;
      });
      await _loadTechs();
      await _loadSlots();
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _loadTechs() async {
    if (_storeId == null) return;
    try {
      final list = await Api.get<List<dynamic>>('/api/customer/technicians', query: {
        'storeId': _storeId,
        if (_item?['bizType'] != null) 'bizType': _item!['bizType'],
      });
      if (mounted) setState(() => _techs = asMapList(list));
    } on ApiException {
      if (mounted) setState(() => _techs = []);
    }
  }

  Future<void> _loadSlots() async {
    if (_storeId == null) return;
    setState(() {
      _loadingSlots = true;
      _slot = null;
    });
    try {
      final list = await Api.get<List<dynamic>>('/api/customer/slots', query: {
        'storeId': _storeId,
        'date': _date,
        if (_itemId != null) 'itemId': _itemId,
        if (_techId != null) 'techId': _techId,
      });
      if (mounted) setState(() => _slots = asMapList(list));
    } on ApiException catch (e) {
      if (mounted) {
        setState(() => _slots = []);
        showToast(context, e.message);
      }
    } finally {
      if (mounted) setState(() => _loadingSlots = false);
    }
  }

  Future<void> _pickDate() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: DateTime.tryParse(_date) ?? now.add(const Duration(days: 1)),
      firstDate: now,
      lastDate: now.add(const Duration(days: 60)),
      helpText: '选择到店日期',
      cancelText: '取消',
      confirmText: '确定',
    );
    if (picked == null) return;
    final iso =
        '${picked.year.toString().padLeft(4, '0')}-${picked.month.toString().padLeft(2, '0')}-${picked.day.toString().padLeft(2, '0')}';
    setState(() => _date = iso);
    await _loadSlots();
  }

  Future<void> _submit() async {
    if (_slot == null) {
      showToast(context, '请选择预约时段');
      return;
    }
    setState(() => _submitting = true);
    try {
      final data = await Api.post<Map<String, dynamic>>('/api/customer/appointments', body: {
        'storeId': _storeId,
        'itemId': _itemId,
        'technicianId': asInt(_slot!['technicianId'], _techId ?? 0),
        'bizType': _item?['bizType'],
        'startTime': _slot!['time'],
        'remark': _remark.text.trim(),
      });
      if (!mounted) return;
      setState(() => _success = asMap(data));
    } on ApiException catch (e) {
      if (mounted) showToast(context, e.message);
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('在线预约')),
      body: _loading
          ? const LoadingView()
          : _error != null
              ? ErrorState(message: _error!, onRetry: _load)
              : _success != null
                  ? _buildSuccess()
                  : ListView(
                      padding: const EdgeInsets.fromLTRB(16, 8, 16, 110),
                      children: [
                        _buildItemSelector(),
                        const SizedBox(height: 14),
                        _buildStoreSelector(),
                        const SizedBox(height: 14),
                        _buildDateSelector(),
                        const SizedBox(height: 14),
                        _buildSlots(),
                        const SizedBox(height: 14),
                        AppCard(
                          child: Column(
                            children: [
                              const FieldLabel(text: '备注（可选）'),
                              TextField(
                                controller: _remark,
                                maxLines: 3,
                                maxLength: 120,
                                decoration: const InputDecoration(
                                  hintText: '如：希望安静的房间 / 对某成分过敏等',
                                  counterText: '',
                                ),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 12),
                        const Text(
                          '预约成功后可在「我的预约」查看；如需取消请提前 2 小时操作。',
                          style: TextStyle(fontSize: 11, color: AppColors.muted, height: 1.5),
                        ),
                      ],
                    ),
      bottomNavigationBar: _success != null || _loading || _error != null
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
                        mainAxisSize: MainAxisSize.min,
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(_slot == null ? '请选择时段' : '已选 ${dt(_slot!['time'])}', style: const TextStyle(fontSize: 11, color: AppColors.muted)),
                          PriceText(value: _item?['price'] ?? 0, size: 18),
                        ],
                      ),
                    ),
                    SizedBox(
                      width: 170,
                      child: FilledButton(
                        onPressed: _submitting || _slot == null ? null : _submit,
                        child: _submitting
                            ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                            : const Text('确认预约'),
                      ),
                    ),
                  ],
                ),
              ),
            ),
    );
  }

  Widget _buildItemSelector() {
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('选择项目', style: TextStyle(fontSize: 14, fontWeight: FontWeight.w700)),
          const SizedBox(height: 8),
          ..._items.map((item) {
            final selected = asInt(item['id']) == _itemId;
            return InkWell(
              onTap: () async {
                setState(() => _itemId = asInt(item['id']));
                await _loadTechs();
                await _loadSlots();
              },
              borderRadius: BorderRadius.circular(10),
              child: Container(
                margin: const EdgeInsets.only(bottom: 6),
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
                decoration: BoxDecoration(
                  color: selected ? AppColors.brand.withValues(alpha: 0.05) : Colors.transparent,
                  border: Border.all(color: selected ? AppColors.brand.withValues(alpha: 0.35) : AppColors.line),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Row(
                  children: [
                    Icon(
                      selected ? Icons.radio_button_checked : Icons.radio_button_unchecked,
                      size: 18,
                      color: selected ? AppColors.brand : AppColors.muted,
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(item['name']?.toString() ?? '', style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500)),
                          const SizedBox(height: 2),
                          Text(
                            '${bizOf(item['bizType'])} · ${item['duration'] ?? 60} 分钟',
                            style: const TextStyle(fontSize: 10.5, color: AppColors.muted),
                          ),
                        ],
                      ),
                    ),
                    PriceText(value: item['price'], size: 15),
                  ],
                ),
              ),
            );
          }),
        ],
      ),
    );
  }

  Widget _buildStoreSelector() {
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('选择门店', style: TextStyle(fontSize: 14, fontWeight: FontWeight.w700)),
          const SizedBox(height: 10),
          DropdownButtonFormField<int>(
            value: _storeId,
            isExpanded: true,
            decoration: const InputDecoration(prefixIcon: Icon(Icons.storefront_outlined, size: 19)),
            items: _stores
                .map((s) => DropdownMenuItem<int>(value: asInt(s['id']), child: Text(s['name']?.toString() ?? '', overflow: TextOverflow.ellipsis)))
                .toList(),
            onChanged: (v) async {
              setState(() {
                _storeId = v;
                _techId = null;
                _slot = null;
              });
              await _loadTechs();
              await _loadSlots();
            },
          ),
          const SizedBox(height: 10),
          DropdownButtonFormField<int?>(
            value: _techId,
            isExpanded: true,
            decoration: const InputDecoration(prefixIcon: Icon(Icons.spa_outlined, size: 19)),
            items: [
              const DropdownMenuItem<int?>(value: null, child: Text('不指定技师，由门店安排')),
              ..._techs.map((t) => DropdownMenuItem<int?>(
                    value: asInt(t['id']),
                    child: Text(
                      '${t['name'] ?? ''} · ${t['position'] ?? '技师'} · ★${t['rating'] ?? '-'}',
                      overflow: TextOverflow.ellipsis,
                    ),
                  )),
            ],
            onChanged: (v) async {
              setState(() => _techId = v);
              await _loadSlots();
            },
          ),
        ],
      ),
    );
  }

  Widget _buildDateSelector() {
    return AppCard(
      onTap: _pickDate,
      child: Row(
        children: [
          const Icon(Icons.calendar_month_outlined, size: 20, color: AppColors.brand),
          const SizedBox(width: 10),
          const Text('到店日期', style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600)),
          const Spacer(),
          Text('${relativeDate(_date)}  $_date', style: const TextStyle(fontSize: 12, color: AppColors.brand)),
          const Icon(Icons.chevron_right, size: 18, color: AppColors.muted),
        ],
      ),
    );
  }

  Widget _buildSlots() {
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Text('可选时段', style: TextStyle(fontSize: 14, fontWeight: FontWeight.w700)),
              const Spacer(),
              if (_loadingSlots)
                const SizedBox(width: 14, height: 14, child: CircularProgressIndicator(strokeWidth: 1.6, color: AppColors.brand)),
            ],
          ),
          const SizedBox(height: 10),
          if (!_loadingSlots && _slots.isEmpty)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 18),
              child: Center(
                child: Text('当日暂无可约时段，请更换日期或技师', style: TextStyle(fontSize: 12, color: AppColors.muted)),
              ),
            )
          else
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: _slots.map((slot) {
                final selected = _slot != null && _slot!['time'] == slot['time'] && _slot!['technicianId'] == slot['technicianId'];
                return GestureDetector(
                  onTap: () => setState(() => _slot = slot),
                  child: Container(
                    width: (MediaQuery.sizeOf(context).width - 80) / 2,
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 9),
                    decoration: BoxDecoration(
                      color: selected ? AppColors.brand : AppColors.bg,
                      borderRadius: BorderRadius.circular(10),
                      border: Border.all(color: selected ? AppColors.brand : AppColors.line),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          dt(slot['time']).substring(11, 16),
                          style: TextStyle(
                            fontSize: 13.5,
                            fontWeight: FontWeight.w600,
                            color: selected ? Colors.white : AppColors.text,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          slot['technicianName']?.toString() ?? '门店安排',
                          style: TextStyle(fontSize: 10, color: selected ? Colors.white70 : AppColors.muted),
                        ),
                      ],
                    ),
                  ),
                );
              }).toList(),
            ),
        ],
      ),
    );
  }

  Widget _buildSuccess() {
    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        const SizedBox(height: 30),
        AppCard(
          padding: const EdgeInsets.fromLTRB(20, 34, 20, 26),
          child: Column(
            children: [
              Container(
                width: 62,
                height: 62,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: AppColors.success.withValues(alpha: 0.10),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.check_rounded, color: AppColors.success, size: 34),
              ),
              const SizedBox(height: 16),
              const Text('预约成功', style: TextStyle(fontSize: 19, fontWeight: FontWeight.w700)),
              const SizedBox(height: 8),
              Text(
                '预约号 ${_success?['appointmentNo'] ?? ''}',
                style: const TextStyle(fontSize: 12, color: AppColors.muted),
              ),
              const SizedBox(height: 4),
              Text(
                '到店时间 ${dt(_success?['startTime'])}',
                style: const TextStyle(fontSize: 13, color: AppColors.text),
              ),
              const SizedBox(height: 20),
              FilledButton(
                onPressed: () {
                  Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => const AppointmentsPage()));
                },
                child: const Text('查看我的预约'),
              ),
              const SizedBox(height: 8),
              TextButton(
                onPressed: () {
                  setState(() {
                    _success = null;
                    _slot = null;
                  });
                },
                child: const Text('继续预约'),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        const Text(
          '温馨提示：请按预约时间提前 10 分钟到店；如需改约请致电门店或在线取消后重新预约。',
          textAlign: TextAlign.center,
          style: TextStyle(fontSize: 11, color: AppColors.muted, height: 1.6),
        ),
      ],
    );
  }
}
