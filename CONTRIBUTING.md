## android-autoclock 接力开发记录

> 本文件是项目维护与交接说明，最近一次重大修复见末尾“最近一次修复”章节。
> 如需查阅历史调研与未通过的实现，请参考 `android-autoclock 后续开发交接文档.md`、
> `README_CURRENT_VERSION_DRAFT.md` 和 `PROJECT_SCAN.md` 的原始记录。

## 项目概览

- **项目名称**：android-autoclock
- **业务场景**：基于 Android AccessibilityService，在指定时间窗口内点击桌面
  “云之家快捷打卡”快捷方式，等待云之家弹出响应弹窗（“操作成功”或者
  心灵鸡汤类任意响应窗口），随后返回桌面、结束云之家后台进程、打开
  向日葵远程界面。
- **当前分支**：`fix/real-autoclock-flow`（最新提交 `9712046`）
- **远程仓库**：`https://github.com/lxsfful/android-autoclock.git`
- **目标设备**：华为 ELE-AL00 (Android 10)，已通过 ADB 安装最新 debug APK。

## 最近一次修复

### 背景

`android-autoclock` 在云之家实际弹出快捷打卡响应窗口时，仍然被记录为
“结束 不成功...”并附带一长串诊断文本。`CONTRIBUTING.md` 历史版本描述的
问题是“窗口状态变化应作为成功主信号”，但实现中 `AutoClockService`
只对窗口事件记录日志、继续等待明确文本，失败文本检测又先于窗口事件
判断，容易被旧失败文案误杀。

### 修复要点

1. **统一响应判定**：`ClockSuccessDetector.kt` 新增
   `ClockResponseDecision` 枚举和 `detectResponse(snapshot)` 函数，集中
   处理成功/失败/未知判定。
2. **明确成功/失败文本优先**：出现“操作成功”“打卡成功”“已打卡”
   “已签到”等明确成功文案时优先记录成功；出现“失败”“不成功”
   “请稍后重试”等明确失败文案时记录失败。
3. **弹窗类窗口事件作为成功兜底**：当目标包 `com.kdweibo.client` 出现
   `TYPE_WINDOW_STATE_CHANGED` 且 `className` 命中 `Dialog` / `Popup` /
   `Toast` 等弹窗类（且不等于 `SmartAttendHomeActivity`），视为响应
   弹窗出现，按成功处理；心灵鸡汤类弹窗同样会被识别。
4. **避免误判**：
   - 等待响应阶段只在 `isWaitingForSuccessPopup && !hasTerminalResult` 下
     接收事件；
   - 目标包普通 Activity 跳转不会被当成成功；
   - 轮询路径中的失败关键词需要命中弹窗类窗口才记失败，历史失败文案
     （如“上次任务失败”）不会触发当前任务失败。
5. **服务层接入**：`AutoClockService.onAccessibilityEvent()` 与
   `detectResponseFromActiveWindow()` 全部改用 `detectResponse(...)`，
   私有 `ClockResponseResult` 枚举移除。

### 关键文件

- `app/src/main/java/com/autoclock/ClockSuccessDetector.kt`
- `app/src/main/java/com/autoclock/AutoClockService.kt`
- `app/src/test/java/com/com/autoclock/ClockSuccessDetectorTest.kt`
- `README.md`：新增“快捷打卡响应检测策略”章节

### 单元测试

`ClockSuccessDetectorTest.kt` 覆盖以下场景：

- 弹窗类窗口状态变化 → 成功
- 目标包 `SmartAttendHomeActivity` 窗口变化 → 未知
- 目标包普通 Activity 窗口变化 → 未知
- 弹窗窗口变化 + 明确失败文案 → 失败
- 内容变化事件 + 明确成功文案 → 成功
- 内容变化事件 + 明确失败文案 → 失败
- 中性内容变化文本 → 未知
- 轮询路径下“上次任务失败”无弹窗表面 → 未知

### 验收

- 单元测试：`gradlew.bat testDebugUnitTest` 全绿
- Lint：`gradlew.bat lintDebug` 通过
- 构建：`gradlew.bat assembleDebug` 通过
- 真机：`app-debug.apk` 已 ADB 安装至 `8KE5T19801026837`
- logcat 关键日志：`检测到云之家打卡响应弹窗（窗口状态变化）`
  （命中弹窗类窗口时）

### 待继续验证（实机部分）

- 在云之家实际响应后，App 历史记录应在 ~2 秒内出现成功记录，
  不再出现“结束 不成功：...”的失败记录。
- 失败原因在真实失败场景下保持可读，不应被“乱码”/过长诊断信息淹没。
- 后置流程仍完整：返回桌面 → `ActivityManager.killBackgroundProcesses`
  → 通过包名打开向日葵（失败时坐标兜底）。

## 历史问题背景（保留以备查阅）

历史版本曾记录以下问题，本次修复已覆盖其中核心两点：

- 检测逻辑未正确识别窗口事件 → 已用 `detectResponse(...)` 集中处理。
- 文本兜底检测中因 `failureKeywords` 误杀 → 已强制 success/failure
  文本先于弹窗事件；轮询路径下“上次任务失败”也不会误杀当前任务。
- “乱码”长串 → Kotlin `String.take()` 已经是字符级截断；本次同时收紧
  判定逻辑，避免不必要的长诊断。

## 开发与构建

```powershell
$env:JAVA_HOME = 'C:\Users\LX\.jdks\jbr-17.0.14'
cd D:\Projects\android-autoclock
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

ADB 安装：

```bash
/c/Users/LX/AppData/Local/Android/Sdk/platform-tools/adb.exe install -r \
  /d/Projects/android-autoclock/app/build/outputs/apk/debug/app-debug.apk
```

GitHub 推送（直连失败时使用 SOCKS5 代理）：

```bash
cd /d/Projects/android-autoclock
git -c http.proxy=socks5h://100.99.78.35:1080 \
    -c https.proxy=socks5h://100.99.78.35:1080 \
    push
```

## 文档清单

- `README.md`：项目主文档，最新检测策略在此。
- `CONTRIBUTING.md`（本文件）：开发交接与最近一次修复说明。
- `CLAUDE.md`：项目级 Claude 指令。
- `android-autoclock 后续开发交接文档.md`：历史开发记录与排查总结。
- `README_CURRENT_VERSION_DRAFT.md`：当前版本说明草稿。
- `PROJECT_SCAN.md`：项目扫描与归档结论。
