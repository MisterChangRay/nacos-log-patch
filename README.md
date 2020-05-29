# nacos-log-patch
nacos配置中心, 日志增强插件, 目前有bug 不能在日志启动前加载配置

# 需求介绍
公司在使用阿里云日志得时候, 日志配置文件中得某些配置不能动态从nacos中获取。
目前公司使用nacos版本 V1.1.4;

nacos 默认提供一下配置进行：
```js
nacos.config.endpoint=192.168.0.179
nacos.config.endpointPort=8848
nacos.config.namespace=8eac56d7-6b0a-4581-ab13-67bb62f71c76
nacos.config.auto-refresh=true
nacos.config.bootstrap.enable=true
# 此行开启nacos可在日志框架加载前进行加载
nacos.config.bootstrap.log-enable-patch=true
## 左边配置的会覆盖右边的配置
nacos.config.data-ids=com-xd-device-common.properties
nacos.config.type=properties
```

目前版本中, 以上配置不能生效, 原因nacos 不能正确加载远程配置.

# 快速使用
1. 引入jar包
2. 在以上配置上加入 `nacos.config.bootstrap.log-enable-patch=true` 配置
3. 启动项目即可正确加载


# 注意事项
1. 日志配置应该以 `com.custom.log` 为前缀
2. xml 中请注意应该引入springcontetxt中得配置
