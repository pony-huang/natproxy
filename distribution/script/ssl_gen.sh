#!/bin/bash

if [ ! -d ./ssl/server/  ];then
  mkdir -p ./ssl/server/
fi

if [ ! -d ./ssl/client/  ];then
  mkdir -p ./ssl/client/
fi


openssl genrsa -out ca.key 4096

CA_SUBJ='/C=CN/ST=GuangDong/L=ShenZhen/O=github/OU=ponyhuang/CN=test.com'
openssl req -new -x509 -days 1000 -subj $CA_SUBJ -key ca.key -out ca.crt

SERVER_KEY_PASS=123456
CLIENT_KEY_PASS=123456

openssl genrsa -aes256 -out server.key -passout pass:$SERVER_KEY_PASS 1024
openssl genrsa --aes256 -out client.key -passout pass:$CLIENT_KEY_PASS 1024

SERVER_SUBJ=$CA_SUBJ
CLIENT_SUBJ=$CA_SUBJ
openssl req -new -key server.key -out server.csr -subj $SERVER_SUBJ -passin pass:$SERVER_KEY_PASS
openssl req -new -key client.key -out client.csr -subj $CLIENT_SUBJ -passin pass:$CLIENT_KEY_PASS

openssl x509 -req -days 3650 -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server.crt
openssl x509 -req -days 3650 -in client.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out client.crt

openssl pkcs8 -topk8 -in server.key -out pkcs8_server.key -nocrypt -passin pass:$SERVER_KEY_PASS
openssl pkcs8 -topk8 -in client.key -out pkcs8_client.key -nocrypt -passin pass:$CLIENT_KEY_PASS

# shellcheck disable=SC2010
ls | grep server | cut -d " " -f 11 | xargs -i mv "{}" ./ssl/server
# shellcheck disable=SC2010
ls | grep client | cut -d " " -f 11 | xargs -i mv "{}" ./ssl/client

mv ca.crt ./ssl/
mv ca.key ./ssl/

rm -rf ./conf/ssl

mv ssl -f ./conf/