# android-autoclock 当前版本说明草稿

> 本文件是对现有 `README.md` 的补充草稿，不直接替代原 README。  
> 适合后续合并到 README 的“当前版本状态 / 验收说明 / 已知问题”章节。

## 当前版本状态

当前版本已经形成可用闭环：

- 支持配置开始任务、结束任务两个时间窗口；
- 支持在时间窗口内随机触发，且跳过周末；
- 支持通过 AccessibilityService 执行返回桌面、点击快捷方式、点击任务按钮等手势；
- 支持等待目标 App 响应窗口，并基于窗口事件或文本内容判断任务结果；
- 支持任务完成后返回桌面、清理目标 App、点击任务后快捷方式；
- 支持本地历史记录；
- 支持成功、失败、测试邮件通知；
- SMTP 授权码使用 Android Keystore 加密保存；
- 已更新应用图标和 adaptive icon 资源。

## 当前主流程

```text
定时闹钟触发
  ↓
检查无障碍服务实例
  ↓
唤醒屏幕
  ↓
返回桌面
  ↓
点击目标 App 桌面快捷方式
  ↓
等待目标 App 进入前台
  ↓
点击任务按钮
  ↓
等待目标 App 响应弹窗或文本变化
  ↓
记录成功 / 失败
  ↓
可选发送邮件
  ↓
返回桌面
  ↓
尝试结束目标 App
  ↓
点击任务后快捷方式，例如向日葵
  ↓
释放 WakeLock
```

## 当前版本重点改动

### 1. 调度时间修复

`AlarmScheduler.nextWeekdayTime()` 已增加兜底逻辑，保证下次任务时间不会落在当前时间之前。

### 2. 响应检测升级（最新）

成功检测不再只依赖“操作成功”等固定文本，而是统一通过
`ClockSuccessDetector.detectResponse(snapshot)` 判定：

1. 目标窗口出现“操作成功”“打卡成功”“已打卡”“已签到”等明确
   成功文案 → 成功；
2. 目标窗口出现“失败”“不成功”“请稍后重试”等明确失败文案
   → 失败；
3. 目标包 `com.kdweibo.client` 出现 `TYPE_WINDOW_STATE_CHANGED`，
   且 `className` 命中 `Dialog` / `Popup` / `Toast` 等弹窗类（且不等于
   `SmartAttendHomeActivity`） → 成功；心灵鸡汤类弹窗按成功处理；
4. 其他情况 → 未知，继续等待。

这样既兼容目标 App 随机弹出的非固定文案，又避免把普通 Activity 跳转
或历史失败文案误判为成功。

### 3. 任务后流程修正

任务完成后执行：

```text
返回桌面 → 结束目标 App 后台进程 → 优先通过包名打开向日葵 → 失败时坐标兜底
```

### 4. 图标更新

已替换应用图标，并保留 Android 8.0+ adaptive icon 所需资源。

## 测试说明

已运行：

```powershell
$env:JAVA_HOME='C:\Users\LX\.jdks\jbr-17.0.14'
cd D:\Projects\android-autoclock
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

结果：

- 单元测试：全绿
- Lint：通过
- Debug APK：构建成功，输出
  `app/build/outputs/apk/debug/app-debug.apk`

`ClockSuccessDetectorTest` 已补充 8 条新用例覆盖：

- 弹窗类窗口状态变化 → 成功
- `SmartAttendHomeActivity` 窗口变化 → 未知
- 目标包普通 Activity 窗口变化 → 未知
- 弹窗窗口变化 + 明确失败文案 → 失败
- 内容变化事件 + 明确成功/失败文案 → 成功/失败
- 中性内容变化文本 → 未知
- 轮询路径下历史失败文案（无弹窗表面）→ 未知

## 已知风险

1. 弹窗类窗口事件是兜底信号，若云之家在打卡后弹出版本升级、权限、
   公告等弹窗，仍可能被识别为成功；后续可通过弹窗文本/类名白名单
   进一步收紧。
2. 坐标点击依赖固定设备、固定桌面布局和固定目标 App 页面。
3. `ActivityManager.killBackgroundProcesses` 在不同 Android 设备上可能
   受权限限制。
4. 当前应用名称仍包含 `Sample`，如果正式归档可考虑改名。
5. 实机响应行为需要在真实设备上再做一次端到端验证。

## 建议后续优化

1. 增加本地日志面板或日志导出功能，方便实机排查。
2. 增强成功判定状态机，只在点击打卡后的短时间窗口内接受响应事件。
3. 失败恢复机制：超时后截图、连续失败统计、失败后自动回向日葵。
4. 减少坐标依赖：按文字/包名查找桌面图标、按 AccessibilityNode
   查找打卡按钮。
5. 关注 C 盘空间与开发环境缓存，必要时把 Gradle/Android 缓存迁移到
   D 盘。
