# AutoClock 开发计划

## 现状分析

| 项目 | 现状 |
|------|------|
| 使用说明模块 | ✅ 已完成（`UsageGuideActivity.kt` + `activity_usage_guide.xml` 已存在，按钮已接入） |
| 打卡后杀云之家进程 | ⚠️ 已有 `killTargetApp()`，但无延时，需加 2s 延迟确保弹窗完全出现后再杀 |
| UI 配色优化 | ❌ 当前为默认 Material 紫色/青色主题，需改为清新淡雅风格 |
| 提交并开源到 GitHub | ❌ 有 10 个修改文件 + 6 个新文件未提交，需 commit + push |
| 编译 APK | ❌ 需执行 `gradlew assembleRelease` |

---

## Task 1：打卡成功后延迟 2s 再杀云之家进程

**目标**：检测到"操作成功"后等 2 秒再 force-stop，避免弹窗残留干扰下次打卡。

**改动文件**：`app/src/main/java/com/autoclock/AutoClockService.kt`

**方案**：
- 在 `completeSequence()` 方法中，`success == true` 分支里，将 `killTargetApp()` 改为 `handler.postDelayed({ killTargetApp() }, 2000L)`
- 同步更新 `sequenceWakeLockTimeoutMs()` 中的超时计算，增加 2s 余量（`+ 2_000L`），防止 WakeLock 在杀进程前释放

**验证**：单元测试不覆盖此逻辑（涉及 Handler 延迟），通过手动测试或代码审查确认。

---

## Task 2：UI 配色清新淡雅优化

**目标**：将默认紫色 Material 主题替换为清新淡雅风格。

**改动文件**：
- `app/src/main/res/values/colors.xml` — 新配色方案
- `app/src/main/res/values/themes.xml` — 使用新颜色
- `app/src/main/res/layout/activity_main.xml` — 状态文字颜色、分隔线颜色等微调
- `app/src/main/res/layout/activity_usage_guide.xml` — 标题颜色同步

**配色方案**（清新淡雅）：
| 用途 | 色值 | 说明 |
|------|------|------|
| Primary | `#5B9BD5` | 柔和蓝，主色调 |
| PrimaryVariant | `#3A7CC0` | 深蓝，用于状态栏 |
| OnPrimary | `#FFFFFF` | 白色文字 |
| Secondary | `#81C784` | 柔和绿，辅助色（成功状态） |
| Background | `#F5F7FA` | 浅灰蓝背景 |
| Surface | `#FFFFFF` | 卡片白 |
| OnSurface | `#333333` | 深灰文字 |
| Accent/Info | `#5B9BD5` | 信息提示 |
| Error | `#E57373` | 柔和红（错误状态） |
| Divider | `#E8ECF1` | 浅灰分隔线 |

**具体调整**：
1. `colors.xml`：替换所有颜色定义，新增 `background`、`surface`、`on_surface`、`divider`、`success`、`error` 等
2. `themes.xml`：改用 `Theme.MaterialComponents.Light.NoActionBar` 作为父主题（去掉默认 ActionBar，改用自定义标题栏更灵活），或保持 `DayNight.DarkActionBar` 但更新颜色
3. `activity_main.xml`：状态文字颜色从硬编码 `#1976D2` / `#444444` / `#666666` 改为引用 color 资源；分隔线颜色更新
4. `activity_usage_guide.xml`：章节标题颜色同步更新

**验证**：在模拟器或真机上查看界面，确认配色统一、文字可读。

---

## Task 3：提交所有修改并开源到 GitHub

**目标**：将当前所有未提交的修改 commit 并 push 到 GitHub。

**步骤**：
1. `git add` 所有修改和新增文件（排除敏感文件如 `.env`）
2. 创建 commit，消息如：`feat: add usage guide, delay kill, polish UI`
3. 创建 GitHub 仓库（如尚不存在）并 push

**注意**：
- 确认 `.gitignore` 已排除 `.env`、`local.properties`、build 产物
- 确认无敏感信息（SMTP 密码等）被提交
- 如需用户授权 GitHub 操作，提示用户执行 `gh auth login` 或提供 token

---

## Task 4：编译 Release APK

**目标**：生成可安装的 APK 文件供测试。

**步骤**：
1. 执行 `./gradlew assembleRelease`（或 `assembleDebug` 如未配置签名）
2. APK 输出路径：`app/build/outputs/apk/release/app-release-unsigned.apk`
3. 如需签名，使用 debug 签名即可用于测试

**验证**：APK 文件存在且大小合理（通常 5-20MB）。

---

## 执行顺序

```
Task 1 (杀进程延迟) → Task 2 (UI优化) → Task 3 (Git提交+GitHub) → Task 4 (编译APK)
```

Task 1 和 Task 2 可以并行开发，但建议先完成代码改动再统一提交。

---

## 风险与注意事项

- **云之家包名**：当前 `ClockSuccessDetector.kt` 中 `CLOCK_APP_PACKAGE = "com.example.targetapp"` 是占位符，用户需替换为真实包名（如 `com.yunzhijia.cloud`）。这不是本次计划范围，但需提醒用户。
- **GitHub 授权**：push 到 GitHub 需要用户已配置 `gh` CLI 或 git credential，否则需用户授权。
- **ProGuard**：release 构建未启用 minify，暂无混淆风险。
