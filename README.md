# NAT PROXY Introduction

Natproxy是一个内网穿透工具，可以使个人计算机或局域网上的服务器连接到公网上，且同时支持TCP/UDP流量转发以及其上层TCP协议（如`SSH`、`HTTP/HTTPS`...）。

# Getting started

1. Download
   ```shell
   git clone https://github.com/PonKing66/natproxy
    ```
2. Build
   ```shell
   cd natproxy
   mvn clean package -Dmaven.test.skip=true
    ```
3. Run
    1. `cd ../build/natproxy`
    2. `run client on the proxy client.`
    3. `run server on the proxy server.`
   
4. 详细配置
   1. 代理客户端配置：
   ```yaml
      server:
         host: 127.0.0.1 # 代理服务器IP
         port: 20001 # 代理服务器端口
      client:
         key: 721b2e485683bd87c32f3c208f787a626c7397a759146a93be30e15ad3193084 # 认证 client key
   ```
   2. 代理服务器配置：
   ```yaml
     server:
        port: 20001 # 代理服务器端口
        keys:
           - 721b2e485683bd87c32f3c208f787a626c7397a759146a93be30e15ad3193084 # 合法登录 client Key,与代理客户端配置中配置一致
        proxy:
          - host: 192.168.31.96 # 目标服务器IP
            intranetPort: 22 # 代理客户端中被代理端口（目标服务器端口）
            extranetPort: 22222 # 代理服务器暴露端口（用户访问端口）
            type: tcp # 代理转发协议
            key: 721b2e485683bd87c32f3c208f787a626c7397a759146a93be30e15ad3193084 # 指定开启代理客户端
          - host: 192.168.31.96
            intranetPort: 3306
            extranetPort: 3306
            type: tcp
            key: 721b2e485683bd87c32f3c208f787a626c7397a759146a93be30e15ad3193084
          - host: 127.0.0.1
            intranetPort: 44444
            extranetPort: 55555
            type: udp
            key: 721b2e485683bd87c32f3c208f787a626c7397a759146a93be30e15ad3193084
   ```
   
# Change Log

# Project structure

# 感谢

参考 [p2p-nat](https://gitee.com/TANGMONK-MEAT/p2p-nat)
