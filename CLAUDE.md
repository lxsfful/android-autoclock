# android-autoclock

**Version:** 1.3 | **Platform:** Android | **Stack:** Kotlin, AndroidX, Gradle 8.4

## What

Android AccessibilityService 自动化打卡应用。调度手势序列，在目标应用窗口内检测云之家响应弹窗或明确成功文本，记录历史，可选邮件通知。

## Commands

```bash
./gradlew assembleDebug    # 构建 debug APK
./gradlew installDebug     # 构建并安装到设备
./gradlew testDebugUnitTest # 运行单元测试
./gradlew lintDebug        # 代码检查
```

Windows 上若 `JAVA_HOME` 未设置，使用 `gradlew.bat` 并显式导出：

```powershell
$env:JAVA_HOME = "C:\Users\LX\.jdks\jbr-17.0.14"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

## Architecture

```text
app/src/main/java/com/autoclock/
├── MainActivity.kt              # 主界面、权限、设置、历史、邮件
├── AutoClockService.kt          # 无障碍服务，执行手势、唤醒和前台诊断
├── AlarmScheduler.kt            # 精确闹钟调度（分钟+秒数随机化）
├── AlarmReceiver.kt             # 闹钟触发并重新调度
├── BootReceiver.kt              # 开机后重新调度
├── ClockSuccessDetector.kt      # 基于窗口事件和文本/描述检测快捷打卡响应
├── ForegroundAppChecker.kt      # 目标 App 前台判断与诊断
├── ClockHistory.kt              # 本地历史记录
├── EmailSender.kt               # 可选 SMTP 通知
├── Prefs.kt                     # SharedPreferences 配置
├── CoordinatePickerActivity.kt  # 通过截图点选坐标
├── CoordinateMapper.kt          # 坐标比例映射
├── TargetApps.kt                # 目标 App 包名集中配置
└── UsageGuideActivity.kt        # 使用说明页
```

## 快捷打卡响应检测策略

等待云之家快捷打卡响应期间，AutoClock 使用以下优先级判定结果：

1. 先读取目标窗口文本/内容描述，出现“操作成功”“打卡成功”“已打卡”“已签到”等明确成功文案时记录成功。
2. 若出现“失败”“不成功”“请稍后重试”等明确失败文案，则记录失败。
3. 若没有明确成功/失败文案，来自 `TargetApps.CLOCK_PACKAGE` 的弹窗类（Dialog/Popup/Toast）`TYPE_WINDOW_STATE_CHANGED` 事件视为打卡响应弹窗出现，记录成功；心灵鸡汤类响应弹窗也按成功处理。

判定逻辑集中在 `ClockSuccessDetector.detectResponse(snapshot)`，事件路径和轮询路径共用同一套规则。

## Environment

- **Android SDK**: `C:\Users\LX\AppData\Local\Android\Sdk`
- **ADB**: `C:\Users\LX\AppData\Local\Android\Sdk\platform-tools\adb.exe`
- **JBR 17**: `C:\Users\LX\.jdks\jbr-17.0.14`

## Configuration

在源码中配置：
- `TargetApps.CLOCK_PACKAGE`：目标应用包名（如云之家），供前台检查和成功检测共用。
- `app/src/main/res/xml/accessibility_service_config.xml` 的 `android:packageNames`：无障碍服务监听/读取目标包名，必须与 `TargetApps.CLOCK_PACKAGE` 一致。
- `TargetApps.SUNFLOWER_PACKAGE`：向日葵包名（可选）。
- `TargetApps.SMART_ATTEND_ACTIVITY`：云之家快捷打卡主界面，仅用于诊断和事件过滤器，不会作为成功响应类。

其他配置通过应用内界面设置。修改无障碍服务 XML 后，需要重新安装应用或重新开启无障碍服务。

## Rules

- 隐私优先：不在公开文档中暴露私有路径、邮箱、设备ID。
- 保持 MIT 许可证和无障碍服务声明。
- 未经要求不改变应用行为。
- 涉及检测策略的改动需要同步更新：
  - `ClockSuccessDetector.kt` 与 `ClockSuccessDetectorTest.kt`
  - `README.md` “快捷打卡响应检测策略”章节
  - `CONTRIBUTING.md` 最近一次修复章节

