已根据你上传的开发记录整理成一份“接力开发文档”。核心依据包括：图标迭代、`AlarmScheduler.nextWeekdayTime()` 调度修复、`AutoClockService` 打卡后流程修正、`ClockSuccessDetector` 改用 `TYPE_WINDOW_STATE_CHANGED` 检测云之家弹窗，以及构建、GitHub 推送、ADB 安装过程。

# android-autoclock 后续开发交接文档

## 1. 项目概况

项目名称：`android-autoclock`

项目路径：

```text
D:\Projects\android-autoclock
```

项目用途：

这是一个 Android 自动打卡辅助 App。核心思路是通过 Android AccessibilityService 模拟人工操作，在指定时间窗口内自动完成云之家打卡流程，并在完成后回到向日葵界面。

当前开发状态：基本有效，已完成图标更新、调度日期修复、打卡后流程修正，并将“打卡成功”判断从特定文字识别升级为云之家窗口变化事件识别。

---

## 2. 当前已实现的主流程

早晨和傍晚流程基本一致：

```text
向日葵界面
  ↓
返回桌面
  ↓
点击云之家桌面快捷方式
  ↓
进入云之家打卡
  ↓
等待云之家弹窗响应
  ↓
检测到云之家响应弹窗，视为打卡成功
  ↓
返回桌面
  ↓
结束云之家进程
  ↓
点击向日葵桌面快捷方式
  ↓
回到向日葵界面
```

关键业务判断：

只要在打卡动作之后，云之家弹出了响应窗口，就视为打卡成功。这个响应窗口可以是“操作成功”，也可以是云之家随机出现的“心灵鸡汤 / 鼓励加班”弹窗。

---

## 3. 最近关键改动记录

### 3.0 修复响应弹窗被误判为失败（2026-06-19，commit 9712046）

涉及文件：

```text
app/src/main/java/com/autoclock/ClockSuccessDetector.kt
app/src/main/java/com/autoclock/AutoClockService.kt
app/src/test/java/com/autoclock/ClockSuccessDetectorTest.kt
README.md
```

历史问题：

云之家实际弹出响应弹窗（“操作成功”或者心灵鸡汤类）时，AutoClock
仍然记为“结束 不成功...”并附带长诊断。原因有两点：

1. `AutoClockService.onAccessibilityEvent()` 只对
   `TYPE_WINDOW_STATE_CHANGED` 记录日志“继续等待明确响应”，没有把
   弹窗事件作为成功结束；
2. 失败文本检测先于窗口事件判断，历史失败文案（如“上次任务失败”）
   可能误杀当前任务。

新逻辑：

```text
detectResponse(snapshot):
  1. 目标窗口出现明确成功文案 → SUCCESS
  2. 目标窗口出现明确失败文案 → FAILURE
  3. 目标包 TYPE_WINDOW_STATE_CHANGED 且 className 命中
     Dialog/Popup/Toast（且不等于 SmartAttendHomeActivity） → SUCCESS
  4. 其他 → UNKNOWN
```

效果：

- 弹窗类（Dialog/Popup/Toast）窗口变化即视为打卡响应成功；
- 明确失败弹窗仍会判失败；
- 普通云之家 Activity 跳转不会被误判成功；
- 轮询路径下的历史失败文案（无弹窗表面）继续视为 UNKNOWN。

事件路径与轮询路径共用 `ClockSuccessDetector.detectResponse(...)`，
移除 `AutoClockService` 中的私有 `ClockResponseResult` 枚举。

验证：

```text
gradlew.bat testDebugUnitTest lintDebug assembleDebug
→ BUILD SUCCESSFUL
```

`app-debug.apk` 已通过 ADB 安装到设备 `8KE5T19801026837`（华为 ELE-AL00）。

### 3.1 图标更新

已将 App 图标替换为新的“时钟 + 定位 / 打卡”风格图标。

最终采用方案：

不要裁切图标中心区域，直接使用原始 1254×1254 图像整体缩放。

原因：

原图自带自然白边，视觉上更舒服，适合 Android 桌面图标。之前尝试过裁切中心区域，裁切后图标主体过大，显得拥挤。最终回退到原图直接缩放，保留约 15% 左右自然留白。

已生成资源：

```text
app/src/main/res/mipmap-mdpi/ic_launcher.png
app/src/main/res/mipmap-mdpi/ic_launcher_round.png
app/src/main/res/mipmap-hdpi/ic_launcher.png
app/src/main/res/mipmap-hdpi/ic_launcher_round.png
app/src/main/res/mipmap-xhdpi/ic_launcher.png
app/src/main/res/mipmap-xhdpi/ic_launcher_round.png
app/src/main/res/mipmap-xxhdpi/ic_launcher.png
app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png
app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png
app/src/main/res/drawable/ic_launcher_foreground.png
```

自适应图标相关文件：

```text
app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
app/src/main/res/drawable/ic_launcher_background.xml
```

注意事项：

`ic_launcher.xml` 和 `ic_launcher_round.xml` 中的 `<monochrome>` 标签需要保留，不要因为替换 foreground PNG 而误删。

---

### 3.2 修复“下次任务时间显示昨天”的问题

涉及文件：

```text
app/src/main/java/com/autoclock/AlarmScheduler.kt
```

问题表现：

当天是 6 月 16 日时，下次开始任务 / 下次结束任务有时仍显示 6 月 15 日，说明缓存的下一次任务时间可能落在过去。

修复思路：

在 `nextWeekdayTime()` 末尾增加保险逻辑：

只要最终计算出的时间戳仍然小于或等于当前时间，就继续顺延到下一个工作日，直到结果一定是未来时间。

核心原则：

```text
nextClockInTime / nextClockOutTime 永远不应该保存过去的时间戳。
```

这属于防御性修复。即使前面的随机窗口、跨天、周末跳过逻辑在绝大多数情况下正确，最后仍要做一次兜底校验，避免 UI 显示旧日期。

---

### 3.3 修正打卡完成后的后续流程

涉及文件：

```text
app/src/main/java/com/autoclock/AutoClockService.kt
```

原逻辑问题：

打卡完成后，代码通过 Intent 直接启动向日葵，而不是点击用户配置的“任务后快捷方式”坐标。

这与实际需求不一致。

用户期望：

```text
返回桌面 → 结束云之家进程 → 点击向日葵桌面快捷方式
```

已修正为：

```text
postClockSequence()
  1. returnToHomeScreen()
  2. 延迟 500ms 后 killTargetApp()
  3. 等待 HOME_SETTLE_DELAY_MS
  4. 使用 prefs.afterClockX / prefs.afterClockY 计算坐标
  5. performTap() 点击向日葵桌面快捷方式
  6. releaseWakeLock()
```

注意：

`openSunflower()` 方法可以保留，作为未来兜底方案，但当前主流程应优先使用坐标点击。

---

### 3.4 打卡成功检测逻辑升级

涉及文件：

```text
app/src/main/java/com/autoclock/ClockSuccessDetector.kt
app/src/main/java/com/autoclock/AutoClockService.kt
```

旧逻辑：

主要依赖文本识别，例如检测是否出现“操作成功”。

问题：

云之家弹窗内容不稳定。有时不是“操作成功”，而是“心灵鸡汤 / 鼓励加班”的随机弹窗。如果只识别固定文字，会漏判成功。

新逻辑：

改为三层检测：

```text
第一层：AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        只要事件来自云之家，并且是窗口状态变化，就认为云之家弹出了响应窗口。

第二层：AccessibilityEvent 文本兜底
        如果文本中没有明确失败关键词，且有文本内容，则视为成功响应。

第三层：rootInActiveWindow 轮询兜底
        每 500ms 主动读取当前窗口内容，逻辑与第二层保持一致。
```

目标包名：

```text
com.kdweibo.client
```

成功判断核心：

```text
打卡后等待阶段内，云之家出现新窗口 = 打卡响应已发生 = 视为成功。
```

失败关键词：

```text
失败
不成功
请稍后重试
未在前台
```

注意事项：

这个方案比纯文字识别更稳，但也存在一个潜在风险：如果云之家在打卡后弹出的是升级提示、权限提示、异常提示，也可能被误判为成功。因此后续可以继续增强过滤条件。

---

## 4. 当前 Git / GitHub 状态

仓库：

```text
https://github.com/lxsfful/android-autoclock.git
```

当前分支：

```text
fix/real-autoclock-flow
```

最近关键提交：

```text
9712046 fix: detect cloud punch response windows
4de8678 feat: 更新图标、修复调度日期显示、修正打卡后流程
34d6507 fix: 图标改用原图直接缩放，保留自然白边（~15%）
41ba9ae feat: 改用 TYPE_WINDOW_STATE_CHANGED 检测打卡弹窗，兼容心灵鸡汤等任意响应窗口
```

如果直接 `git push` 失败，可能是网络连接被重置。记录中成功使用过 SOCKS5 代理推送：

```bash
git -c http.proxy=socks5h://100.99.78.35:1080 \
    -c https.proxy=socks5h://100.99.78.35:1080 \
    push
```

---

## 5. 构建与安装环境

### 5.1 Java / Gradle

直接运行：

```bash
./gradlew assembleDebug
```

曾失败，原因是：

```text
JAVA_HOME is not set and no 'java' command could be found in your PATH.
```

可用 JBR 路径：

```text
C:\Users\LX\.jdks\jbr-17.0.14
```

推荐使用 PowerShell 构建：

```powershell
$env:JAVA_HOME = 'C:\Users\LX\.jdks\jbr-17.0.14'
cd D:\Projects\android-autoclock
.\gradlew assembleDebug
```

构建产物：

```text
D:\Projects\android-autoclock\app\build\outputs\apk\debug\app-debug.apk
```

---

### 5.2 ADB

系统 PATH 中可能没有 `adb`，但实际可用路径为：

```text
C:\Users\LX\AppData\Local\Android\Sdk\platform-tools\adb.exe
```

检查设备：

```bash
/c/Users/LX/AppData/Local/Android/Sdk/platform-tools/adb.exe devices
```

记录中连接过的设备：

```text
8KE5T19801026837   device
```

安装 debug APK：

```bash
/c/Users/LX/AppData/Local/Android/Sdk/platform-tools/adb.exe install -r /d/Projects/android-autoclock/app/build/outputs/apk/debug/app-debug.apk
```

如果遇到签名冲突，需要先卸载旧版，再安装 debug 包。

---

## 6. 关键文件说明

### 6.1 `AlarmScheduler.kt`

负责定时任务调度。

重点函数：

```text
nextWeekdayTime()
scheduleAll()
```

关键逻辑：

* 根据早晨 / 傍晚时间窗口生成随机触发时间。
* 跳过周末。
* 保证返回值一定是未来时间。
* 调度后写入 `prefs.nextClockInTime` 和 `prefs.nextClockOutTime`，供 UI 显示。

后续注意：

任何涉及日期、时间窗口、周末跳过、节假日跳过的改动，都应在这里集中处理。

---

### 6.2 `AlarmReceiver.kt`

负责接收系统闹钟触发。

记录中提到：

闹钟触发后会调用 `AlarmScheduler.scheduleAll()` 重新安排下一次任务。

后续注意：

这里是“执行任务”和“安排下一次任务”的衔接点。若出现下次任务不更新、重复触发、触发后仍显示旧时间，应重点检查这里与 `AlarmScheduler` 的交互。

---

### 6.3 `AutoClockService.kt`

项目核心文件之一。

负责：

* AccessibilityService 事件监听。
* 模拟返回桌面。
* 模拟点击云之家快捷方式。
* 等待云之家响应。
* 检测成功 / 失败。
* 打卡完成后的收尾流程。
* WakeLock 释放。
* 点击向日葵快捷方式返回远程控制界面。

重点函数 / 逻辑：

```text
onAccessibilityEvent()
postClockSequence()
performTap()
returnToHomeScreen()
killTargetApp()
collectActiveWindowTexts()
detectSuccessFromActiveWindow()
```

后续注意：

这里是最容易出现设备适配问题的地方。尤其是坐标点击、桌面布局变化、云之家界面变化、向日葵界面恢复，都要实机验证。

---

### 6.4 `ClockSuccessDetector.kt`

负责判断云之家响应是否代表打卡成功。

当前策略（2026-06-19 更新）：

```text
detectResponse(snapshot):
  1. 目标窗口出现明确成功文案 → SUCCESS
  2. 目标窗口出现明确失败文案 → FAILURE
  3. 目标包 TYPE_WINDOW_STATE_CHANGED 且 className 命中
     Dialog/Popup/Toast（且不等于 SmartAttendHomeActivity） → SUCCESS
  4. 其他 → UNKNOWN
```

事件路径和轮询路径共用 `detectResponse(...)`。

后续优化方向：

可以把检测逻辑改成更严格的状态机：

```text
只有在“已点击打卡按钮后的 N 秒等待窗口内”
且事件 packageName == com.kdweibo.client
且 eventType == TYPE_WINDOW_STATE_CHANGED
且 className 命中弹窗类（Dialog/Popup/Toast）
才判定为成功。
```

这样可以减少升级弹窗、权限弹窗等被误判为成功的概率。

---

### 6.5 `Prefs.kt`

负责保存用户配置。

重点包括：

```text
nextClockInTime
nextClockOutTime
afterClockX
afterClockY
```

后续注意：

如果出现坐标不生效、下次任务显示不对、配置丢失等问题，优先检查这里。

---

### 6.6 `MainActivity.kt`

负责配置界面。

记录中提到：

界面上已经存在“任务后快捷方式”坐标配置，例如 `btnPickAfterClockCoords`。之前问题是 UI 有配置项，但 `postClockSequence()` 没有真正使用它。现在已经改为使用 `prefs.afterClockX / prefs.afterClockY`。

---

## 7. 当前最重要的实机测试清单

### 7.1 基础测试

1. 安装 debug APK。
2. 打开 App。
3. 确认辅助功能服务已开启。
4. 确认电池优化 / 后台限制不会杀掉服务。
5. 设置云之家快捷方式坐标。
6. 设置任务后向日葵快捷方式坐标。
7. 查看“下次开始任务 / 下次结束任务”是否显示未来时间，而不是过去时间。

---

### 7.2 打卡流程测试

建议用临近时间窗口手动测试。

验证点：

```text
是否能从向日葵界面回到桌面
是否能点击云之家快捷方式
是否能进入云之家打卡
是否能触发打卡动作
云之家弹窗出现后是否立即判定成功
是否返回桌面
是否结束云之家进程
是否点击向日葵快捷方式
是否最终回到向日葵界面
WakeLock 是否释放
```

---

### 7.3 弹窗识别测试

重点测试三类情况：

```text
1. 云之家弹出“操作成功”
2. 云之家弹出心灵鸡汤 / 鼓励加班文案
3. 云之家弹出失败 / 网络异常 / 权限异常
```

预期：

```text
前两类应判定成功。
第三类不应判定成功，或至少应在日志中清楚暴露原因。
```

---

### 7.4 时间调度测试

需要覆盖：

```text
工作日早晨窗口前
工作日早晨窗口中
工作日早晨窗口后
工作日傍晚窗口前
工作日傍晚窗口中
工作日傍晚窗口后
周五傍晚后
周六
周日
系统时间跨天
```

核心验收标准：

```text
nextClockInTime / nextClockOutTime 永远不能落在当前时间之前。
周末应跳到下一个工作日。
```

---

## 8. 已知风险与短板

### 8.1 云之家窗口事件可能误判

当前逻辑认为：

```text
打卡后，云之家弹窗类（Dialog/Popup/Toast）窗口变化 = 成功
```

这符合目前观察，但不是数学上绝对安全。

潜在误判：

```text
版本升级弹窗
权限弹窗
网络异常弹窗
登录失效弹窗
公告弹窗
```

后续建议：

增加“打卡等待态”状态机，只在点击打卡后的短时间窗口内接受窗口变化事件。

可以记录：

```text
lastClockTapTimestamp
isWaitingForSuccessPopup
hasTerminalResult
lastWindowPackage
```

并限制：

```text
now - lastClockTapTimestamp <= 10~20 秒
```

并通过弹窗文本/类名白名单进一步收紧，例如只接受云之家自家的
弹窗 Activity。

---

### 8.2 坐标点击依赖桌面布局

当前流程依赖用户配置桌面快捷方式坐标。

风险：

```text
桌面图标移动
分辨率变化
横竖屏变化
系统缩放变化
桌面启动器变化
状态栏 / 导航栏变化
```

后续建议：

优先保留坐标点击，因为它最贴合当前实机环境；但可以增加备用方案：

```text
坐标点击失败 → Intent 启动目标 App
```

或者：

```text
通过 AccessibilityNode 查找云之家 / 向日葵图标文字
```

---

### 8.3 图标资源替换容易破坏 adaptive icon

之前替换图标时出现过 `<monochrome>` 标签丢失的问题。

后续修改图标时务必检查：

```text
mipmap-anydpi-v26/ic_launcher.xml
mipmap-anydpi-v26/ic_launcher_round.xml
drawable/ic_launcher_background.xml
drawable/ic_launcher_foreground.png
```

不要只看传统 mipmap PNG，也要看 Android 8.0+ 自适应图标效果。

---

### 8.4 Windows 开发环境不稳定点

已遇到：

```text
adb 不在 PATH
JAVA_HOME 未设置
GitHub 直连 push 被 reset
PowerShell / bash 路径转义问题
C 盘空间告急
```

后续开发者应优先确认环境，而不是直接改代码。

---

## 9. 后续推荐开发路线

### 9.1 第一优先级：做实机日志增强

建议增加一个简单的本地日志面板或导出日志功能，记录：

```text
任务触发时间
点击坐标
当前窗口 packageName
AccessibilityEvent eventType
检测到的文本
是否进入等待弹窗状态
是否检测成功
失败原因
postClockSequence 是否完整执行
WakeLock 是否释放
```

这样以后不需要反复猜测。

---

### 9.2 第二优先级：增强成功判定状态机

当前检测逻辑已经比文字识别更稳，但可以更严谨：

```text
IDLE
  ↓
OPENING_CLOCK_APP
  ↓
WAITING_FOR_CLOCK_RESPONSE
  ↓
SUCCESS / FAILURE / TIMEOUT
  ↓
POST_CLOCK_SEQUENCE
  ↓
DONE
```

只有在 `WAITING_FOR_CLOCK_RESPONSE` 状态内，才允许 `TYPE_WINDOW_STATE_CHANGED` 判成功。

---

### 9.3 第三优先级：失败恢复机制

建议增加：

```text
超时后截图或记录窗口文本
失败后返回桌面并清理云之家
失败后仍回到向日葵
失败后下次任务正常调度
连续失败次数统计
```

避免一次失败后 App 卡死在中间状态。

---

### 9.4 第四优先级：减少坐标依赖

短期保留坐标点击。

中期可加入：

```text
按文字查找桌面图标
按 packageName 直接启动 App
按 AccessibilityNode 查找打卡按钮
```

但不要一开始就完全替换坐标方案，因为当前项目的有效性来自“贴合固定手机环境”。

---

### 9.5 第五优先级：处理 C 盘空间问题

开发记录最后卡在 C 盘空间告急和 Claude API quota 冷却。

建议下一次接手时先做环境清理：

```text
检查 C 盘大文件
清理 Gradle 缓存中过旧版本
清理 Android build 缓存
清理 Android Studio 缓存
清理临时文件
确认项目是否可以迁移更多缓存到 D 盘
```

但不要盲目删除：

```text
C:\Users\LX\.gradle
C:\Users\LX\AppData\Local\Android\Sdk
C:\Users\LX\.jdks
```

这些目录虽然可能很大，但直接删除可能导致构建环境损坏。

---

## 10. 常用命令

### 10.1 构建 debug 包

PowerShell：

```powershell
$env:JAVA_HOME = 'C:\Users\LX\.jdks\jbr-17.0.14'
cd D:\Projects\android-autoclock
.\gradlew assembleDebug
```

---

### 10.2 检查 ADB 设备

Git Bash / bash：

```bash
/c/Users/LX/AppData/Local/Android/Sdk/platform-tools/adb.exe devices
```

---

### 10.3 安装 APK

```bash
/c/Users/LX/AppData/Local/Android/Sdk/platform-tools/adb.exe install -r /d/Projects/android-autoclock/app/build/outputs/apk/debug/app-debug.apk
```

---

### 10.4 Git 提交

```bash
cd /d/Projects/android-autoclock
git status
git add .
git commit -m "feat: 描述本次改动"
```

---

### 10.5 GitHub 推送

普通推送：

```bash
git push origin master
```

如果网络失败，用代理：

```bash
git -c http.proxy=socks5://100.99.78.35:1080 \
    -c https.proxy=socks5://100.99.78.35:1080 \
    push origin master
```

---

## 11. 开发经验总结

### 11.1 不要过度依赖 UI 文案

云之家弹窗文案不稳定，因此“识别操作成功四个字”不是可靠方案。

更好的思路是识别行为：

```text
打卡后，目标 App 弹出响应窗口
```

这比识别具体文字更稳定。

---

### 11.2 时间调度必须有最终保险

任何调度算法最后都应保证：

```text
返回时间 > 当前时间
```

即使前面的窗口计算、周末跳过都看起来正确，也要有最终 while 兜底。

---

### 11.3 图标不要盲目裁切

图标原图自带的留白不一定是浪费，可能正是视觉设计所需的“呼吸感”。

本项目最终经验：

```text
原图直接缩放 > 激进裁切中心区域
```

尤其是 Android 桌面图标，小尺寸下过度铺满会显得粗糙、拥挤。

---

### 11.4 先实机有效，再追求抽象优雅

这个 App 的目标不是做通用自动化框架，而是在固定手机、固定桌面、固定云之家流程下可靠运行。

因此当前更重要的是：

```text
稳定
可观察
可恢复
可测试
```

而不是过早做复杂架构。

---

## 12. 下一位开发者接手建议

接手后不要马上大改，建议顺序：

```text
1. 先确认当前 master 能构建。
2. 用 ADB 安装到手机。
3. 打开 App，确认图标、权限、坐标配置。
4. 做一次手动触发或临近时间窗口测试。
5. 看日志确认 TYPE_WINDOW_STATE_CHANGED 是否能捕捉云之家弹窗。
6. 如果误判或漏判，再改 ClockSuccessDetector。
7. 如果流程卡住，再改 AutoClockService 的状态机和延迟。
8. 最后再处理 C 盘空间和开发环境优化。
```

当前项目最值得继续投入的方向：

```text
把成功检测、失败检测、超时恢复做成清晰状态机，并增加可查看日志。
```

这样这个 App 会从“基本有效”提升到“可长期维护”。
