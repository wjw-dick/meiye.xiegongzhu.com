import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'config.dart';

class AuthUser {
  AuthUser({
    this.customerId,
    this.tenantId,
    this.tenantCode,
    this.name,
    this.username,
    this.userType,
    this.role,
    this.storeId,
    this.avatar,
  });

  final int? customerId;
  final int? tenantId;
  final String? tenantCode;
  final String? name;
  final String? username;
  final String? userType;
  final String? role;
  final int? storeId;
  final String? avatar;

  factory AuthUser.fromJson(Map<String, dynamic> json) {
    return AuthUser(
      customerId: _int(json['customerId']),
      tenantId: _int(json['tenantId']),
      tenantCode: json['tenantCode']?.toString(),
      name: json['name']?.toString(),
      username: json['username']?.toString(),
      userType: json['userType']?.toString(),
      role: json['role']?.toString(),
      storeId: _int(json['storeId']),
      avatar: json['avatar']?.toString(),
    );
  }

  Map<String, dynamic> toJson() => {
        'customerId': customerId,
        'tenantId': tenantId,
        'tenantCode': tenantCode,
        'name': name,
        'username': username,
        'userType': userType,
        'role': role,
        'storeId': storeId,
        'avatar': avatar,
      };

  static int? _int(dynamic v) {
    if (v == null) return null;
    if (v is int) return v;
    return int.tryParse(v.toString());
  }
}

/// 登录态单例：根组件监听变化，登录/退出自动切换页面。
class AuthStore extends ChangeNotifier {
  AuthStore._();

  static final AuthStore instance = AuthStore._();

  static const _keyToken = 'meiye_token';
  static const _keyUser = 'meiye_user';
  static const _keyTenant = 'meiye_tenant';

  String token = '';
  String tenantCode = AppConfig.defaultTenant;
  AuthUser? user;
  bool ready = false;

  bool get loggedIn => token.isNotEmpty && user != null;

  Future<void> init() async {
    final prefs = await SharedPreferences.getInstance();
    token = prefs.getString(_keyToken) ?? '';
    tenantCode = prefs.getString(_keyTenant) ?? AppConfig.defaultTenant;
    final raw = prefs.getString(_keyUser);
    if (raw != null && raw.isNotEmpty) {
      try {
        user = AuthUser.fromJson(jsonDecode(raw) as Map<String, dynamic>);
      } catch (_) {
        user = null;
        token = '';
      }
    }
    if (user == null) token = '';
    ready = true;
    notifyListeners();
  }

  Future<void> save({
    required String token,
    required String tenantCode,
    required AuthUser user,
  }) async {
    this.token = token;
    this.tenantCode = tenantCode;
    this.user = user;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_keyToken, token);
    await prefs.setString(_keyTenant, tenantCode);
    await prefs.setString(_keyUser, jsonEncode(user.toJson()));
    notifyListeners();
  }

  Future<void> logout() async {
    token = '';
    user = null;
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_keyToken);
    await prefs.remove(_keyUser);
    notifyListeners();
  }
}
