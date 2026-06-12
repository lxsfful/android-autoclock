# android-autoclock

**Version:** 1.3 | **Platform:** Android | **Stack:** Kotlin, AndroidX, Gradle 8.4

## What

Android AccessibilityService 自动化打卡应用。调度手势序列，检测目标应用弹窗事件，记录历史，可选邮件通知。

## Commands

```bash
./gradlew assembleDebug    # 构建 debug APK
./gradlew installDebug     # 构建并安装到设备
./gradlew testDebugUnitTest # 运行单元测试
./gradlew lintDebug        # 代码检查
```

## Architecture

```text
app/src/main/java/com/autoclock/
├── MainActivity.kt          # 主界面、权限、设置
├── AutoClockService.kt      # 无障碍服务，执行手势和弹窗事件检测
├── AlarmScheduler.kt        # 精确闹钟调度（分钟+秒数随机化）
├── ClockSuccessDetector.kt  # 弹窗事件检测打卡成功
├── Prefs.kt                 # SharedPreferences 配置
└── CoordinateMapper.kt      # 坐标比例映射
```

## Environment

```bash
# Windows PowerShell
$env:JAVA_HOME = "C:\Users\LX\.jdks\jbr-17.0.14"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

- **Android SDK**: `C:\Users\LX\AppData\Local\Android\Sdk`
- **ADB**: `$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe`

## Configuration

在 `AutoClockService.kt` 中配置：
- `TARGET_APP_PACKAGE`: 目标应用包名（如云之家）
- `SUNFLOWER_PACKAGE`: 向日葵包名（可选）

其他配置通过应用内界面设置。

## Rules

- 隐私优先：不在公开文档中暴露私有路径、邮箱、设备ID
- 保持 MIT 许可证和无障碍服务声明
- 未经要求不改变应用行为
