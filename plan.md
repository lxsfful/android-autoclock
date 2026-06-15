# AutoClock 开发计划（历史记录）

> 本文件是历史开发计划，不再作为当前实现状态的唯一依据。当前运行逻辑以源码、`README.md`、`CLAUDE.md` 和应用内使用说明为准。

## 当前关键状态

- 随机时间调度通过 `AlarmScheduler` 使用 `AlarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP)` 实现。
- 自动化入口为 `AlarmReceiver` → `AutoClockService.instance?.performClockSequence(...)`。
- 目标应用包名需在以下位置保持一致：
  - `TargetApps.CLOCK_PACKAGE`
  - `app/src/main/res/xml/accessibility_service_config.xml` 的 `android:packageNames`
- 成功检测基于目标应用窗口内的“操作成功”文本/内容描述，不再仅凭窗口变化事件或目标 App 前台状态判定成功。
- WakeLock 可唤醒/点亮屏幕，但不能绕过安全锁屏；锁屏会记录为明确失败原因。
- 打卡完成后当前逻辑为：返回桌面 → 尝试结束目标 App → 打开向日葵。

## 已废弃/过时的历史项

- “`ClockSuccessDetector.kt` 中 `CLOCK_APP_PACKAGE = "com.example.targetapp"` 是占位符”已过时；当前包名为 `com.kdweibo.client`。
- “打卡后杀云之家进程无延时”已过时；当前已有打卡完成后的延迟后续操作。
- “第三次点击任务后快捷方式”不是当前运行逻辑；如后续需要恢复，应单独设计并同步 UI/文档。

## 当前排查重点

1. 历史显示“任务 App 未在前台”时，优先检查桌面快捷方式坐标、等待秒数、目标包名一致性和无障碍服务是否重启生效。
2. 历史显示“设备处于锁屏状态”时，说明系统已触发任务，但 AutoClock 不能绕过安全锁屏。
3. 历史显示“无障碍服务未运行”时，说明闹钟触发时 AccessibilityService 未连接。
