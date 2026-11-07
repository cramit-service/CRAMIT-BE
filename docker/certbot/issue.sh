#!/bin/sh
# 서버에서 Let's Encrypt 인증서를 처음 발급받는다. 저장소 폴더에서 `sh docker/certbot/issue.sh`로 실행한다.
# 먼저 할 일: DNS A 레코드가 이 서버를 가리키고, 방화벽 80·443이 열려 있고,
# docker compose -f docker-compose.prod.yml up -d 로 nginx가 떠 있어야 한다 (처음에는 연습용 인증서로 뜬다).
# .env의 APP_DOMAIN·APP_CERT_EMAIL을 쓴다.
set -eu
cd "$(dirname "$0")/../.."
set -a
. ./.env
set +a
: "${APP_DOMAIN:?.env에 APP_DOMAIN이 필요합니다}"
: "${APP_CERT_EMAIL:?.env에 APP_CERT_EMAIL이 필요합니다}"

docker compose -f docker-compose.prod.yml --profile certbot run --rm certbot certonly --webroot -w /var/www/certbot \
	-d "${APP_DOMAIN}" --email "${APP_CERT_EMAIL}" --agree-tos --no-eff-email --non-interactive \
	--deploy-hook 'cp -L "$RENEWED_LINEAGE/fullchain.pem" "$RENEWED_LINEAGE/privkey.pem" /tls/ && chmod 600 /tls/privkey.pem'
docker compose -f docker-compose.prod.yml --profile certbot run --rm --entrypoint sh certbot /scripts/check-tls.sh --require-ca
docker compose -f docker-compose.prod.yml exec nginx nginx -t
docker compose -f docker-compose.prod.yml exec nginx nginx -s reload
echo "Let's Encrypt 인증서를 적용했습니다: https://${APP_DOMAIN}"
