/// 全局配置。
///
/// 编译/打包时通过 --dart-define 覆盖：
/// flutter build apk --dart-define=API_BASE_URL=https://api.example.com \
///                    --dart-define=TENANT_CODE=5MY
class AppConfig {
  AppConfig._();

  static const String appName = '美业臻选';
  static const String version = '1.0.0';

  /// Android 模拟器访问宿主机使用 10.0.2.2；真机请传入局域网/线上 HTTPS 域名。
  static const String apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8081',
  );

  static const String defaultTenant = String.fromEnvironment(
    'TENANT_CODE',
    defaultValue: '5MY',
  );

  static const Duration requestTimeout = Duration(seconds: 20);

  static String get normalizedBaseUrl {
    var url = apiBaseUrl.trim();
    while (url.endsWith('/')) {
      url = url.substring(0, url.length - 1);
    }
    return url;
  }
}
