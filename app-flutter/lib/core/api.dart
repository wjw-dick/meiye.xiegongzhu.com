import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:http/http.dart' as http;

import 'auth.dart';
import 'config.dart';

class ApiException implements Exception {
  ApiException(this.message, {this.status = 0});

  final String message;
  final int status;

  @override
  String toString() => message;
}

/// 后端统一响应：{ code: 200, msg: 'success', data: ... }
class Api {
  Api._();

  static final http.Client _client = http.Client();

  static Future<T> get<T>(String path, {Map<String, dynamic>? query}) {
    return _request<T>('GET', path, query: query);
  }

  static Future<T> post<T>(String path, {Object? body, Map<String, dynamic>? query}) {
    return _request<T>('POST', path, body: body, query: query);
  }

  static Future<T> put<T>(String path, {Object? body}) {
    return _request<T>('PUT', path, body: body);
  }

  static Future<T> delete<T>(String path) {
    return _request<T>('DELETE', path);
  }

  static Uri _uri(String path, Map<String, dynamic>? query) {
    final qp = <String, String>{};
    query?.forEach((key, value) {
      if (value == null) return;
      if (value is String && value.isEmpty) return;
      qp[key] = value.toString();
    });
    final base = Uri.parse('${AppConfig.normalizedBaseUrl}$path');
    return base.replace(queryParameters: qp.isEmpty ? base.queryParameters : qp);
  }

  static Future<T> _request<T>(
    String method,
    String path, {
    Object? body,
    Map<String, dynamic>? query,
  }) async {
    final headers = <String, String>{
      'Accept': 'application/json',
      'Content-Type': 'application/json; charset=utf-8',
    };
    final token = AuthStore.instance.token;
    if (token.isNotEmpty) headers['Authorization'] = 'Bearer $token';

    try {
      late http.Response res;
      final uri = _uri(path, query);
      switch (method) {
        case 'POST':
          res = await _client
              .post(uri, headers: headers, body: body == null ? null : jsonEncode(body))
              .timeout(AppConfig.requestTimeout);
          break;
        case 'PUT':
          res = await _client
              .put(uri, headers: headers, body: body == null ? null : jsonEncode(body))
              .timeout(AppConfig.requestTimeout);
          break;
        case 'DELETE':
          res = await _client.delete(uri, headers: headers).timeout(AppConfig.requestTimeout);
          break;
        default:
          res = await _client.get(uri, headers: headers).timeout(AppConfig.requestTimeout);
      }
      return _parse<T>(res);
    } on TimeoutException {
      throw ApiException('请求超时，请检查网络后重试');
    } on SocketException {
      throw ApiException('网络不可用，请检查网络连接');
    } on http.ClientException {
      throw ApiException('网络异常，请稍后重试');
    } on FormatException {
      throw ApiException('服务器返回数据格式异常');
    }
  }

  static T _parse<T>(http.Response res) {
    dynamic body;
    try {
      body = jsonDecode(utf8.decode(res.bodyBytes));
    } catch (_) {
      throw ApiException('服务器响应异常（${res.statusCode}）', status: res.statusCode);
    }
    final map = body is Map<String, dynamic> ? body : <String, dynamic>{};
    final code = map['code'] is int ? map['code'] as int : res.statusCode;
    if (res.statusCode == 401 || code == 401) {
      // 让根组件切换回登录页
      unawaited(AuthStore.instance.logout());
      throw ApiException(map['msg']?.toString() ?? '登录已过期，请重新登录', status: 401);
    }
    if (res.statusCode < 200 || res.statusCode >= 300 || code != 200) {
      throw ApiException(map['msg']?.toString() ?? '请求失败（${res.statusCode}）', status: code);
    }
    return map['data'] as T;
  }
}
