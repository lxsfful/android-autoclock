# android-autoclock

Android/Kotlin 应用，基于 AccessibilityService 实现自动化打卡。调度手势序列，检测目标应用成功消息，记录历史，可选邮件通知。

## Ethical and authorized use

仅在你有明确权限的设备、账户和应用上使用本项目。不要用于规避规则、伪造用户操作、绕过控制，或自动化任何禁止自动化的服务。你有责任遵守适用法律、应用条款、组织政策和 Android AccessibilityService 披露要求。

## Features

- Kotlin Android 应用，使用 Gradle 和 AndroidX
- AccessibilityService 手势自动化，用户可选屏幕坐标
- 精确闹钟调度，支持两个可配置的时间窗口
- 目标应用成功检测（可配置包名和成功文本）
- 本地运行历史记录存储
- 可选 SMTP 邮件通知（成功、失败、测试）
- Android Keystore 加密存储 SMTP 密码
- 打卡完成后自动杀死目标应用，防止弹窗残留
- 3秒延迟后返回桌面并打开向日葵，便于远程测试
- 清新优雅的蓝绿色 UI 主题

## Quick start

```bash
git clone https://github.com/lxsfful/android-autoclock.git
cd android-autoclock
./setup.sh
```

`setup.sh` 验证 Java 并创建 `local.properties`。如果创建了占位符，请在运行 Gradle 前编辑它：

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## Prerequisites

- Android Studio 或 Android SDK 命令行工具
- JDK 17 或更高版本，设置 `JAVA_HOME`
- Android SDK Platform 34
- `local.properties` 中的本地 Android SDK 路径：

```properties
sdk.dir=C:/Users/yourname/AppData/Local/Android/Sdk
```

## Configuration

在 `ClockSuccessDetector.kt` 和 `AutoClockService.kt` 中配置：

| 配置项 | 位置 | 说明 |
| --- | --- | --- |
| `TARGET_APP_PACKAGE` | `AutoClockService.kt` | 目标应用包名（如云之家） |
| `CLOCK_APP_PACKAGE` | `ClockSuccessDetector.kt` | 目标应用包名（需保持一致） |
| `SUCCESS_TEXT` | `ClockSuccessDetector.kt` | 成功检测文本 |
| `SUNFLOWER_PACKAGE` | `AutoClockService.kt` | 向日葵包名（可选） |
| SMTP 配置 | 应用内设置 | 可选邮件通知 |

## Build and test

```bash
./gradlew testDebugUnitTest      # JVM 单元测试
./gradlew assembleDebug          # debug APK（自动签名）
./gradlew assembleRelease        # release APK（未签名）
./gradlew installDebug           # 构建并安装到设备
./gradlew lintDebug              # Android lint
./gradlew clean                  # 清除构建输出
```

## Architecture

```text
app/src/main/java/com/autoclock/
├── MainActivity.kt          # 主界面、权限、设置、历史、邮件测试
├── AutoClockService.kt      # 无障碍服务，手势序列和目标文本检测
├── AlarmScheduler.kt        # 精确闹钟调度
├── AlarmReceiver.kt         # 闹钟触发
├── BootReceiver.kt          # 开机重新调度
├── ClockSuccessDetector.kt  # 目标包名和成功文本匹配
├── Prefs.kt                 # 应用设置和加密存储
├── CoordinateMapper.kt      # 坐标比例映射
├── ClockHistory.kt          # 本地历史记录
├── EmailSender.kt           # 可选 SMTP 通知
└── UsageGuideActivity.kt    # 使用说明
```

**数据流**：`MainActivity` 保存配置 → `AlarmScheduler` 调度闹钟 → `AlarmReceiver` 触发 → `AutoClockService` 执行手势 → `ClockSuccessDetector` 检测成功 → 3秒后返回桌面 → 杀死目标应用 → 打开向日葵 → 可选邮件通知。

## Accessibility and privacy

本应用使用 Android AccessibilityService 功能。用户启用服务后，它可以：

- 执行配置的屏幕手势，包括点击和全局 Home 动作
- 读取目标包名的文本和内容描述，检测成功消息
- 在设备唤醒时运行自动化流程
- 使用 `WAKE_LOCK` 和精确闹钟触发序列
- 成功后强制停止目标应用，防止弹窗残留
- 3秒后返回桌面并打开向日葵

隐私边界：

- AccessibilityEvent 过滤仅限于配置的目标包名
- 仅在前台包名匹配目标包名时读取文本
- 目标应用文本仅用于成功检测，不持久化存储
- 坐标、时间窗口、历史和邮件设置存储在应用私有偏好中
- SMTP 密码使用 Android Keystore 加密存储
- 仅在用户配置的 SMTP 和收件人设置下发送邮件

## Security notes

- 应用请求强大权限：AccessibilityService、精确闹钟、唤醒锁、开机启动、网络（可选邮件）
- 保持 `android:packageNames` 限定在目标包名，避免广泛的无障碍访问
- 不要记录或提交密钥。`.gitignore` 排除 `.env`、`local.properties` 和构建输出
- 发布前审查导出组件
- 报告漏洞请参见 `SECURITY.md`

## License

MIT. See `LICENSE`.

## Contributing

See `CONTRIBUTING.md`.
