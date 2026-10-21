# 影梭 (MockLocation) 变更与部署指南

> 包名：`com.shaua.mocklocation` | 版本：1.0.7 | 更新日期：2026-10-21

---

## 二、版本变更记录

### v1.0.7（2026-10-21）

#### CI 自动发布与安全加固
| 变更项 | 说明 |
|---|---|
| CI 发布调通 | GitHub Actions 工作流完成端到端验证：推送 `v*` tag 自动构建签名 APK 并创建 Release 上传；CI 签名证书与本地完全一致（SHA-1 已比对），百度地图 AK 校验不受影响 |
| 安全加固 | secrets-gradle-plugin 默认将 local.properties 全部键注入 BuildConfig（会打进 APK，反编译可见），新增 `secrets.ignoreList` 排除签名四项与 sdk.dir，签名密码不再进入 APK |
| CI 修复 | workflow 的 local.properties 生成步骤补充 MAPS_API_KEY/MAPS_SAFE_CODE 字段，修复 CI 缺键导致 BuildConfig 非法表达式编译失败 |

#### 使用说明
| 变更项 | 说明 |
|---|---|
| 发版流程 | 后续版本发布只需：更新版本号与 CHANGELOG → 推送 main → 推送 `v*` tag，CI 自动完成构建与发布 |

### v1.0.6（2026-10-21）

#### 模拟拟真补全
| 变更项 | 说明 |
|---|---|
| 海拔/航向微波动 | 注入的 Location 海拔在基准值 ±3m、航向 ±10° 内缓慢随机漂移，补齐最后的恒定值特征（此前海拔/航向固定不变） |

#### 稳定性与工程质量
| 变更项 | 说明 |
|---|---|
| 崩溃日志自收集 | 新增 `CrashHandler`：崩溃报告写入独立文件（保留最近 5 份）并追加主日志，日志查看页与分享自动包含崩溃现场 |
| 单元测试 | 新增随机偏移、历史过期阈值、语义化版本比较的 JUnit 测试（12 例）；随机偏移逻辑提取至 `MapUtils.randomOffset` |
| 更新检查修复 | 版本判断由 `contains` 字符串包含改为语义化版本比较，修复 v1.0.10 被 v1.0.1 误判为最新的 bug |
| targetSdk 34 → 35 | Android 15 合规；主题新增 `windowOptOutEdgeToEdgeEnforcement` 暂保持原布局 |
| R8 规则补充 | 保留行号与源文件名属性，崩溃堆栈可对照 mapping.txt 还原 |
| CI 自动发布 | 新增 GitHub Actions 工作流（推送 `v*` tag 自动构建并创建 Release），需在仓库配置 keystore secrets 后启用 |
| 免责声明强化 | 用户协议明确"仅供开发调试用途"、第三方平台服务条款后果自担条款 |

#### 其他
| 变更项 | 说明 |
|---|---|
| 随机偏移默认开启 | 设置项默认值由关闭改为开启（仅对未手动改过该开关的安装生效） |

### v1.0.5（2026-10-21）

#### 模拟定位反检测增强
| 变更项 | 说明 |
|---|---|
| 隐藏 mock 标记 | 通过反射隐藏 Location 的 mock 标记，规避 `isFromMockProvider()`（API<31）/ `isMock()`（API31+）检测；Android 9+ 使用元反射绕过隐藏 API 限制 |
| GPS 信号拟真 | 精度 8~25m 随机波动、坐标缓慢随机游走（±11m）、卫星数 6~12 随机、垂直/方位/速度精度同步拟真，避免"精度恒 1m、坐标纹丝不动"的异常特征 |
| FUSED Provider 覆盖 | Android 12+ 同时向原生 `fused` Provider 注入模拟位置，覆盖融合定位读取路径（部分 ROM 禁止注册时自动降级） |
| NETWORK 拟真 | 网络定位精度 30~80m 波动，坐标偏移量按 GPS 游走量 2.5 倍联动 |

#### 使用提示优化
| 变更项 | 说明 |
|---|---|
| 服务通知提示 | 前台服务通知文案新增使用技巧：若目标应用显示真实位置，切换卫星图/普通图可刷新为模拟位置 |
| 启动提示 | 点击「穿越」启动模拟时的 Snackbar 同步展示切换图层技巧 |

> 说明：部分应用（如高德）走自研定位 SDK 的网络侧路径（基站/WiFi/IP → 服务端换算），完全绕过系统 LocationManager，客户端无法拦截。图层切换会重建"我的位置"图层并重读系统位置（拿到模拟坐标），且不触发服务端网络定位，实测可解决刷新后闪回真实位置的问题。

### v1.0.4（2026-10-12）

#### Material 3 主题重构
| 变更项 | 说明 |
|---|---|
| 全量 M3 语义化适配 | 所有硬编码颜色（`#212121`、`#9E9E9E` 等）替换为 Material 3 语义化属性（`colorOnSurface`、`colorOnSurfaceVariant`、`colorOutline` 等） |
| Drawable 资源适配 | 12 个 drawable 背景改用 `?attr/colorSurface`、`colorSurfaceVariant`、`colorOutlineVariant` 等主题属性 |
| 状态栏统一 | `statusBarColor` 改为 `colorPrimary`，消除白底割裂感，自动适配明暗模式图标颜色 |
| 系统导航栏适配 | `navigationBarColor` 改为 `colorSurface`，与底部导航栏融为一体 |
| 对话框主题 | `AppBarOverlay`/`PopupOverlay` 改为 Material 3 覆盖主题，支持昼夜模式 |

#### 底部导航栏优化
| 变更项 | 说明 |
|---|---|
| 高度调整 | 56dp → 80dp，图标文字不再错位 |
| 顶部分隔线 | 新增 `colorOutlineVariant` 顶部分隔线 |
| 系统导航栏适配 | 动态添加底部 padding，适配系统导航栏 inset |
| 搜索时隐藏 | 展开搜索时自动隐藏底部导航栏，扩大搜索区域 |

#### 应用图标切换
| 变更项 | 说明 |
|---|---|
| 5 款图标 | 极简定位针、指南针定位针、纸飞机定位针、科技坐标网格、默认图标 |
| 动态切换 | 通过 `activity-alias` + `PackageManager` 实现运行时切换，无需重启 |
| 设置入口 | 「设置 → 应用图标」选择，重启后生效 |

#### 缓存与内存优化
| 变更项 | 说明 |
|---|---|
| 搜索历史过期清理 | 启动时自动清理超过 30 天的搜索历史记录 |
| 更新 APK 残留清理 | 应用启动时自动清理 `Updates/` 目录下的旧安装包 |
| 一键清理缓存 | 设置页新增「清理缓存」，清理日志、搜索历史、地图瓦片缓存、残留安装包 |
| Release 日志精简 | Release 版本日志级别降为 `WARN`，移除边框/调用栈，减少日志体积 |

#### 弹窗 UI 样式统一
| 变更项 | 说明 |
|---|---|
| 主按钮样式统一 | 新增 `bg_btn_primary.xml`（主题色背景，6dp 圆角，扁平无 elevation） |
| 次按钮样式统一 | 4 个自定义弹窗（协议/隐私/更新/坐标输入）的两个按钮统一为相同样式 |
| Material Button tint 修复 | 添加 `backgroundTint="@null"`，解决 Material 默认 tint 覆盖自定义背景的问题 |
| 标题样式统一 | 标题字号统一 18sp，底部分隔线统一 `colorOutlineVariant` |

#### 16KB 页面兼容
| 变更项 | 说明 |
|---|---|
| 百度地图 SDK 对齐 | `extractNativeLibs="true"` + `useLegacyPackaging=true`，解决 libindoor.so 未按 16KB 对齐的问题 |

### v1.0.3（2026-10-12）

#### Material 3 主题适配
| 变更项 | 说明 |
|---|---|
| 主题重构 | 引入 Material 3 DayNight 主题，四套预设配色（青/蓝/紫/橙） |
| 颜色语义化 | 硬编码颜色替换为 M3 语义属性，支持深色模式 |
| 状态栏/导航栏 | 统一适配主题色，消除割裂感 |

### v1.0.2（2026-10-11）

#### 安全加固
| 变更项 | 说明 |
|---|---|
| 禁止应用备份 | `AndroidManifest.xml` 添加 `android:allowBackup="false"`，防止 ADB 备份泄露用户数据 |
| 网络安全配置 | 新增 `network_security_config.xml`，全局禁止明文流量（`cleartextTrafficPermitted="false"`） |
| FileProvider 收紧 | 移除 `provider_paths.xml` 中 `<external-path path=".">`（暴露整个 SD 卡），删除重复的 `cache-path` |
| 移除无用权限 | 删除 `READ_PHONE_STATE`（未实际使用）、`MOUNT_UNMOUNT_FILESYSTEMS`（未实际使用） |

#### 功能调整
| 变更项 | 说明 |
|---|---|
| 移除历史记录有效期设置 | 设置页不再提供有效期配置，固定为 7 天自动归档 |
| APK 命名 ASCII 化 | 输出文件名前缀 `迁境_` → `qianjing_`，解决 GitHub Release 上传时中文乱码 |

### v1.0.1（2026-10-11）

#### 界面与交互
| 变更项 | 说明 |
|---|---|
| 更新应用图标 | 青色背景 `#009688` + 白色定位针 + 信号波纹（表示虚拟定位） |
| 关于页重构 | 「反馈与更新」分类改名为「关于」；版本号并入检测更新摘要；开发者并入联系作者摘要 |
| 反馈流程优化 | 合并问题反馈与邮箱：点击反馈弹框提醒，确认后调邮件应用并自动附上日志文件 |

### v1.0.0（2026-10-10）— 首个版本

#### 架构重构

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
| Android SDK | platform 34（Android 14）、build-tools 36.0.0 |
| Gradle | 8.13（由 Wrapper 自动下载，无需手动安装） |
| Android Gradle Plugin | 8.12.1（build.gradle 中声明） |
| 网络 | 首次构建需下载依赖（Maven Central + Google） |

### 4.3 构建步骤

```powershell
cd GoGoGo

# 1. 构建 debug APK
.\gradlew.bat assembleDebug

# 输出路径：app/build/outputs/apk/debug/qianjing_1.0.4_arm64-v8a_debug.apk
```

### 4.4 真机安装与配置

1. **安装 APK**：
   ```powershell
   adb install -r app\build\outputs\apk\debug\qianjing_1.0.4_arm64-v8a_debug.apk
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

1. **仅支持 arm64-v8a**：不支持 32 位设备
2. **存储权限**：使用 `requestLegacyExternalStorage`，未适配 Scoped Storage
3. **`local.properties` 不入库**：新设备需自行配置（已随项目复制）

### 后续优化方向

| 优先级 | 优化项 | 说明 |
|---|---|---|
| 高 | 适配 Scoped Storage | 移除 `requestLegacyExternalStorage`，使用 MediaStore/SAF |
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
