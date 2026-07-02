# android-autoclock 维护交接

本文件是开发者交接入口。面向使用者的说明以 `README.md` 为准；Claude/Agent 操作规则以 `CLAUDE.md` 为准。

## 当前关键行为

- 自动任务入口：`AlarmReceiver` → `AutoClockService.performClockSequence(...)`。
- 成功/失败判定集中在 `ClockSuccessDetector.detectResponse(snapshot)`。
- 调度策略集中在 `AlarmScheduler.kt`：正常调度不会在当前窗口内重复安排；失败才可能在当前窗口重试。
- 每日尝试上限集中在 `ClockAttemptTracker.kt`：早上最多 2 次，傍晚最多 2 次。
- 节假日跳过集中在 `ChineseHolidayCalendar.kt`：当前维护 2025/2026 年春节、劳动节、端午节、中秋节、国庆节静态表；未配置年份只跳过周末。
- 邮件内容集中在 `ClockEmailContent.kt`；发送逻辑在 `EmailSender.kt`。失败邮件为 best-effort，不做持久化重试。

## 常用命令

```powershell
$env:JAVA_HOME = 'C:\Users\LX\.jdks\jbr-17.0.14'
cd D:\Projects\android-autoclock
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

Git Bash 中可用：

```bash
export JAVA_HOME='C:\Users\LX\.jdks\jbr-17.0.14'
export PATH='/c/Users/LX/.jdks/jbr-17.0.14/bin:'"$PATH"
cmd.exe //C "D: && cd \\Projects\\android-autoclock && .\\gradlew.bat testDebugUnitTest"
```

ADB 安装：

```bash
/c/Users/LX/AppData/Local/Android/Sdk/platform-tools/adb.exe devices -l
/c/Users/LX/AppData/Local/Android/Sdk/platform-tools/adb.exe install -r \
  /d/Projects/android-autoclock/app/build/outputs/apk/debug/app-debug.apk
```

## 本次调度可靠性修复

### 背景

实测出现：自动打卡未执行、失败无邮件提醒、同一早/晚窗口内短时间连续触发多次。根因是旧版 `AlarmReceiver` 每次触发后无条件重新 `scheduleAll`，而旧调度逻辑可能再次随机到当前窗口内的未来几分钟；服务未运行时也只写历史，不发失败邮件。

### 修复点

1. `AlarmScheduler.kt`：正常调度进入或错过当前窗口后，直接安排下一有效工作日。
2. `AlarmScheduler.scheduleRetryOrNext(...)`：失败/服务缺失时才尝试当前窗口重试。
3. `ClockAttemptTracker.kt`：早/晚窗口按本地日期分别计数，每个窗口最多 2 次。
4. `ChineseHolidayCalendar.kt`：跳过周末和静态表内指定中国节假日。
5. `AlarmReceiver.kt`：服务未运行时记录失败、发送失败邮件，并按规则安排重试或下一工作日。
6. `AutoClockService.kt`：任务终态后按成功/失败安排下一次或重试。
7. `ClockEmailContent.kt` / `EmailSender.kt`：失败邮件正文包含时间和原因。

### 覆盖测试

- `AlarmSchedulerTest.kt`：正常调度、失败重试、尝试上限、周末/节假日跳过。
- `ClockAttemptTrackerTest.kt`：同日计数、换日重置、最多两次。
- `ChineseHolidayCalendarTest.kt`：2026 年春节、劳动节、端午节、中秋节、国庆节样例和周末。
- `ClockEmailContentTest.kt`：失败邮件正文包含时间和原因。
- `ClockSuccessDetectorTest.kt`：目标窗口文本、内容描述、弹窗事件的成功/失败/未知判定。

## 后续维护清单

### 修改检测策略时

同步更新：

- `app/src/main/java/com/autoclock/ClockSuccessDetector.kt`
- `app/src/test/java/com/autoclock/ClockSuccessDetectorTest.kt`
- `README.md` 的“快捷打卡响应检测策略”

### 修改调度/重试/节假日时

同步更新：

- `app/src/main/java/com/autoclock/AlarmScheduler.kt`
- `app/src/main/java/com/autoclock/ClockAttemptTracker.kt`
- `app/src/main/java/com/autoclock/ChineseHolidayCalendar.kt`
- `app/src/test/java/com/autoclock/AlarmSchedulerTest.kt`
- `app/src/test/java/com/autoclock/ClockAttemptTrackerTest.kt`
- `app/src/test/java/com/autoclock/ChineseHolidayCalendarTest.kt`
- `README.md` 的“调度、重试与节假日策略”

### 每年节假日维护

国务院办公厅发布新一年放假安排后：

1. 在 `ChineseHolidayCalendar.kt` 增加对应年份日期。
2. 在 `ChineseHolidayCalendarTest.kt` 增加至少一个春节、劳动节、端午节、中秋节、国庆节样例。
3. 运行 `testDebugUnitTest` 和 `assembleDebug`。

当前策略仍保持“周末不打卡”，未把调休补班日作为工作日。

## 验收标准

完成代码改动后至少运行：

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

涉及无障碍服务 XML、目标包名、安装包更新时，安装后需要在手机系统设置中重新确认 AutoClock 无障碍服务是否仍启用。
