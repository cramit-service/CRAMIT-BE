#!/bin/sh
# tls 볼륨에 인증서가 없을 때만 연습용(자체 서명) 인증서를 만든다. 이미 있으면(연습용이든 Let's Encrypt든) 그대로 둔다.
# 서버에서는 issue.sh로 Let's Encrypt 인증서로 바꾼다.
set -eu
: "${APP_DOMAIN:?인증서를 만들 도메인이 필요합니다}"
if [ -s /tls/fullchain.pem ] && [ -s /tls/privkey.pem ]; then
	echo "인증서가 이미 있어 그대로 둡니다."
	exit 0
fi
umask 077
openssl req -x509 -newkey rsa:2048 -nodes -days 30 \
	-subj "/CN=${APP_DOMAIN}" -addext "subjectAltName=DNS:${APP_DOMAIN}" \
	-keyout /tls/privkey.pem -out /tls/fullchain.pem
echo "연습용 인증서를 만들었습니다 (${APP_DOMAIN}, 30일)."
