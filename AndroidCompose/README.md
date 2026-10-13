# 音书 2.0（Kotlin + Compose 重写版）

旧版 Java 工程保留在 `../Android`，本目录是重写后的新工程。

## 构建 APK（不需要 Android Studio）

方式一：本机命令行。双击 `build-apk.bat`。首次运行会把 JDK 17、Android 命令行 SDK、Gradle 下载到项目所在盘的 `AndroidBuildTools` 文件夹（约 3 GB，含依赖缓存），之后只做增量构建。产物是本目录下的 `MelodyBook-debug.apk`。

- `build-apk.bat -Install`：构建后通过 USB 直接装到手机（需开启 USB 调试）
- `build-apk.bat -NoMirror`：不用阿里云 / 腾讯镜像
- 卸载工具链：删除 `AndroidBuildTools` 文件夹即可

方式二：GitHub Actions 云端构建。推送到 GitHub 后，在仓库的 Actions 页面打开 “Build APK” 这次运行，从 Artifacts 下载 APK。也可以在 Actions 页面手动点 “Run workflow”。

以后想用 Android Studio，直接打开 `AndroidCompose` 目录即可。

依赖版本集中在 `gradle/libs.versions.toml`，是一组已知互相兼容的稳定版。想升级时在 Android Studio 里按提示统一升级即可。

## 结构

```
data/       Room 曲库、MediaStore 扫描、拼音、搜索排序、DataStore 设置
playback/   Media3 播放服务（MediaSessionService）、界面侧连接、定时关闭
ui/theme/   设计令牌（间距/尺寸/字体/圆角）与皮肤配色
ui/         曲库页、播放页、迷你播放条、弹层
```

## 与旧版的功能对照

| 旧版 | 新版 |
| --- | --- |
| 递归遍历存储扫描 | MediaStore 查询，增量合并，保留“已移除”标记 |
| 按格式扫描（含 MP4 听声） | 保留，MP4 听声会额外申请视频权限 |
| 列表搜索 | 歌名/歌手/专辑/文件夹 + 全拼 + 首字母 |
| 长按移除 | 长按或“更多”菜单：下一首播放 / 从列表移除 |
| 单曲 / 列表循环 | 列表循环 / 单曲循环 / 随机 |
| 定时关闭 | 预设时长 + 播完当前这首，结束时淡出 |
| 定位到当前歌曲 | 保留 |
| 自定义通知栏 | 系统媒体通知 + 锁屏 + 蓝牙/耳机按键 |
| 21 张随机背景 | 12 套配色皮肤 + 随壁纸取色 + 深浅色 + 可选相册背景图 |
| 旋转 CD | 播放页旋转唱片，中间显示封面 |
| 在线影视/寻图/寻歌外链 | 移除 |
| — | 记忆播放进度、拼音字母索引、随机播放全部、音频焦点、拔耳机暂停 |
