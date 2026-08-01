# 悬浮球 — Flyme 风格导航球

[English](README.md)

受魅族 Flyme OS 启发，为 **小米 HyperOS 3 (Android 16)** 打造的悬浮导航球。通过手势实现返回、桌面、单手模式等快捷操作。

> ⚠️ **仅在 HyperOS 3 (OS3.0.301.0.WPMEUXM, Android 16, myron) 上测试。** 详见 [KNOWN_ISSUES.md](KNOWN_ISSUES.md)。

## 功能

| 手势 | 默认动作 |
|---------|---------|
| 单击 | 返回 |
| 双击 | 切换单手模式 |
| 上滑 | 桌面（同时退出单手模式） |
| 下滑 | 通知栏 |
| 左滑 | 控制中心 |
| 右滑 | 最近应用 |
| 长按拖动 | 移动 |

- 单手模式 + 球位置自动补偿 + 外部退出检测
- Flyme 风格内圈滑动动效
- 横屏自动隐藏
- 所有外观和手势可配置
- 前台服务 + 健康检查保活

## 安装

```bash
adb install -r -t app-debug.apk
adb shell pm grant com.floatingball android.permission.WRITE_SECURE_SETTINGS
adb shell dumpsys deviceidle whitelist +com.floatingball
```

然后开启无障碍服务、悬浮窗权限、自启动，电池设为"无限制"。

## 已知问题

详见 [KNOWN_ISSUES.md](KNOWN_ISSUES.md)。

## 免责声明

本项目仅供**学习研究**，研究 Android 无障碍服务、窗口管理和系统集成。复刻 Flyme 视觉设计仅出于学术目的。

- **与魅族、小米无关**
- **禁止商业用途**
- **使用风险自负**

## 开源协议

MIT
