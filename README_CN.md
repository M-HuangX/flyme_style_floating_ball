# 悬浮球 — Flyme 风格导航球

[English](README.md)

> # ⚠️ 免责声明 — 请务必阅读 ⚠️
>
> **本项目仅供学习和研究目的使用。**
>
> 复制魅族 Flyme OS 悬浮球的视觉设计和交互模式，**仅用于研究** Android 无障碍服务、窗口管理和系统集成技术。
>
> - **与魅族科技有限公司或小米公司无任何关联。**
> - **严禁商业用途。**
> - **使用风险自负。** 作者不提供任何明示或暗示的担保。作者对因使用本软件造成的任何损害、数据丢失、系统不稳定或法律后果不承担任何责任。
> - 本项目**不含任何**来自魅族、小米或任何第三方的专有代码。所有代码均为学术研究的原创作品。
>
> **使用本软件即表示您已阅读并理解本免责声明。**

---

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
- 前台服务 + 15s 健康检查保活

## 安装

```bash
adb install -r -t app-debug.apk
adb shell pm grant com.floatingball android.permission.WRITE_SECURE_SETTINGS
adb shell dumpsys deviceidle whitelist +com.floatingball
adb shell cmd appops set com.floatingball RUN_IN_BACKGROUND allow
adb shell cmd appops set com.floatingball RUN_ANY_IN_BACKGROUND allow
```

然后开启无障碍服务、悬浮窗权限、自启动，电池设为"无限制"。

## 已知问题

详见 [KNOWN_ISSUES.md](KNOWN_ISSUES.md)。

## 开源协议

MIT
