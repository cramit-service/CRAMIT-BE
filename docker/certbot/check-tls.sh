#!/bin/sh
# tls 볼륨의 인증서를 nginx에 적용하기 전에 확인한다 (certbot 컨테이너 안에서 실행). 하나라도 틀리면 0이 아닌 값으로 끝난다.
#   - 인증서·개인 키가 있고, 인증서가 아직 유효하며, 개인 키와 짝이 맞고, 도메인 이름이 들어 있다
#   - --require-ca: 연습용(자체 서명)이 아니라 브라우저가 믿는 공개 인증기관까지 체인이 이어져야 한다 (issue.sh·renew.sh)
set -eu
: "${APP_DOMAIN:?확인할 도메인이 필요합니다}"
cert=/tls/current/fullchain.pem
key=/tls/current/privkey.pem

fail() {
	echo "인증서 확인 실패: $1" >&2
	exit 1
}

[ -s "$cert" ] || fail "$cert 이 없습니다"
[ -s "$key" ] || fail "$key 가 없습니다"
openssl x509 -in "$cert" -noout -checkend 0 > /dev/null || fail "인증서가 만료되었습니다"
cert_pub=$(openssl x509 -in "$cert" -noout -pubkey | openssl sha256)
key_pub=$(openssl pkey -in "$key" -pubout | openssl sha256)
[ "$cert_pub" = "$key_pub" ] || fail "인증서와 개인 키가 짝이 아닙니다"
openssl x509 -in "$cert" -noout -checkhost "$APP_DOMAIN" | grep -q "does match" || fail "인증서에 $APP_DOMAIN 이 없습니다"
if [ "${1:-}" = "--require-ca" ]; then
	openssl verify -untrusted "$cert" "$cert" > /dev/null 2>&1 \
		|| fail "공개 인증기관 체인으로 확인되지 않습니다 (연습용 인증서이거나 중간 인증서가 빠짐)"
fi
echo "인증서 확인 통과: $(openssl x509 -in "$cert" -noout -enddate)"
