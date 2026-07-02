# android-autoclock

Android/Kotlin AccessibilityService 自动打卡应用。当前权威说明：`README.md`；开发交接：`CONTRIBUTING.md`。

## Commands

```bash
./gradlew testDebugUnitTest # JVM 单元测试
./gradlew assembleDebug    # 构建 debug APK
./gradlew installDebug     # 构建并安装到设备
./gradlew lintDebug        # Android lint
```

Windows/Git Bash 若找不到 Java：

```bash
export JAVA_HOME='C:\Users\LX\.jdks\jbr-17.0.14'
export PATH='/c/Users/LX/.jdks/jbr-17.0.14/bin:'"$PATH"
cmd.exe //C "D: && cd \\Projects\\android-autoclock && .\\gradlew.bat testDebugUnitTest"
```

ADB：`C:\Users\LX\AppData\Local\Android\Sdk\platform-tools\adb.exe`

## Core files

```text
app/src/main/java/com/autoclock/
├── AlarmScheduler.kt          # 精确闹钟、失败重试、节假日跳过
├── AlarmReceiver.kt           # 闹钟入口、尝试上限、服务缺失失败记录
├── AutoClockService.kt        # 无障碍手势序列、检测等待、终态处理
├── ClockSuccessDetector.kt    # 响应成功/失败/未知判定
├── ClockAttemptTracker.kt     # 每日早/晚窗口尝试次数上限
├── ChineseHolidayCalendar.kt  # 中国节假日静态表
├── EmailSender.kt             # SMTP 发送
├── ClockEmailContent.kt       # 邮件内容
├── Prefs.kt                   # 设置、历史 JSON、加密邮件密码
├── TargetApps.kt              # 目标包名配置
└── MainActivity.kt            # 主界面和配置入口
```

## Maintenance rules

- 隐私优先：不要在公开文档中新增邮箱、设备 ID、私有密钥。
- 保持 MIT 许可证和无障碍服务声明。
- 未经要求不改变应用行为。
- 修改目标包名时同步：`TargetApps.CLOCK_PACKAGE` 与 `accessibility_service_config.xml` 的 `android:packageNames`。
- 修改检测策略时同步：
  - `ClockSuccessDetector.kt`
  - `ClockSuccessDetectorTest.kt`
  - `README.md` 的“快捷打卡响应检测策略”
- 修改调度/重试/节假日/尝试上限时同步：
  - `AlarmScheduler.kt`、`ClockAttemptTracker.kt`、`ChineseHolidayCalendar.kt`
  - `AlarmSchedulerTest.kt`、`ClockAttemptTrackerTest.kt`、`ChineseHolidayCalendarTest.kt`
  - `README.md` 的“调度、重试与节假日策略”
  - `CONTRIBUTING.md` 的维护清单
- 代码改动后必须运行相关测试；至少运行 `testDebugUnitTest`，发布/安装前运行 `assembleDebug`。
