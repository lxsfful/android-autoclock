# android-autoclock

Android/Kotlin 应用，基于 AccessibilityService 实现自动化打卡。调度手势序列，在目标应用窗口内检测云之家响应窗口或明确成功文本，记录历史，可选邮件通知。

## Ethical and authorized use

仅在你有明确权限的设备、账户和应用上使用本项目。不要用于规避规则、伪造用户操作、绕过控制，或自动化任何禁止自动化的服务。你有责任遵守适用法律、应用条款、组织政策和 Android AccessibilityService 披露要求。

## Features

- Kotlin Android 应用，使用 Gradle 和 AndroidX
- AccessibilityService 手势自动化，用户可选屏幕坐标
- 精确闹钟调度，支持两个可配置的时间窗口
- 分钟+秒数完全随机化，避免规律性
- 仅在云之家范围内监听响应窗口事件并读取窗口文本/内容描述，用于检测快捷打卡响应
- 本地运行历史记录存储
- 可选 SMTP 邮件通知（成功、失败、测试）
- Android Keystore 加密存储 SMTP 密码
- 打卡完成后自动请求结束云之家后台进程进程，防止弹窗残留
- 3秒延迟后返回桌面并优先通过包名打开向日葵，便于远程测试
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

在 `AutoClockService.kt` 中配置：

| 配置项 | 位置 | 说明 |
| --- | --- | --- |
| `TargetApps.CLOCK_PACKAGE` | `TargetApps.kt` | 云之家包名，供前台检查和成功检测共用 |
| `TargetApps.SMART_ATTEND_ACTIVITY` | `TargetApps.kt` | 云之家快捷打卡界面 Activity，仅用于诊断日志，不直接启动 |
| `android:packageNames` | `app/src/main/res/xml/accessibility_service_config.xml` | 无障碍服务监听/读取的目标包名，应与 `TargetApps.CLOCK_PACKAGE` 一致 |
| `TargetApps.SUNFLOWER_PACKAGE` | `TargetApps.kt` | 向日葵包名，后置流程优先通过它启动向日葵 |
| SMTP 配置 | 应用内设置 | 可选邮件通知 |

修改无障碍服务 XML 后，请重新安装应用，或在系统设置里关闭再重新开启 AutoClock 无障碍服务，确保 Android 重新加载 service metadata。

## 快捷打卡响应检测策略

等待云之家快捷打卡响应期间，AutoClock 使用以下优先级判定结果：

1. 先读取目标窗口文本/内容描述，出现“操作成功”“打卡成功”“已打卡”“已签到”等明确成功文案时记录成功。
2. 若出现“失败”“不成功”“请稍后重试”等明确失败文案，则记录失败。
3. 若没有明确成功/失败文案，来自 `TargetApps.CLOCK_PACKAGE` 的弹窗类（Dialog/Popup/Toast）`TYPE_WINDOW_STATE_CHANGED` 事件视为打卡响应弹窗出现，记录成功；心灵鸡汤类响应弹窗也按成功处理。

这个策略避免响应弹窗已出现但因文案不固定而被误判为超时，同时避免普通云之家 Activity 跳转或历史失败文案误判当前任务。

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
├── AutoClockService.kt      # 无障碍服务，手势序列、唤醒和前台诊断
├── AlarmScheduler.kt        # 精确闹钟调度
├── AlarmReceiver.kt         # 闹钟触发
├── BootReceiver.kt          # 开机重新调度
├── ClockSuccessDetector.kt  # 基于目标窗口事件和文本/描述检测快捷打卡响应
├── Prefs.kt                 # 应用设置和加密存储
├── CoordinateMapper.kt      # 坐标比例映射
├── ClockHistory.kt          # 本地历史记录
├── EmailSender.kt           # 可选 SMTP 通知
└── UsageGuideActivity.kt    # 使用说明
```

**数据流**：`MainActivity` 保存配置 → `AlarmScheduler` 随机精确闹钟调度 → `AlarmReceiver` 触发 → `AutoClockService` 唤醒、回桌面并点击桌面“云之家快捷打卡” → 等待快捷打卡界面并执行界面内“打卡”保险点击 → `ClockSuccessDetector` 在云之家窗口内检测响应窗口事件或明确成功/失败文本 → 3秒后返回桌面 → 请求结束云之家后台进程 → 通过包名打开向日葵（失败时坐标兜底） → 可选邮件通知。

## Accessibility and privacy

本应用使用 Android AccessibilityService 功能。用户启用服务后，它可以：

- 执行配置的屏幕手势，包括点击和全局 Home 动作
- 监听云之家包名的窗口变化事件作为主响应信号，并读取目标窗口文本/内容描述作为成功/失败兜底检测
- 通过精确闹钟和唤醒锁点亮屏幕后运行自动化流程（不能绕过安全锁屏）
- 使用 `WAKE_LOCK` 和精确闹钟触发序列
- 成功后请求结束云之家后台进程，防止弹窗残留
- 3秒后返回桌面并优先通过包名打开向日葵

隐私边界：

- AccessibilityEvent 和窗口内容读取仅限于配置的目标包名
- 前台确认阶段只记录粗粒度包名类别（target/launcher/system/other/unreadable），不保存其他 App 的完整包名
- 仅为判断快捷打卡响应读取目标窗口文本/内容描述，不存储完整 UI 文本
- 坐标、时间窗口、历史原因和邮件设置存储在应用私有偏好中
- SMTP 密码使用 Android Keystore 加密存储
- 仅在用户配置的 SMTP 和收件人设置下发送邮件

## Troubleshooting

### 历史记录显示“未确认云之家前台”或包含“观测包”诊断

AutoClock 已经点击桌面“云之家快捷打卡”，但在重试窗口内没有稳定确认云之家前台。当前版本不会因此立刻中断；为避免误触其他界面，会跳过界面内“打卡”保险点击并继续等待快捷打卡响应。如果最终仍失败，请根据历史原因中的观测包类别排查：

- 观测包一直是桌面 Launcher：检查“云之家快捷打卡”坐标和桌面页是否正确
- 观测包为 `null` 或无观测记录：检查无障碍服务是否重新开启生效
- 观测包出现其他 App：检查触发时是否有弹窗、远程界面或系统界面遮挡
- `TargetApps.CLOCK_PACKAGE` 与 `accessibility_service_config.xml` 的 `android:packageNames` 是否一致
- 等待秒数是否足够云之家快捷打卡界面启动
- 设备是否仍处于锁屏状态；WakeLock 可以点亮屏幕，但不能解锁安全锁屏

### 历史记录显示“无障碍服务未运行”

闹钟已触发，但系统中的 AutoClock AccessibilityService 不在运行。请重新开启无障碍服务，并将应用加入电池优化白名单。

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
