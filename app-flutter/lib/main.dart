import 'package:flutter/material.dart';

import 'core/auth.dart';
import 'core/theme.dart';
import 'pages/login_page.dart';
import 'pages/main_shell.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await AuthStore.instance.init();
  runApp(const MeiyeApp());
}

class MeiyeApp extends StatelessWidget {
  const MeiyeApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '美业臻选',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      home: const _AuthGate(),
    );
  }
}

class _AuthGate extends StatelessWidget {
  const _AuthGate();

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: AuthStore.instance,
      builder: (context, _) {
        if (!AuthStore.instance.ready) {
          return const _SplashPage();
        }
        if (!AuthStore.instance.loggedIn) {
          return const LoginPage();
        }
        return const MainShell();
      },
    );
  }
}

class _SplashPage extends StatelessWidget {
  const _SplashPage();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFF8C1A45), Color(0xFFB4235A), Color(0xFFD46A92)],
          ),
        ),
        child: const Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text('美', style: TextStyle(color: Colors.white, fontSize: 58, fontWeight: FontWeight.w700)),
              SizedBox(height: 10),
              Text('美业臻选', style: TextStyle(color: Colors.white, fontSize: 22, letterSpacing: 4)),
              SizedBox(height: 26),
              SizedBox(
                width: 22,
                height: 22,
                child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white70),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
