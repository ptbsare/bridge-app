# Bridge 跳板

[English](#english) | [中文](#中文)

> A tiny Android "launcher bridge" app. When opened, it instantly brings a **configurable foreground Activity** (e.g. your backup app's home screen) to the front, then exits itself. Works with **system apps** too.

> 一个极小的 Android「跳板」应用。打开后立即把**可配置的前台 Activity**（例如备份应用主页）拉到前台，然后自动退出。**系统应用也可以**。

---

## ✨ Features / 特性

- 🚀 **Tap icon → auto-launch target** — open the app, it jumps to your target Activity, then quits itself. Perfect for scheduled tasks / automations that can only open regular apps.
- ⚙️ **Long-press icon → settings** — scan **every launchable Activity on your phone**, including **hidden/system ones**, pick from a searchable list. No need to type package names by hand.
- 🎯 **Default target** — `com.miui.backup/.local.LocalHomeActivity` (Xiaomi/Redmi local backup home), so it works out of the box on MIUI phones.
- 🔧 **adb-friendly** — pass a target via intent extra for scripted/one-off launches without changing your saved setting.
- 📦 **CI-built** — GitHub Actions builds a fresh APK on every push; grab it from the Release page.

---

## 🇨🇳 中文

### 为什么需要它

红米/小米手机（Android 14/15，targetSdk 35）上，**系统应用的后台自动任务经常被「后台启动前台服务限制」拦截**（`startForegroundService not allowed` / `FGS DENIED`）。本地备份、系统应用刷新等定时任务即使到点触发，也因为 App 不处于前台而失败。

解决办法：让一个**第三方 App** 作为跳板 —— 你的定时任务打开跳板 → 跳板立刻把备份等目标 App 拉到前台 → 目标 App 进入前台后即可正常完成工作（也绕过了「定时服务不能直接打开系统应用」的限制）。

### 安装

1. 从 **Releases** 下载最新 APK：[bridge-app Releases](https://github.com/ptbsare/bridge-app/releases)
2. 手机安装（需允许「安装未知来源应用」）

### 使用

| 操作 | 行为 |
|------|------|
| 短按图标（或用任意方式启动） | 自动拉起「当前目标」前台 Activity，随后自身退出 |
| 长按图标 | 弹出「设置」→ 进入设置界面 |

### 设置界面

- 自动扫描手机**全部可启动 Activity**（含系统应用、未导出但可访问的公开 Activity）
- **搜索框**按应用名 / 组件名实时过滤
- 点击条目 → **保存为默认目标** 并立即测试拉起
- 顶部显示当前目标；蓝色高亮 = 已保存的目标

### adb / 自动化用法

```bash
# 用保存的目标启动
adb shell am start -n com.hermes.bridge/.MainActivity

# 临时指定目标（不改变已保存的配置）
adb shell am start -n com.hermes.bridge/.MainActivity \
    --es target "com.miui.backup/.local.LocalHomeActivity"

# 直接打开设置界面（可通过配对后的无线 adb 调用）
adb shell am start -n com.hermes.bridge/.SettingsActivity
```

### 配置存储

| 项 | 详情 |
|----|------|
| SharedPreferences | `bridge`（私有） |
| 键 | `target` |
| 默认值 | `com.miui.backup/.local.LocalHomeActivity` |
| 组件格式 | `包名/完整类别名` 或 `包名/.缩写类别名`（如 `com.miui.backup/.local.LocalHomeActivity`） |

---

## English

### Why this app exists

On Xiaomi/Redmi phones (Android 14/15, targetSdk 35), **background auto-tasks of system apps are often blocked by the "background start foreground service" restriction** (`startForegroundService not allowed` / `FGS DENIED`). Scheduled jobs such as local backup fire at their alarm time but fail because the app isn't in the foreground.

The workaround: use a **third-party app as a bridge** — your scheduler opens the bridge → the bridge instantly brings the target app (e.g. backup) to the foreground → the target app can then do its job normally. This also sidesteps the common limitation that "scheduler can't directly open system apps".

### Install

1. Grab the latest APK from **Releases**: [bridge-app Releases](https://github.com/ptbsare/bridge-app/releases)
2. Install on your phone (allow "install unknown apps" if prompted).

### Usage

| Action | Behavior |
|--------|----------|
| Tap icon (or launch any way) | Brings the "current target" Activity to the foreground, then the bridge exits itself |
| Long-press icon | Shows "Settings" shortcut → opens the settings screen |

### Settings screen

- Scans **every launchable Activity on the device**, including **system apps** and public components.
- **Search box** filters live by app label / component name.
- Tap an entry → **saved as default target** and immediately test-launched.
- Current target shown at the top; the saved one is highlighted in blue.

### adb / automation

```bash
# Launch with the saved target
adb shell am start -n com.hermes.bridge/.MainActivity

# Launch with a temporary target (does NOT overwrite the saved one)
adb shell am start -n com.hermes.bridge/.MainActivity \
    --es target "com.miui.backup/.local.LocalHomeActivity"

# Open the settings screen directly (works over wireless adb after pairing)
adb shell am start -n com.hermes.bridge/.SettingsActivity
```

### Configuration storage

| Item | Details |
|------|---------|
| SharedPreferences | `bridge` (private) |
| Key | `target` |
| Default | `com.miui.backup/.local.LocalHomeActivity` |
| Component format | `package/fully.qualified.Class` or `package/.abbreviated.Class` (e.g. `com.miui.backup/.local.LocalHomeActivity`) |

---

## 🛠 Build from source / 本地构建

```
gradle assembleDebug        # APK → app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17 + Android SDK (compileSdk 34, minSdk 26).

构建需要 JDK 17 + Android SDK（compileSdk 34，minSdk 26）。

## ⚠️ Notes / 备注

- The bridge does **not** start hidden/private (non-exported) components that require the app's own signature/permissions — use an Activity that is actually launchable.
- If the target Activity asks for a lock-screen password (MIUI privacy protection), that's normal behavior of the target app, not the bridge.
- 跳板只能拉起**真实可启动**（exported）的组件；若目标 Activity 弹出锁屏密码确认（MIUI 隐私保护），属于目标应用自身行为，与跳板无关。

## 📄 License

MIT