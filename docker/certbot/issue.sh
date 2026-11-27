#!/bin/sh
# 서버에서 Let's Encrypt 인증서를 처음 발급받는다. 저장소 폴더에서 `sh docker/certbot/issue.sh`로 실행한다.
# 먼저 할 일: DNS A 레코드가 이 서버를 가리키고, 방화벽 80·443이 열려 있고,
# docker compose -f docker-compose.prod.yml up -d 로 nginx가 떠 있어야 한다 (처음에는 연습용 인증서로 뜬다).
# APP_DOMAIN·APP_CERT_EMAIL은 .env를 셸에서 source하지 않고, docker compose가 .env를 읽어
# certbot 컨테이너 환경변수로 넘긴 값을 컨테이너 안에서만 쓴다 (비밀값에 셸 특수문자가 있어도 안전).
set -eu
cd "$(dirname "$0")/../.."

docker compose -f docker-compose.prod.yml --profile certbot run --rm --entrypoint sh certbot -c '
	set -eu
	: "${APP_DOMAIN:?APP_DOMAIN이 필요합니다}"
	: "${APP_CERT_EMAIL:?APP_CERT_EMAIL이 필요합니다}"
	certbot certonly --webroot -w /var/www/certbot \
		-d "$APP_DOMAIN" --email "$APP_CERT_EMAIL" --agree-tos --no-eff-email --non-interactive \
		--deploy-hook "/scripts/publish-cert.sh \$RENEWED_LINEAGE/fullchain.pem \$RENEWED_LINEAGE/privkey.pem"
'
docker compose -f docker-compose.prod.yml --profile certbot run --rm --entrypoint sh certbot /scripts/check-tls.sh --require-ca
docker compose -f docker-compose.prod.yml exec nginx nginx -t
docker compose -f docker-compose.prod.yml exec nginx nginx -s reload
echo "Let's Encrypt 인증서를 적용했습니다."
