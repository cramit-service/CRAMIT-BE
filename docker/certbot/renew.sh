#!/bin/sh
# 인증서를 갱신한다 (만료 30일 전부터만 실제로 바뀐다). 서버의 cron에 하루 한 번 걸어 둔다. 예:
#   17 3 * * * cd /home/ubuntu/CRAMIT-BE && sh docker/certbot/renew.sh >> /var/log/cramit-renew.log 2>&1
set -eu
cd "$(dirname "$0")/../.."
docker compose -f docker-compose.prod.yml --profile certbot run --rm certbot renew --non-interactive
docker compose -f docker-compose.prod.yml --profile certbot run --rm --entrypoint sh certbot /scripts/check-tls.sh --require-ca
docker compose -f docker-compose.prod.yml exec nginx nginx -t
docker compose -f docker-compose.prod.yml exec nginx nginx -s reload
