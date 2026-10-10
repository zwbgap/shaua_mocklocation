# 影梭 (MockLocation) 变更与部署指南

> 包名：`com.shaua.mocklocation` | 版本：1.0.0 | 更新日期：2026-10-10

---

## 一、现有功能点

### 核心功能
| 功能 | 说明 | 状态 |
|---|---|---|
| 模拟定位 | 通过 `LocationManager` 同时注入 GPS + NETWORK Provider，无需 ROOT | ✅ |
| 百度地图选点 | 地图展示、单击/长按/双击交互、普通/卫星图切换 | ✅ |
| POI 逆地理编码 | 长按地图弹出地址信息（保存/复制/分享/传送） | ✅ |
| POI 联想搜索 | 百度搜索建议，支持全国/城市搜索 | ✅ |
| 手动输入坐标 | 支持 BD09 / WGS84 坐标系输入 | ✅ |
| 运行中更新位置 | 模拟运行时选新点，位置实时更新 | ✅ |
| 收藏位置 | SQLite 存储收藏地点，支持增删改查、搜索、有效期归档 | ✅ |
| 随机偏移 | 仅对收藏位置生效，经纬度最大偏移可配置 | ✅ |

### 辅助功能
| 功能 | 说明 | 状态 |
|---|---|---|
| 海拔高度配置 | 设置项可配置模拟定位的海拔 | ✅ |
| 版本检测 | 检查 GitHub Releases 最新版本并下载安装 | ✅ |
| 用户协议/隐私 | 首次启动弹窗，同意后进入 | ✅ |
| 权限管理 | 运行时权限申请（定位、存储等） | ✅ |
| 日志记录 | XLog 写入文件，支持开关 | ✅ |

### 已移除功能
| 功能 | 说明 |
|---|---|
| 悬浮摇杆 | 圆盘摇杆 + 八方向按键摇杆（控制移动） |
| 模拟移动 | 步行/跑步/自行车速度切换 |
| 方向传感器 | 地图定位图层方向显示 |

---

## 二、今日变更（2026-10-10）

### 2.1 架构重构

| 变更项 | 说明 |
|---|---|
| 移除悬浮摇杆模块 | 删除 `JoyStick`、`RockerView`、`ButtonView` 及相关布局、资源 |
| `ServiceGo` → `MockLocationService` | 移除摇杆初始化、监听器、通知栏摇杆按钮，保留纯定位注入核心 |
| 引入 MVVM 架构 | 新增 `MainViewModel`、`LocationRepository`、`FavoriteLocationDao` |
| 数据层重构 | `DataBaseHistoryLocation` → `FavoriteLocationDao`，表名改为 `FavoriteLocation` |
| `MainActivity` 精简 | 移除方向传感器、悬浮窗权限检查，集成 ViewModel |

### 2.2 Bug 修复

| 修复项 | 说明 |
|---|---|
| 搜索失败 | `SuggestionSearchOption.city()` 在 `mCurrentCity` 为 null 时不传 city，改为全国搜索 |
| 坐标转换错误 | `MapUtils.wgs2bd09(lat, lng)` 参数顺序颠倒，修正为 `wgs2bd09(lng, lat)` |

### 2.3 包名重命名

| 项目 | 旧值 | 新值 |
|---|---|---|
| 包名 | `com.zcshou.gogogo` | `com.shaua.mocklocation` |
| namespace | `com.zcshou.gogogo` | `com.shaua.mocklocation` |
| applicationId | `com.zcshou.gogogo` | `com.shaua.mocklocation` |
| 目录结构 | `com/zcshou/gogogo/` | `com/shaua/mocklocation/` |

### 2.4 签名证书重新配置

| 项目 | 旧值 | 新值 |
|---|---|---|
| 证书文件 | `keystore/GoGoGo.jks` | `keystore/mocklocation.jks` |
| Alias | `GoGoKey` | `mocklocation` |
| 密码 | 硬编码于 build.gradle | 移至 `local.properties`（不入库） |
| SHA1 | `32:2F:E6:05:...` | 已重新生成（见 `local.properties`，不公开） |
| 安全码 | — | `SHA1;com.shaua.mocklocation`（在百度地图控制台配置） |

### 2.5 设置页精简

移除：摇杆类型、步行/跑步/自行车速度
保留：海拔高度、随机偏移、日志开关、收藏有效期、版本、作者

### 2.6 代码优化

- `app_author`: `ZCShou` → `shaua`
- 删除未使用的 `arrays.xml`（`array_map_type`）
- `location_input.xml` 中 `joystick_longitude/latitude` → `input_longitude/latitude`

### 2.7 文件清理

删除：`.gradle/`、Eclipse 配置文件（`.settings/`、`.classpath`、`.project`）

---

## 三、项目结构

```
GoGoGo/
├── app/
│   ├── libs/                          # 百度地图 SDK（jar + arm64-v8a .so）
│   ├── src/main/
│   │   ├── java/com/shaua/mocklocation/
│   │   │   ├── data/                  # 数据层（Model + DAO + Repository）
│   │   │   ├── database/              # 搜索历史数据库
│   │   │   ├── service/               # MockLocationService（定位注入）
│   │   │   ├── utils/                 # 工具类（GoUtils/MapUtils/ShareUtils）
│   │   │   ├── MainActivity.java      # 主界面（地图 + 选点 + 启动定位）
│   │   │   ├── MainViewModel.java     # ViewModel（LiveData 状态）
│   │   │   ├── HistoryActivity.java   # 收藏位置列表
│   │   │   ├── SettingsActivity.java  # 设置页
│   │   │   ├── WelcomeActivity.java   # 协议 + 权限
│   │   │   └── ...
│   │   ├── res/                       # 资源文件
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── gradle/wrapper/                    # Gradle Wrapper（gradle-wrapper.jar + properties）
├── keystore/
│   └── mocklocation.jks               # 签名证书
├── docs/
│   ├── CHANGELOG.md                   # 本文档
│   └── build-log.md                   # 构建日志
├── local.properties                   # AK + 签名配置（不入库）
├── build.gradle / settings.gradle / gradle.properties
├── gradlew / gradlew.bat
└── README.md
```

---

## 四、迁移到另一台设备的步骤

### 4.1 打包项目

将整个 `GoGoGo/` 目录复制到新设备，确保包含以下关键文件：

```
必须包含：
  gradlew, gradlew.bat
  gradle/wrapper/gradle-wrapper.jar
  gradle/wrapper/gradle-wrapper.properties
  build.gradle, settings.gradle, gradle.properties
  app/build.gradle
  app/src/          （全部源码和资源）
  app/libs/         （百度地图 SDK）
  keystore/mocklocation.jks
  local.properties  （AK + 签名配置）
  proguard-rules.pro
```

### 4.2 新设备环境要求

| 依赖 | 版本要求 |
|---|---|
| JDK | 17 或更高 |
| Android SDK | platform 32（Android 12L）、build-tools 36.0.0 |
| Gradle | 8.13（由 Wrapper 自动下载，无需手动安装） |
| Android Gradle Plugin | 8.12.1（build.gradle 中声明） |
| 网络 | 首次构建需下载依赖（Maven Central + Google） |

### 4.3 构建步骤

```powershell
cd GoGoGo

# 1. 构建 debug APK
.\gradlew.bat assembleDebug

# 输出路径：app/build/outputs/apk/debug/Go_1.0.0_arm64-v8a_debug.apk
```

### 4.4 真机安装与配置

1. **安装 APK**：
   ```powershell
   adb install -r app\build\outputs\apk\debug\Go_1.0.0_arm64-v8a_debug.apk
   ```

2. **开启模拟位置权限**（必需）：
   - 设置 → 开发者选项 → 选择模拟位置信息应用 → 选择「影梭」

3. **开启定位服务**：
   - 设置 → 位置信息 → 开启

4. **授予权限**：
   - 首次启动同意协议和隐私政策
   - 授予定位权限

---

## 五、测试验收清单

### 5.1 基础功能测试

| 测试项 | 预期结果 | 状态 |
|---|---|---|
| 地图底图显示 | 百度地图正常加载，无白屏 | ☐ |
| 定位权限申请 | 首次启动弹出定位权限请求 | ☐ |
| 选点交互 | 单击/长按/双击地图有响应 | ☐ |
| 逆地理编码 | 长按地图弹出地点名称和地址 | ☐ |
| POI 搜索 | 输入关键词返回搜索结果列表 | ☐ |
| 手动输入坐标 | 输入经纬度后地图定位到该点 | ☐ |

### 5.2 模拟定位测试

| 测试项 | 预期结果 | 状态 |
|---|---|---|
| 启动模拟定位 | 点击传送后系统定位变为选定位置 | ☐ |
| 运行中更新位置 | 模拟运行中选新点，位置实时更新 | ☐ |
| 停止模拟定位 | 点击停止后恢复真实定位 | ☐ |
| GPS + NETWORK | 两个 Provider 均被成功 mock | ☐ |
| 通知栏 | 显示「影梭服务正在运行中」 | ☐ |

### 5.3 收藏功能测试

| 测试项 | 预期结果 | 状态 |
|---|---|---|
| 保存位置 | 长按地图 → 保存，位置存入收藏 | ☐ |
| 查看收藏 | 导航菜单 → 收藏位置，显示列表 | ☐ |
| 选择收藏 | 点击收藏项，地图定位到该位置 | ☐ |
| 删除收藏 | 长按收藏项可删除 | ☐ |
| 随机偏移 | 开启后收藏位置有随机偏移 | ☐ |

### 5.4 设置功能测试

| 测试项 | 预期结果 | 状态 |
|---|---|---|
| 海拔高度 | 修改后模拟定位海拔变化 | ☐ |
| 随机偏移开关 | 开关状态正确保存和生效 | ☐ |
| 日志开关 | 关闭后不再写入日志文件 | ☐ |
| 收藏有效期 | 过期的收藏自动归档 | ☐ |

---

## 六、已知限制与后续优化

### 已知限制

1. **targetSdk = 32**：已过期（Google Play 要求 ≥ 34），如需上架需升级
2. **仅支持 arm64-v8a**：不支持 32 位设备
3. **存储权限**：使用 `requestLegacyExternalStorage`，未适配 Scoped Storage
4. **`local.properties` 不入库**：新设备需自行配置（已随项目复制）

### 后续优化方向

| 优先级 | 优化项 | 说明 |
|---|---|---|
| 高 | 升级 targetSdk 到 34 | 适配 Android 14，处理前台服务类型声明 |
| 高 | 适配 Scoped Storage | 移除 `requestLegacyExternalStorage` |
| 中 | 拆分 MainActivity | 引入 ViewModel + Repository 后可进一步拆分 UI 逻辑 |
| 中 | 补充单元测试 | 为 `MapUtils` 坐标转换、`GoUtils` 工具方法编写测试 |
| 低 | 多 ABI 支持 | 增加 `armeabi-v7a`、`x86_64` 支持 |
| 低 | 国际化 | 补充 `values-en/strings.xml` |

---

## 七、关键技术信息

### 百度地图 SDK

- **AK**：已配置在 `local.properties` 的 `MAPS_API_KEY`（不公开）
- **安全码**：`SHA1;com.shaua.mocklocation`（在百度地图控制台配置）
- **SHA1**：签名证书的 SHA1（见 `local.properties`，不公开）
- **包名**：`com.shaua.mocklocation`
- **SDK 版本**：BaiduLBS_Android v7.6.5

### 模拟定位原理

1. `LocationManager.addTestProvider()` 添加 GPS 和 NETWORK Provider
2. `setTestProviderEnabled()` 启用
3. `setTestProviderLocation()` 每 100ms 推送一次 Location（HandlerThread 驱动）
4. Location 需设置：`latitude`、`longitude`、`altitude`、`accuracy`、`bearing`、`speed`、`time`、`elapsedRealtimeNanos`

### 坐标系转换

- **BD09**（百度地图）→ **GCJ02**（火星坐标）→ **WGS84**（GPS 原始坐标，用于注入系统）
- `MapUtils.java` 实现 `bd092gcj`、`gcj2wgs`、`wgs2gcj`、`gcj2bd09` 转换

---

## 八、常见问题

**Q: 地图显示白屏？**
A: 检查 AK 是否为 Android SDK 类型，SHA1 和包名是否与百度控制台一致。

**Q: 模拟定位不生效？**
A: 检查「开发者选项 → 选择模拟位置信息应用」是否选择了「影梭」。

**Q: 搜索无结果？**
A: 检查网络连接，确认百度地图 AK 已开通 POI 搜索权限。

**Q: 构建失败提示签名错误？**
A: 确认 `local.properties` 中的签名配置与 `keystore/mocklocation.jks` 一致。
