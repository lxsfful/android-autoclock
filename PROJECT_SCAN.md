# android-autoclock 项目扫描卡片

> 扫描日期：2026-06-19  
> 项目路径：`D:\Projects\android-autoclock`  
> 当前状态：单元测试 / Lint / Debug 构建全绿，response decision 修复
> 已部署到设备 `8KE5T19801026837`（华为 ELE-AL00）。

## 1. 项目定位

`android-autoclock` 是一个 Android/Kotlin 自动化辅助应用，核心能力是通过 Android AccessibilityService 在指定时间窗口内执行用户配置的手势流程，用于在固定设备、固定桌面布局、固定目标 App 流程下完成自动打卡动作。

项目不是通用自动化框架，而是面向特定实机环境的稳定执行工具。

## 2. 技术栈

- 平台：Android
- 语言：Kotlin
- 构建：Gradle / Android Gradle Plugin 8.2.2
- Kotlin 插件：1.9.22
- compileSdk：34
- minSdk：26
- targetSdk：34
- UI：AndroidX、AppCompat、Material Components、ConstraintLayout、ViewBinding
- 通知能力：JavaMail for Android
- 本地存储：SharedPreferences
- 敏感信息保护：Android Keystore + AES/GCM

## 3. 顶层结构

```text
D:\Projects\android-autoclock
├── app/                         # Android 应用主模块
├── gradle/                      # Gradle Wrapper
├── .github/                     # Issue 模板等 GitHub 配置
├── README.md                    # 项目说明
├── android-autoclock 后续开发交接文档.md
├── CLAUDE.md
├── CONTRIBUTING.md
├── SECURITY.md
├── CODE_OF_CONDUCT.md
├── LICENSE
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew / gradlew.bat
├── setup.sh
└── ChatGPT Image 2026年6月16日 13_54_45.png
```

## 4. 核心源码结构

```text
app/src/main/java/com/autoclock/
├── MainActivity.kt              # 主界面、权限检查、配置入口、历史显示、邮件设置
├── AutoClockService.kt          # 无障碍服务核心：点击、等待、检测、收尾流程
├── AlarmScheduler.kt            # 精确闹钟调度与下次触发时间计算
├── AlarmReceiver.kt             # 闹钟触发入口，并重新调度下一轮任务
├── BootReceiver.kt              # 开机后重新调度
├── ClockSuccessDetector.kt      # 目标 App 响应窗口/文本检测
├── Prefs.kt                     # 配置、坐标、时间窗口、历史、邮件密钥存储
├── CoordinatePickerActivity.kt  # 通过截图点选坐标
├── CoordinateMapper.kt          # 将截图点击位置映射为屏幕比例坐标
├── ClockHistory.kt              # 本地历史记录追加、读取、清理
├── EmailSender.kt               # SMTP 邮件通知
├── ForegroundAppChecker.kt      # 目标 App 是否进入前台的判断
├── TargetApps.kt                # 目标 App 包名集中配置
└── UsageGuideActivity.kt        # 使用说明页
```

## 5. 当前主流程

```text
系统闹钟触发
  ↓
AlarmReceiver.onReceive()
  ↓
AutoClockService.performClockSequence()
  ↓
唤醒屏幕并检查是否锁屏
  ↓
返回桌面
  ↓
点击用户配置的目标 App 桌面快捷方式坐标
  ↓
等待目标 App 进入前台
  ↓
点击用户配置的任务按钮坐标
  ↓
等待目标 App 响应弹窗 / 文本变化
  ↓
记录成功或失败
  ↓
可选发送邮件通知
  ↓
延迟后返回桌面
  ↓
尝试结束目标 App 进程
  ↓
点击用户配置的任务后快捷方式坐标，例如向日葵
  ↓
释放 WakeLock，结束本轮序列
```

## 6. 已完成成果

### 6.1 自动化打卡主链路

已实现从定时触发、唤醒、回桌面、打开目标 App、点击任务按钮、检测响应、收尾回到后续 App 的完整链路。

### 6.2 双时间窗口与随机化调度

支持开始任务和结束任务两个时间窗口，并在窗口内随机生成触发时间。`AlarmScheduler.nextWeekdayTime()` 已加入兜底逻辑，确保缓存的下一次任务时间不会落在当前时间之前。

### 6.3 周末跳过

调度逻辑会跳过周六、周日，进入下一个工作日。

### 6.4 无障碍服务限定目标包名

`accessibility_service_config.xml` 中通过 `android:packageNames="com.kdweibo.client"` 限定目标 App；`collectActiveWindowTexts()` 也会再次确认当前窗口包名，避免读取非目标 App 文本。

### 6.5 响应检测逻辑升级

成功判断已统一收敛到 `ClockSuccessDetector.detectResponse(snapshot)`：

- 主检测：目标窗口出现“操作成功”“打卡成功”“已打卡”“已签到”
  等明确成功文案 → 成功；
- 次检测：目标窗口出现“失败”“不成功”“请稍后重试”等明确失败
  文案 → 失败；
- 兜底：目标包 `com.kdweibo.client` 出现 `TYPE_WINDOW_STATE_CHANGED`，
  且 `className` 命中 `Dialog` / `Popup` / `Toast` 等弹窗类（且不等于
  `SmartAttendHomeActivity`）→ 成功；心灵鸡汤类弹窗按成功处理；
- 轮询：事件路径与轮询路径共用同一套规则。

这样既能兼容“操作成功”之外的随机弹窗文案，又避免把普通 Activity
跳转或历史失败文案误判为成功。

### 6.6 坐标选择工具

提供 `CoordinatePickerActivity`，可以选择手机截图并点击截图位置，自动转换为比例坐标，降低手动填写坐标的成本。

### 6.7 历史记录

`ClockHistory` 支持记录成功/失败结果，按时间倒序保存，并保留最近 30 天。

### 6.8 邮件通知

支持 SMTP 成功、失败、测试邮件通知。SMTP 授权码不再明文保存，而是使用 Android Keystore 加密存储。

### 6.9 图标与文档

已完成应用图标替换，并保留 adaptive icon 相关资源。项目已有 README、贡献指南、安全说明、后续开发交接文档。

## 7. 测试情况

最新已执行命令：

```powershell
$env:JAVA_HOME='C:\Users\LX\.jdks\jbr-17.0.14'
cd D:\Projects\android-autoclock
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

测试结果：

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

历史“40 tests completed, 2 failed”已修复：原失败用例是基于“必须有
操作成功文字”的旧规则编写，与新的“窗口事件 + 文本兜底”规则不一致，
已在 `ClockSuccessDetectorTest` 中替换为新规则对应的用例。

## 8. Git 状态

当前分支：

```text
fix/real-autoclock-flow...origin/fix/real-autoclock-flow
```

近期提交：

```text
9712046 fix: detect cloud punch response windows
41ba9ae feat: 改用 TYPE_WINDOW_STATE_CHANGED 检测打卡弹窗，兼容心灵鸡汤等任意响应窗口
34d6507 fix: 图标改用原图直接缩放，保留自然白边（~15%）
4de8678 feat: 更新图标、修复调度日期显示、修正打卡后流程
c7e78d3 chore: update autoclock docs features and LX series app icon
3c7b9f2 docs: 更新文档至v1.3，反映弹窗事件检测和秒数随机化
```

最新一次提交（`9712046`）：

- 引入 `ClockResponseDecision` 枚举和 `ClockSuccessDetector.detectResponse(...)`
  统一响应判定；
- 明确成功/失败文本优先于弹窗事件；
- 弹窗类窗口（`Dialog` / `Popup` / `Toast`）作为成功兜底；
- 轮询路径下历史失败文案（无弹窗表面）不再误杀当前任务；
- `AutoClockService` 移除私有 `ClockResponseResult`，统一接入新判定；
- `README.md` 新增“快捷打卡响应检测策略”章节；
- 单元测试全绿。

扫描时存在未跟踪文件：

```text
ChatGPT Image 2026年6月16日 13_54_45.png
android-autoclock 后续开发交接文档.md
AGENTS.md
README_CURRENT_VERSION_DRAFT.md
PROJECT_SCAN.md
```

本扫描新增：

```text
PROJECT_SCAN.md
```

## 9. 质量观察

### 9.1 优点

- 核心职责拆分比较清楚：调度、接收、无障碍执行、配置、历史、邮件通知分别在不同文件中。
- 目标包名集中在 `TargetApps.kt`，并与无障碍 XML 配置保持同步要求。
- 读取窗口文本前做了目标包名二次校验，隐私边界意识较好。
- 邮件授权码使用 Android Keystore 加密，相比明文 SharedPreferences 更安全。
- 单元测试覆盖较全面，最近一次补充了弹窗类判定、活动跳转过滤、轮询路径失败语义等用例。
- 成功检测逻辑收敛到 `ClockSuccessDetector.detectResponse(...)`，事件路径与轮询路径共用同一套规则。
- README、SECURITY、CONTRIBUTING、交接文档比较完整。

### 9.2 风险与短板

#### 弹窗类响应事件仍有误判可能

当前策略把目标包的 `Dialog` / `Popup` / `Toast` 弹窗类窗口变化视为成功。
这符合当前实机观察，但可能误判以下情况：

- 版本升级弹窗
- 权限提示弹窗
- 公告类弹窗
- 网络异常但未命中失败关键词

后续可通过弹窗文本/类名白名单进一步收紧。

#### `killBackgroundProcesses` 受系统限制

`AutoClockService.killTargetApp()` 使用：

```kotlin
activityManager.killBackgroundProcesses(TARGET_APP_PACKAGE)
```

在普通第三方 App 环境中，强制停止其他应用可能受系统权限限制。若当前实机可用，可以保留；但它不是所有 Android 设备上的稳定能力。

#### 坐标点击依赖环境稳定

该项目的有效性依赖固定设备、固定桌面布局、固定分辨率/缩放、固定目标 App 流程。桌面图标移动、系统缩放变化、目标 App UI 变化都可能影响结果。

#### App 名称仍带 Sample

`strings.xml` 中：

```xml
<string name="app_name">AutoClock Sample</string>
<string name="accessibility_service_label">AutoClock Sample Service</string>
```

如果这个版本作为正式自用版本归档，建议后续改成更正式的名称。

## 10. README 状态

项目已有 `README.md`，内容较完整，最近一次同步增加了：

- 介绍里补上“检测云之家响应窗口或明确成功文本”；
- 新增“快捷打卡响应检测策略”章节；
- 架构说明里更新 `ClockSuccessDetector.kt` 的注释；
- Accessibility/隐私章节同步说明窗口事件是主信号。

## 11. 建议后续处理

按优先级排序：

1. 增加本地日志导出能力，记录每次触发、坐标、前台包名、事件类型、成功/失败原因。
2. 进一步收紧成功判定状态机，只在“点击打卡按钮后的短时间窗口”内接受响应事件。
3. 弹窗文本/类名白名单，避免升级/权限弹窗被误判。
4. 如作为正式版本归档，修改 `app_name` 和无障碍服务名称，去掉 `Sample`。
5. 根据实机情况确认 `killBackgroundProcesses` 是否真的生效；若不稳定，增加兜底策略。

## 12. 当前归档结论

`android-autoclock` 已形成一个功能闭环完整的 Android 自动化项目：

- 能配置定时任务；
- 能通过无障碍服务执行手势；
- 能基于窗口事件 + 文本兜底检测目标 App 响应；
- 能记录历史；
- 能发送邮件通知；
- 能在任务后返回指定后续 App；
- 单元测试 / Lint / Debug 构建全绿；
- 有较完整文档和较新测试覆盖。

最新 commit `9712046` 已经将历史上“响应被误判为失败”的问题修复，并
部署到设备 `8KE5T19801026837` 上。
