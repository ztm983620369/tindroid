# 电脑版（Compose Desktop）

这是一个纯 JVM 的 Compose Desktop 客户端，最大化复用本仓库的 Tinode SDK（`tinodesdk-jvm`）实现：

- 登录（Basic）
- 拉取会话列表（通过订阅 `me`）
- 打开聊天、加载消息、收发文本消息

## 运行

在仓库根目录执行：

```bash
cd /root/tindroid
./gradlew :desktop:run
```

## 说明

- 默认 API Key 与 Android 端保持一致（来源：`tindroid/app/src/main/java/co/tinode/tindroid/Cache.java`）。
- 默认 Host 预填为 Android debug 的 `default_host_name`（来源：`tindroid/app/build.gradle`）。
