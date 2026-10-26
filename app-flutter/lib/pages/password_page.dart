import 'package:flutter/material.dart';

import '../core/api.dart';
import '../core/theme.dart';
import '../widgets/common.dart';

class PasswordPage extends StatefulWidget {
  const PasswordPage({super.key});

  @override
  State<PasswordPage> createState() => _PasswordPageState();
}

class _PasswordPageState extends State<PasswordPage> {
  final _formKey = GlobalKey<FormState>();
  final _old = TextEditingController();
  final _new = TextEditingController();
  final _confirm = TextEditingController();
  bool _obscure = true;
  bool _loading = false;

  @override
  void dispose() {
    _old.dispose();
    _new.dispose();
    _confirm.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _loading = true);
    try {
      await Api.post<dynamic>('/api/auth/password', body: {
        'oldPassword': _old.text,
        'newPassword': _new.text,
      });
      if (!mounted) return;
      showToast(context, '密码修改成功');
      Navigator.pop(context);
    } on ApiException catch (e) {
      if (mounted) showToast(context, e.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('修改密码')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          AppCard(
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('安全提示', style: TextStyle(fontSize: 14, fontWeight: FontWeight.w700)),
                  const SizedBox(height: 6),
                  const Text(
                    '建议使用 8 位以上、包含字母与数字的密码，并定期更换。修改成功后当前登录保持有效。',
                    style: TextStyle(fontSize: 11.5, color: AppColors.muted, height: 1.55),
                  ),
                  const FieldLabel(text: '原密码', required: true),
                  TextFormField(
                    controller: _old,
                    obscureText: _obscure,
                    decoration: InputDecoration(
                      hintText: '请输入原密码',
                      suffixIcon: IconButton(
                        onPressed: () => setState(() => _obscure = !_obscure),
                        icon: Icon(_obscure ? Icons.visibility_off_outlined : Icons.visibility_outlined, size: 19),
                      ),
                    ),
                    validator: (v) => (v == null || v.isEmpty) ? '请输入原密码' : null,
                  ),
                  const FieldLabel(text: '新密码', required: true),
                  TextFormField(
                    controller: _new,
                    obscureText: _obscure,
                    decoration: const InputDecoration(hintText: '至少 6 位'),
                    validator: (v) => (v == null || v.length < 6) ? '新密码至少 6 位' : null,
                  ),
                  const FieldLabel(text: '确认新密码', required: true),
                  TextFormField(
                    controller: _confirm,
                    obscureText: _obscure,
                    decoration: const InputDecoration(hintText: '再次输入新密码'),
                    validator: (v) => v != _new.text ? '两次输入不一致' : null,
                  ),
                  const SizedBox(height: 20),
                  FilledButton(
                    onPressed: _loading ? null : _submit,
                    child: _loading
                        ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                        : const Text('确认修改'),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
