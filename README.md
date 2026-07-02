# android-autoclock

Android/Kotlin 应用，基于 AccessibilityService 在指定时间窗口内执行自动化打卡流程，记录历史，并可通过 SMTP 发送成功/失败通知。

## 使用边界

仅在你有明确权限的设备、账户和应用上使用。不要用于规避规则、伪造操作、绕过控制，或自动化任何禁止自动化的服务。

## 核心能力

- 两个可配置时间窗口：早上打卡、傍晚打卡。
- `AlarmManager.setExactAndAllowWhileIdle` 精确闹钟触发。
- 窗口内随机分钟/秒，避免固定触发点。
- 每个窗口每天最多自动尝试 2 次；失败才可能在当前窗口内重试。
- 跳过周末和静态表内中国节假日：春节、劳动节、端午节、中秋节、国庆节。
- 通过 AccessibilityService 执行 Home、坐标点击、窗口事件监听和目标窗口文本读取。
- 仅在目标包名范围内检测云之家响应窗口/文本。
- 本地历史记录和可选 SMTP 邮件通知。
- SMTP 授权码使用 Android Keystore 加密保存。

## 快速开始

```bash
./setup.sh
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Windows / Git Bash 若找不到 Java：

```bash
export JAVA_HOME='C:\Users\LX\.jdks\jbr-17.0.14'
export PATH='/c/Users/LX/.jdks/jbr-17.0.14/bin:'"$PATH"
cmd.exe //C "D: && cd \\Projects\\android-autoclock && .\\gradlew.bat testDebugUnitTest"
```

安装到已授权设备：

```bash
/c/Users/LX/AppData/Local/Android/Sdk/platform-tools/adb.exe devices -l
/c/Users/LX/AppData/Local/Android/Sdk/platform-tools/adb.exe install -r \
  /d/Projects/android-autoclock/app/build/outputs/apk/debug/app-debug.apk
```

## 常用命令

| Command | Description |
| --- | --- |
| `./setup.sh` | 验证 JDK、Gradle Wrapper、`local.properties` |
| `./gradlew testDebugUnitTest` | JVM 单元测试 |
| `./gradlew assembleDebug` | 构建 debug APK |
| `./gradlew installDebug` | 构建并安装 debug APK |
| `./gradlew lintDebug` | Android lint |
| `./gradlew clean` | 清理构建输出 |

## 关键配置

| 配置项 | 位置 | 说明 |
| --- | --- | --- |
| `TargetApps.CLOCK_PACKAGE` | `TargetApps.kt` | 云之家包名，供前台检查和成功检测共用 |
| `TargetApps.SMART_ATTEND_ACTIVITY` | `TargetApps.kt` | 快捷打卡界面 Activity，仅用于诊断/过滤 |
| `android:packageNames` | `app/src/main/res/xml/accessibility_service_config.xml` | 无障碍服务监听/读取目标包名，需与 `TargetApps.CLOCK_PACKAGE` 一致 |
| `TargetApps.SUNFLOWER_PACKAGE` | `TargetApps.kt` | 后置流程优先打开的向日葵包名 |
| SMTP 配置 | 应用内设置 | 可选邮件通知 |

修改无障碍服务 XML 或重新安装 APK 后，请在系统设置中确认 AutoClock 无障碍服务仍启用。

SMTP 通知为 best-effort：配置为空、网络异常或授权码失效时只写日志，不阻塞打卡流程。端口 `587` 使用 STARTTLS；`465` 以及其他非 `587` 端口按 SSL 处理。

## 调度、重试与节假日策略

- 正常调度只会在窗口开始前安排当天随机时刻；若已经进入或错过当前窗口，则安排下一有效工作日。
- 闹钟触发后记录当天该窗口尝试次数：早上最多 2 次，傍晚最多 2 次。
- 成功后不再重试同一窗口。
- 失败或无障碍服务未运行时，若未达 2 次且窗口仍有足够时间，才安排一次当前窗口重试。
- `ChineseHolidayCalendar.kt` 维护离线静态节假日表；当前覆盖 2025/2026 年相关假期。
- 未配置年份退化为仅跳过周末；当前不支持调休补班日周末执行。

## 快捷打卡响应检测策略

等待云之家快捷打卡响应期间，`ClockSuccessDetector.detectResponse(snapshot)` 按以下优先级判定：

1. 目标窗口文本/内容描述出现“操作成功”“打卡成功”“已打卡”“已签到”等明确成功文案 → 成功。
2. 出现“失败”“不成功”“请稍后重试”等明确失败文案 → 失败。
3. 无明确文案时，目标包名的 Dialog/Popup/Toast 类 `TYPE_WINDOW_STATE_CHANGED` 事件视为响应弹窗出现 → 成功。
4. 其他情况 → 继续等待，直到超时失败。

## 诊断记录

### 手动诊断模式

在"开始诊断"页面可录制完整无障碍事件日志，用于排查打卡失败原因。录制时：

1. 确认已开启 AutoClock 无障碍服务。
2. 点击"开始诊断"——3 秒倒计时后自动返回桌面。
3. 手动完成一次真实的打卡操作（进入云之家 → 快捷打卡 → 观察结果）。
4. 回到应用点击"停止诊断"。

### 自动诊断记录

自动打卡流程（闹钟触发）会在没有进行手动诊断时，自动启动本地诊断采样，完整记录本次打卡序列的无障碍事件和节点快照，无需手动干预。

采样文件同样保存在 `filesDir/diagnostics/` 下，包含以下标记记录：

- `clock_sequence_start` — 打卡序列开始，`reason` 字段说明启动原因（自动启动或已有手动诊断进行中）。
- `clock_sequence_terminal_result` — 打卡终态（成功/失败/未知），含结果和原因。
- `clock_sequence_stop` — 打卡序列结束。

如果手动诊断正在录制，自动打卡序列不会停止该会话，仅追加上述标记记录，避免干扰手动诊断的完整性。

### 通用说明

日志为本地私有 JSONL 文件，文件名含时间戳。内容经过截断/脱敏（密码字段自动过滤），单文件最大 2 MB。**无任何网络捕获或上传行为。**

## 核心文件

```text
app/src/main/java/com/autoclock/
├── AlarmScheduler.kt          # 精确闹钟、失败重试、节假日跳过
├── AlarmReceiver.kt           # 闹钟入口、尝试上限、服务缺失失败记录
├── AutoClockService.kt        # 无障碍手势序列、检测等待、终态处理
├── ClockSuccessDetector.kt    # 响应成功/失败/未知判定
├── ClockAttemptTracker.kt     # 每日早/晚窗口尝试次数上限
├── ChineseHolidayCalendar.kt  # 中国节假日静态表
├── DiagnosticRecorder.kt      # 诊断记录：JSONL 写入、节点快照、脱敏
├── EmailSender.kt             # SMTP 发送
├── ClockEmailContent.kt       # 邮件内容
├── Prefs.kt                   # 设置、历史 JSON、加密邮件密码
├── TargetApps.kt              # 目标包名配置
└── MainActivity.kt            # 主界面和配置入口
```

## 常见问题

### 历史记录显示“无障碍服务未运行”

闹钟已触发，但 AutoClock AccessibilityService 不在运行。应用会记录失败历史，并在 SMTP 配置完整时发送失败邮件。请重新开启无障碍服务，并将应用加入电池优化白名单。

### 历史记录显示“未确认云之家前台”

优先检查：桌面快捷方式坐标、等待秒数、目标包名一致性、手机是否锁屏、是否有系统弹窗或其他 App 遮挡。

### 节假日或周末未触发

这是预期行为。新年份放假安排发布后，需要更新 `ChineseHolidayCalendar.kt` 和 `ChineseHolidayCalendarTest.kt`。

## 开发交接

维护规则、测试覆盖和年度节假日更新清单见 `CONTRIBUTING.md`。Claude/Agent 项目指令见 `CLAUDE.md`。

## License

MIT. See `LICENSE`.
