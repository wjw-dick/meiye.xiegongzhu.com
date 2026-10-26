import 'package:flutter/material.dart';

import '../core/theme.dart';

class CoverImage extends StatelessWidget {
  const CoverImage({
    super.key,
    required this.url,
    this.width = 80,
    this.height = 80,
    this.radius = 12,
    this.placeholder = '美',
  });

  final String? url;
  final double width;
  final double height;
  final double radius;
  final String placeholder;

  @override
  Widget build(BuildContext context) {
    final src = url?.trim() ?? '';
    if (src.isEmpty) return _placeholderBox();
    return ClipRRect(
      borderRadius: BorderRadius.circular(radius),
      child: Image.network(
        src,
        width: width,
        height: height,
        fit: BoxFit.cover,
        errorBuilder: (_, __, ___) => _placeholderBox(),
        loadingBuilder: (context, child, progress) {
          if (progress == null) return child;
          return Container(
            width: width,
            height: height,
            color: AppColors.brand.withValues(alpha: 0.05),
            child: const Center(
              child: SizedBox(
                width: 14,
                height: 14,
                child: CircularProgressIndicator(strokeWidth: 1.4, color: AppColors.brand),
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _placeholderBox() {
    return Container(
      width: width,
      height: height,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [
            AppColors.brand.withValues(alpha: 0.06),
            AppColors.gold.withValues(alpha: 0.14),
          ],
        ),
        borderRadius: BorderRadius.circular(radius),
      ),
      child: Text(
        placeholder,
        style: TextStyle(
          fontSize: height * 0.32,
          color: AppColors.brand.withValues(alpha: 0.45),
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}
