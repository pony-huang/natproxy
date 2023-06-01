# NAT PROXY Introduction

Natproxy is an intranet penetration tool that connects person computer or servers on the LAN to the public network,
and it supports tcp/udp traffic forwarding and any upper-layer tcp protocol (ssh, http, https ...) .

# Feature

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

# Change Log

# Related warehouse

[p2p-nat](https://gitee.com/TANGMONK-MEAT/p2p-nat)
