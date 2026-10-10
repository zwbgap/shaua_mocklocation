# 迁境 (MockLocation)

> 一款基于 Android 调试 API + 百度地图 SDK 实现的无需 ROOT 权限的虚拟定位应用。

---

## 项目说明

本项目基于开源项目 **影梭 (GoGoGo)** Fork 并修改而来。

- **原项目**：[影梭 GoGoGo](https://github.com/ZCShou/GoGoGo) — 作者 ZCShou，采用 GPL-3.0 协议开源
- **本项目**：[迁境](https://github.com/zwbgap/shaua_mocklocation) — 在原项目基础上进行架构重构、功能精简与体验优化

本项目遵循 GPL-3.0 协议开源，详见 [LICENSE](./LICENSE)。

---

## 功能特性

### 核心功能

- **模拟定位**：通过 Android 调试 API 同时注入 GPS 与 NETWORK Provider，无需 ROOT 权限即可修改系统定位
- **百度地图选点**：支持地图展示、单击/长按/双击交互，普通图与卫星图切换
- **POI 逆地理编码**：长按地图弹出地点详情，支持保存、复制、分享、传送
- **POI 联想搜索**：基于百度地图搜索建议，支持全国与城市范围搜索
- **手动输入坐标**：支持 BD09 / WGS84 坐标系输入，可自动解析逗号、空格等多种分隔格式的坐标串
- **运行中更新位置**：模拟定位运行时可重新选点，位置实时更新
- **收藏位置**：SQLite 本地存储收藏地点，支持增删改查与搜索

### 辅助功能

- **海拔高度配置**：可自定义模拟定位的海拔值
- **随机偏移**：对收藏位置启用经纬度随机偏移（可配置最大偏移距离）
- **主题定制**：支持青、蓝、紫、橙四种配色主题，全局适配状态栏与导航栏
- **用户协议与隐私政策**：首次启动需阅读并同意
- **运行时权限管理**：定位、存储等权限按需申请
- **本地日志记录**：基于 XLog 的本地运行日志，支持开关与查看分享
- **历史记录**：搜索历史自动记录，支持有效期归档

### 已移除功能（相对原项目）

为精简核心体验，本项目移除了原项目中的以下功能：

- 悬浮摇杆（圆盘摇杆 + 八方向按键摇杆）
- 模拟移动（步行/跑步/自行车速度切换）
- 方向传感器地图方向显示

---

## 环境要求

| 依赖 | 版本要求 |
|---|---|
| JDK | 17 或更高 |
| Android SDK | platform 34、build-tools 36.0.0 |
| Gradle | 8.13（由 Wrapper 自动下载） |
| Android Gradle Plugin | 8.12.1 |
| 百度地图 SDK | BaiduLBS_Android v7.6.5 |

---

## 构建说明

### 1. 配置 local.properties

在项目根目录创建 `local.properties` 文件（该文件不入库），配置百度地图 AK 与签名信息：

```properties
sdk.dir=你的Android SDK路径

# 百度地图 AK
MAPS_API_KEY=你的百度地图AK

# 签名配置
storeFile=keystore/mocklocation.jks
storePassword=你的store密码
keyAlias=你的key别名
keyPassword=你的key密码
```

### 2. 构建 APK

```bash
# Debug 包
./gradlew assembleDebug

# Release 包
./gradlew assembleRelease
```

构建产物输出至 `app/build/outputs/apk/` 目录，文件命名格式：

- Debug：`迁境_<版本号>_arm64-v8a_debug.apk`
- Release：`迁境_<版本号>_arm64-v8a_release.apk`

### 3. 百度地图 AK 配置

前往 [百度地图开放平台](https://lbsyun.baidu.com/) 创建 Android 应用，获取 AK 并配置安全码：

- **包名**：`com.shaua.mocklocation`
- **SHA1**：使用签名证书的 SHA1 值
- **安全码**：`SHA1;com.shaua.mocklocation`

---

## 使用说明

1. 安装 APK 后启动应用
2. 首次启动需阅读并同意《用户协议》和《隐私政策》
3. 授予定位权限
4. 在 **设置 → 开发者选项 → 选择模拟位置信息应用** 中选择「迁境」
5. 在地图上选取目标位置，点击传送即可开始模拟定位
6. 点击停止按钮恢复真实定位

---

## 项目结构

```
GoGoGo/
├── app/
│   ├── libs/                              # 百度地图 SDK（jar + arm64-v8a .so）
│   ├── src/main/
│   │   ├── java/com/shaua/mocklocation/
│   │   │   ├── data/                      # 数据层（Model + DAO + Repository）
│   │   │   ├── database/                  # 搜索历史数据库
│   │   │   ├── service/                   # MockLocationService（定位注入）
│   │   │   ├── utils/                     # 工具类（GoUtils / MapUtils / ShareUtils）
│   │   │   ├── BaseActivity.java          # 基类 Activity（主题应用）
│   │   │   ├── MainActivity.java          # 主界面（地图 + 选点 + 启动定位）
│   │   │   ├── MainViewModel.java         # ViewModel（LiveData 状态驱动）
│   │   │   ├── FavoritesFragment.java     # 收藏位置列表
│   │   │   ├── MineFragment.java          # 设置页面
│   │   │   ├── HistoryActivity.java       # 历史记录
│   │   │   ├── LogViewerActivity.java     # 运行日志查看
│   │   │   ├── SettingsActivity.java      # 设置入口
│   │   │   ├── WelcomeActivity.java       # 协议与权限引导页
│   │   │   └── GoApplication.java         # Application 入口
│   │   ├── res/                           # 资源文件（布局、图片、字符串、主题等）
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── gradle/wrapper/                        # Gradle Wrapper
├── keystore/
│   └── mocklocation.jks                   # 签名证书（密码见 local.properties）
├── docs/
│   ├── CHANGELOG.md                       # 变更日志
│   └── build-log.md                       # 构建日志
├── local.properties                       # AK + 签名配置（不入库）
├── build.gradle / settings.gradle / gradle.properties
├── gradlew / gradlew.bat
└── README.md / README.en.md
```

---

## 技术原理

### 模拟定位实现

1. 通过 `LocationManager.addTestProvider()` 同时添加 GPS 与 NETWORK 两个 Provider
2. `setTestProviderEnabled()` 启用 Provider
3. `setTestProviderLocation()` 每 100ms 推送一次 Location（由 HandlerThread 驱动）
4. Location 需设置：`latitude`、`longitude`、`altitude`、`accuracy`、`bearing`、`speed`、`time`、`elapsedRealtimeNanos`

### 坐标系转换

- **BD09**（百度地图坐标系）→ **GCJ02**（火星坐标）→ **WGS84**（GPS 原始坐标，用于注入系统）
- `MapUtils.java` 中实现了 `bd092gcj`、`gcj2wgs`、`wgs2gcj`、`gcj2bd09` 等转换方法

### 主题系统

- 通过 `BaseActivity` 在 `onCreate` 中读取 SharedPreferences 的配色设置并动态应用主题
- 支持青、蓝、紫、橙四种配色，全局覆盖状态栏、导航栏、按钮等元素
- 切换主题后页面自动重启生效

---

## 常见问题

**Q：地图显示白屏？**
A：检查百度地图 AK 是否为 Android SDK 类型，SHA1 与包名是否与百度控制台配置一致。

**Q：模拟定位不生效？**
A：确认已在「开发者选项 → 选择模拟位置信息应用」中选择了「迁境」，并开启了设备定位服务。

**Q：搜索无结果？**
A：检查网络连接，确认百度地图 AK 已开通 POI 搜索权限。

**Q：构建失败提示签名错误？**
A：确认 `local.properties` 中的签名配置与 `keystore/mocklocation.jks` 一致。

---

## 许可证

本项目基于 GPL-3.0 协议开源，版权声明如下：

- 原项目：© 2020 ZCShou (影梭 GoGoGo) — https://github.com/ZCShou/GoGoGo
- 修改部分：© 2026 shaua (迁境)

完整协议内容见 [LICENSE](./LICENSE)。

---

## 致谢

感谢原项目作者 ZCShou 开源「影梭」项目，以及以下参考项目：

- [MockGPS](https://github.com/Hilaver/MockGPS)
- [together-go](https://github.com/bxxfighting/together-go)
- [Mocklation](https://github.com/P72B/Mocklation)
