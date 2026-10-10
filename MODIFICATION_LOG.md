# 迁境 (MockLocation) 项目修改记录文档

> 本文档记录从原开源项目「影梭 (GoGoGo)」Fork 到「迁境」的完整修改过程与内容。

---

## 一、原项目信息

| 项目 | 内容 |
|---|---|
| **项目名称** | 影梭 (GoGoGo) |
| **原作者** | ZCShou |
| **仓库地址** | https://github.com/ZCShou/GoGoGo |
| **开源协议** | GPL-3.0 |
| **原包名** | `com.zcshou.gogogo` |
| **原签名证书** | `keystore/GoGoGo.jks`（别名 GoGoKey，密码硬编码于 build.gradle） |

### 原项目简介

影梭是一款基于 Android 调试 API + 百度地图 SDK 实现的无需 ROOT 权限的虚拟定位应用，核心功能包括：
- 模拟定位（GPS + NETWORK Provider 注入）
- 百度地图选点与 POI 搜索
- 悬浮摇杆控制移动（圆盘摇杆 + 八方向按键）
- 步行/跑步/自行车速度模拟移动
- 历史记录与坐标输入

---

## 二、修改概览

| 项目 | 内容 |
|---|---|
| **项目名称** | 迁境 (MockLocation) |
| **修改者** | shaua |
| **包名** | `com.shaua.mocklocation` |
| **版本** | 1.12.3 |
| **协议** | GPL-3.0（遵循原项目协议） |
| **核心改动** | 架构重构、功能精简、体验优化 |

---

## 三、架构重构

### 3.1 移除悬浮摇杆模块

原项目的核心特色之一是悬浮摇杆控制移动，本项目为精简核心定位体验，将其整体移除：

| 删除项 | 说明 |
|---|---|
| `JoyStick.java`、`RockerView.java`、`ButtonView.java` | 摇杆自定义 View |
| `ServiceGo.java` | 原模拟定位服务（含摇杆逻辑） → 重命名为 `MockLocationService.java` |
| `joystick.xml`、`joystick_button.xml`、`joystick_history.xml`、`joystick_map.xml` | 摇杆相关布局 |
| `ic_left/right/up/down/...`、`ic_lock_close/open`、`ic_walk/run/bike`、`ic_move` 等 | 摇杆方向与移动图标 |
| `SYSTEM_ALERT_WINDOW` 权限 | 悬浮窗权限 |

### 3.2 引入 MVVM 架构

| 新增文件 | 职责 |
|---|---|
| `MainViewModel.java` | ViewModel，通过 LiveData 驱动 UI 状态 |
| `data/LocationRepository.java` | 仓库层，封装数据访问逻辑 |
| `data/FavoriteLocationDao.java` | 收藏位置数据库访问对象 |
| `data/FavoriteLocation.java` | 收藏位置数据模型 |

### 3.3 数据层重构

- 原 `DataBaseHistoryLocation.java`（搜索历史与收藏混用）→ 拆分为收藏专用 `FavoriteLocationDao.java`
- 数据库表名从历史记录表改为 `FavoriteLocation`
- `HistoryActivity.java` 改用 `LocationRepository`，通过 Intent 传递选中位置

### 3.4 MainActivity 精简

- 移除方向传感器代码
- 移除悬浮窗权限检查
- 集成 `MainViewModel`，改用 LiveData 观察状态
- 通过 Intent 接收从收藏页跳转的位置

---

## 四、包名与签名重配置

### 4.1 包名重命名

| 项目 | 旧值 | 新值 |
|---|---|---|
| package | `com.zcshou.gogogo` | `com.shaua.mocklocation` |
| namespace | `com.zcshou.gogogo` | `com.shaua.mocklocation` |
| applicationId | `com.zcshou.gogogo` | `com.shaua.mocklocation` |
| 目录结构 | `com/zcshou/gogogo/` | `com/shaua/mocklocation/` |

变更范围涵盖全部 16 个 Java 源文件、2 个测试文件、`AndroidManifest.xml` 中的 Service 全限定名、`build.gradle` 的 namespace/applicationId、`local.properties` 的安全码包名部分。

### 4.2 签名证书重新配置

| 项目 | 旧值 | 新值 |
|---|---|---|
| 证书文件 | `keystore/GoGoGo.jks` | `keystore/mocklocation.jks` |
| Alias | `GoGoKey` | `mocklocation` |
| 密码 | 硬编码于 `build.gradle` | 移至 `local.properties`（不入库） |
| 算法 | — | RSA 2048 / SHA384withRSA |
| 有效期 | — | 10,000 天 |

安全改进：
- 签名密码从 `build.gradle` 移至 `local.properties`（已加入 `.gitignore`）
- `build.gradle` 通过 `keystoreProperties` 读取签名配置
- debug 与 release 均使用同一签名（确保百度地图 SDK 在 debug 包正常工作）

### 4.3 百度地图安全码

- 新安全码：`D086683CA79CD91EC9B844B5A7E9E609ABCE8892;com.shaua.mocklocation`
- AK 配置于 `local.properties` 的 `MAPS_API_KEY`

---

## 五、Bug 修复

| 修复项 | 问题 | 解决方案 |
|---|---|---|
| POI 搜索失败 | `SuggestionSearchOption.city()` 在 `mCurrentCity` 为 null 时传入 null 导致搜索失败 | city 为 null 时不传 city 参数，改为全国搜索 |
| 坐标转换错误 | `MapUtils.wgs2bd09(lat, lng)` 参数顺序颠倒 | 修正为 `wgs2bd09(lng, lat)` |
| 收藏时间显示为 1970 年 | 保存时使用秒级时间戳，`Date` 构造函数期望毫秒级 | 显示时将秒级时间戳乘以 1000 转换为毫秒 |
| 收藏列表删除按钮被挤出屏幕 | `history_item.xml` 中间内容区域使用 `match_parent` | 改为 `0dp` + `layout_weight="1"` |
| 坐标输入粘贴报错 | `digits` 限制过滤逗号，粘贴地图坐标时产生非法值 | 移除 `digits` 限制，添加自动识别逗号/中文逗号/空格分隔的坐标对解析逻辑 |
| WiFi 提示对话框按钮不可见 | 深色主题下按钮文字颜色与背景对比度不足 | 对话框 show() 后显式设置按钮文字颜色 |
| MainActivity 启动闪退 | `setSupportActionBar` 使用自定义 Toolbar，但主题继承自带 ActionBar 的主题 | 在 `styles.xml` 的 AppTheme 中设置 `windowActionBar=false` 和 `windowNoTitle=true` |
| 用户协议/隐私政策页接受按钮不可见 | 修复 Lint 警告时覆盖了按钮背景和文字颜色 | 还原按钮样式，保留原有背景和文字颜色配置 |
| 列表项布局挤压 | `search_item.xml`、`search_poi_item.xml` 中间内容容器 `match_parent` | 改为 `0dp` + `layout_weight="1"` |

---

## 六、功能增强

### 6.1 主题系统

- 新增 `BaseActivity.java`，在 `onCreate` 中读取 SharedPreferences 的配色设置并动态应用主题
- 支持 **青、蓝、紫、橙** 四种配色主题
- 主题全局适配：覆盖顶部状态栏、底部导航栏、按钮等所有界面元素
- 切换主题后页面自动重启生效
- 移除原有的日间/夜间模式切换，仅保留四种配色

### 6.2 收藏功能增强

- 收藏列表项右侧添加删除按钮，点击弹出确认对话框后执行删除
- 收藏时间显示修复（秒级 → 毫秒级时间戳）

### 6.3 坐标输入增强

- 新增独立坐标串输入框
- 支持自动解析逗号、中文逗号、空格等多种分隔格式的坐标串
- 解析后自动拆分填入经度、纬度输入框
- try-catch 捕获解析异常，避免崩溃

### 6.4 界面统一

- 所有功能页面统一添加顶部 Toolbar 标题栏（收藏页、日志页、设置页）
- 统一输入框高度与内边距
- 底部取消/传送按钮改为等宽对称分布
- 新增次要按钮背景资源 `bg_btn_secondary.xml`

### 6.5 其他

- `app_author`：`ZCShou` → `shaua`
- 删除未使用的 `arrays.xml`（`array_map_type`）
- `location_input.xml` 中 ID `joystick_longitude/latitude` 重命名为 `input_longitude/latitude`
- 编译 SDK 从预览版 36 回退到稳定版 34
- `compileSdk` / `targetSdk` 统一为 34
- 16KB 页设备兼容：`packagingOptions.jniLibs.useLegacyPackaging = true`（百度地图 SDK libindoor.so 未按 16KB 边界对齐）

---

## 七、设置页精简

### 移除项

| 设置项 | 说明 |
|---|---|
| 摇杆类型 | 悬浮摇杆已移除 |
| 步行/跑步/自行车速度 | 模拟移动功能已移除 |

### 保留项

| 设置项 | 说明 |
|---|---|
| 海拔高度 | 模拟定位的海拔配置 |
| 随机偏移 | 仅对收藏位置生效，可配置最大偏移距离 |
| 日志开关 | XLog 本地日志开关 |
| 历史记录有效期 | 搜索历史自动归档天数 |
| 版本号 / 开发者 | 关于信息 |
| 主题配色 | 青、蓝、紫、橙四色选择 |

---

## 八、构建与依赖版本

| 配置项 | 版本 |
|---|---|
| compileSdk / targetSdk | 34 |
| buildToolsVersion | 36.0.0 |
| minSdkVersion | 27 (Android 8.1) |
| Gradle | 8.13 |
| Android Gradle Plugin | 8.12.1 |
| JDK 目标 | 11 |
| NDK | 26.2.11394342 |
| ABI | arm64-v8a |

### 主要依赖

| 依赖 | 版本 |
|---|---|
| androidx.appcompat | 1.6.1 |
| androidx.lifecycle (viewmodel + livedata) | 2.7.0 |
| androidx.constraintlayout | 2.1.4 |
| androidx.preference | 1.2.1 |
| com.google.android.material | 1.10.0 |
| com.squareup.okhttp3 | 4.12.0 |
| com.elvishew:xlog | 1.11.1 |
| io.noties.markwon:core | 4.6.2 |
| BaiduLBS_Android (本地 jar) | v7.6.5 |

---

## 九、当前会话（2026-10-11）操作记录

### 9.1 项目完整性检查

- 使用 Gradle Wrapper 执行 `assembleDebug` 构建验证
- 构建产物：`app/build/outputs/apk/debug/迁境_1.12.3_arm64-v8a_debug.apk`（约 18.7 MB）
- APK 结构验证通过：包含 `AndroidManifest.xml` 及多个 `classes.dex`
- 说明：沙箱环境限制了 AGP 遥测服务（AnalyticsService / AndroidLocationsBuildService）对 `C:\Android\metrics\spool\` 的写入，导致构建进程以非零退出码结束，但所有编译打包任务（34 个）均成功完成（UP-TO-DATE），APK 有效

### 9.2 GitHub 相关配置清理

为准备上传至个人 GitHub 仓库，清理了原项目的 GitHub 平台相关配置，仅保留代码与构建配置：

| 删除项 | 类型 | 说明 |
|---|---|---|
| `.github/` | 目录 | Issue 模板、Pull Request 模板、GitHub Actions 工作流（build-check、build-release、codeql-analysis、issue-check、issue-comment）、FUNDING.yml、dependabot.yml |
| `CODE_OF_CONDUCT.md` | 文件 | 社区行为准则（原项目 GitHub 社区配置） |
| `SECURITY.md` | 文件 | 安全策略（原项目 GitHub 社区配置） |

### 9.3 仓库地址更新

本项目迁移至个人 GitHub 仓库：`https://github.com/zwbgap/shaua_mocklocation`

同步更新代码中指向原项目的 URL：

| 文件 | 修改项 | 旧值 | 新值 |
|---|---|---|---|
| `MineFragment.java` | 联系作者链接 | `https://gitee.com/itexp/gogogo/issues` | `https://github.com/zwbgap/shaua_mocklocation/issues` |
| `MainActivity.java` | 版本检测 API | `https://api.github.com/repos/zcshou/gogogo/releases/latest` | `https://api.github.com/repos/zwbgap/shaua_mocklocation/releases/latest` |
| `MainActivity.java` | 版本检测开关 | 临时屏蔽（`return;`） | 已启用 |

### 保留的 GitHub 无关文件

- `LICENSE` — GPL-3.0 协议文本（法律要求保留）
- `.gitignore` — Git 忽略规则
- `README.md` / `README.en.md` — 项目说明（已重写）

### 9.3 文档重写

| 文件 | 说明 |
|---|---|
| `README.md` | 中文版项目说明，记录 Fork 来源（影梭 GoGoGo by ZCShou）、功能特性、构建说明、项目结构、技术原理、许可证 |
| `README.en.md` | 英文版项目说明，内容与中文版对应 |
| 本文档 `MODIFICATION_LOG.md` | 专业修改记录文档，完整记录从原项目到本项目的所有修改内容与过程 |

---

## 十、项目文件清单（上传 GitHub 前）

### 核心代码与配置

```
GoGoGo/
├── app/
│   ├── libs/                    # 百度地图 SDK
│   ├── src/                     # 源码与资源
│   ├── build.gradle
│   └── proguard-rules.pro
├── gradle/
│   └── wrapper/                 # Gradle Wrapper
├── keystore/
│   └── mocklocation.jks         # 签名证书
├── docs/
│   ├── CHANGELOG.md
│   └── build-log.md
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── .gitignore
├── LICENSE
├── README.md
└── README.en.md
```

### 不入库文件（.gitignore 已覆盖）

- `local.properties`（AK + 签名配置）
- `keystore/mocklocation.jks` 的密码（在 local.properties 中）
- `.gradle/`、`build/`、`.idea/`、`.vscode/` 等 IDE 与构建缓存

> 注意：`keystore/mocklocation.jks` 证书文件本身当前在仓库目录中，上传 GitHub 前请评估是否需要将其加入 `.gitignore`（签名密码已在 `local.properties` 中不入库）。

---

## 十一、许可证声明

本项目基于 GPL-3.0 协议开源，修改遵循原项目协议要求：

- 原项目：© 2020 ZCShou (影梭 GoGoGo) — https://github.com/ZCShou/GoGoGo
- 修改部分：© 2026 shaua (迁境)

完整协议文本见 `LICENSE` 文件。
