import 'package:flutter/material.dart';

import '../core/format.dart';
import '../core/theme.dart';

class AppCard extends StatelessWidget {
  const AppCard({super.key, required this.child, this.padding, this.onTap, this.color});

  final Widget child;
  final EdgeInsetsGeometry? padding;
  final VoidCallback? onTap;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    final content = Container(
      padding: padding ?? const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: color ?? Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.line.withValues(alpha: 0.7)),
      ),
      child: child,
    );
    if (onTap == null) return content;
    return Material(
      color: Colors.transparent,
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: onTap,
        child: content,
      ),
    );
  }
}

class SectionHeader extends StatelessWidget {
  const SectionHeader({super.key, required this.title, this.action, this.onAction, this.subtitle});

  final String title;
  final String? subtitle;
  final String? action;
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(0, 4, 0, 10),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700)),
                if (subtitle != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 2),
                    child: Text(subtitle!, style: const TextStyle(fontSize: 12, color: AppColors.muted)),
                  ),
              ],
            ),
          ),
          if (action != null)
            GestureDetector(
              onTap: onAction,
              child: Text(action!, style: const TextStyle(fontSize: 12, color: AppColors.brand)),
            ),
        ],
      ),
    );
  }
}

class Pill extends StatelessWidget {
  const Pill({super.key, required this.text, this.color});

  final String text;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    final c = color ?? AppColors.brand;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: c.withValues(alpha: 0.09),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(text, style: TextStyle(fontSize: 11, color: c, fontWeight: FontWeight.w500)),
    );
  }
}

class StatusPill extends StatelessWidget {
  const StatusPill({super.key, required this.status});

  final String status;

  @override
  Widget build(BuildContext context) {
    final key = colorForStatus(status);
    final color = switch (key) {
      'success' => AppColors.success,
      'warning' => AppColors.warning,
      'danger' => AppColors.danger,
      'brand' => AppColors.brand,
      _ => AppColors.muted,
    };
    return Pill(text: labelOf(status), color: color);
  }
}

class PriceText extends StatelessWidget {
  const PriceText({super.key, required this.value, this.size = 18, this.color});

  final dynamic value;
  final double size;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    return Text(
      money(value),
      style: TextStyle(
        color: color ?? AppColors.brand,
        fontSize: size,
        fontWeight: FontWeight.w700,
        letterSpacing: -0.3,
      ),
    );
  }
}

class EmptyState extends StatelessWidget {
  const EmptyState({super.key, required this.text, this.action, this.onAction, this.icon = '🌿'});

  final String text;
  final String? action;
  final VoidCallback? onAction;
  final String icon;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 56, horizontal: 24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(icon, style: const TextStyle(fontSize: 40)),
            const SizedBox(height: 12),
            Text(text, textAlign: TextAlign.center, style: const TextStyle(color: AppColors.muted, fontSize: 13)),
            if (action != null) ...[
              const SizedBox(height: 14),
              OutlinedButton(
                onPressed: onAction,
                style: OutlinedButton.styleFrom(
                  minimumSize: const Size(140, 40),
                  padding: const EdgeInsets.symmetric(horizontal: 20),
                ),
                child: Text(action!),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class ErrorState extends StatelessWidget {
  const ErrorState({super.key, required this.message, this.onRetry});

  final String message;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    return EmptyState(
      icon: '⚠️',
      text: message,
      action: onRetry == null ? null : '重新加载',
      onAction: onRetry,
    );
  }
}

class LoadingView extends StatelessWidget {
  const LoadingView({super.key, this.text = '加载中…'});

  final String text;

  @override
  Widget build(BuildContext context) {
    return const Center(
      child: Padding(
        padding: EdgeInsets.symmetric(vertical: 48),
        child: CircularProgressIndicator(strokeWidth: 2, color: AppColors.brand),
      ),
    );
  }
}

class KVRow extends StatelessWidget {
  const KVRow({super.key, required this.label, required this.value, this.highlight = false});

  final String label;
  final String value;
  final bool highlight;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 84,
            child: Text(label, style: const TextStyle(fontSize: 13, color: AppColors.muted)),
          ),
          Expanded(
            child: Text(
              value,
              textAlign: TextAlign.right,
              style: TextStyle(
                fontSize: 13,
                color: highlight ? AppColors.brand : AppColors.text,
                fontWeight: highlight ? FontWeight.w600 : FontWeight.w400,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class FieldLabel extends StatelessWidget {
  const FieldLabel({super.key, required this.text, this.required = false});

  final String text;
  final bool required;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6, top: 12),
      child: Row(
        children: [
          Text(text, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600)),
          if (required)
            const Padding(
              padding: EdgeInsets.only(left: 2),
              child: Text('*', style: TextStyle(color: AppColors.danger, fontSize: 13)),
            ),
        ],
      ),
    );
  }
}

Future<bool> confirmDialog(
  BuildContext context, {
  required String title,
  required String content,
  String confirmText = '确定',
  bool danger = false,
}) async {
  final result = await showDialog<bool>(
    context: context,
    builder: (ctx) => AlertDialog(
      title: Text(title, style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w600)),
      content: Text(content, style: const TextStyle(fontSize: 14, height: 1.5)),
      actions: [
        TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('取消')),
        TextButton(
          onPressed: () => Navigator.pop(ctx, true),
          child: Text(confirmText, style: TextStyle(color: danger ? AppColors.danger : AppColors.brand)),
        ),
      ],
    ),
  );
  return result ?? false;
}

void showToast(BuildContext context, String text) {
  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(text)));
}

void showError(BuildContext context, Object error) {
  showToast(context, error is Exception ? error.toString() : error.toString());
}

class BrandAvatar extends StatelessWidget {
  const BrandAvatar({super.key, this.text, this.size = 48});

  final String? text;
  final double size;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        gradient: const LinearGradient(colors: [Color(0xFFD65C8C), AppColors.brand]),
        borderRadius: BorderRadius.circular(size / 2),
      ),
      child: Text(
        text == null || text!.isEmpty ? '美' : text!,
        style: TextStyle(color: Colors.white, fontSize: size * 0.4, fontWeight: FontWeight.w600),
      ),
    );
  }
}

class QuickAction extends StatelessWidget {
  const QuickAction({super.key, required this.icon, required this.label, required this.onTap});

  final String icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 8),
        child: Column(
          children: [
            Container(
              width: 44,
              height: 44,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: AppColors.brand.withValues(alpha: 0.07),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Text(icon, style: const TextStyle(fontSize: 21)),
            ),
            const SizedBox(height: 6),
            Text(label, style: const TextStyle(fontSize: 11.5, color: AppColors.text)),
          ],
        ),
      ),
    );
  }
}

class ListSkeleton extends StatelessWidget {
  const ListSkeleton({super.key, this.count = 3});

  final int count;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: List.generate(
        count,
        (i) => Container(
          height: 82,
          margin: const EdgeInsets.only(bottom: 10),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(14),
          ),
          child: const Center(
            child: SizedBox(
              width: 18,
              height: 18,
              child: CircularProgressIndicator(strokeWidth: 1.6, color: AppColors.line),
            ),
          ),
        ),
      ),
    );
  }
}
