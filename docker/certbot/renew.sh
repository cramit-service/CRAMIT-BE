#!/bin/sh
# 인증서를 갱신한다 (만료 30일 전부터만 실제로 바뀐다). 서버의 cron에 하루 한 번 걸어 둔다. 예:
#   17 3 * * * cd /home/ubuntu/CRAMIT-BE && sh docker/certbot/renew.sh >> /var/log/cramit-renew.log 2>&1
set -eu
cd "$(dirname "$0")/../.."
docker compose -f docker-compose.prod.yml --profile certbot run --rm certbot renew --non-interactive

# renew의 deploy-hook이 이번 실행에서 안 돌았거나(갱신 대상 아님) 중간에 실패했어도, 여기서 매번
# 현재 발급된 인증서를 /tls와 다시 맞춘다. 그래야 과거에 반영이 실패한 채로 남아있는 걸 놓치지 않는다.
# publish-cert.sh가 새 버전 디렉터리를 만들고 symlink를 원자적으로 바꿔치기하므로, 두 파일이
# 따로따로 교체되는 중간 상태가 nginx에 노출되지 않는다.
docker compose -f docker-compose.prod.yml --profile certbot run --rm --entrypoint sh certbot -c '
	set -eu
	: "${APP_DOMAIN:?APP_DOMAIN이 필요합니다}"
	live="/etc/letsencrypt/live/$APP_DOMAIN"
	[ -d "$live" ] || { echo "인증서 디렉터리($live)가 없습니다" >&2; exit 1; }
	/scripts/publish-cert.sh "$live/fullchain.pem" "$live/privkey.pem"
'
docker compose -f docker-compose.prod.yml --profile certbot run --rm --entrypoint sh certbot /scripts/check-tls.sh --require-ca
docker compose -f docker-compose.prod.yml exec nginx nginx -t
docker compose -f docker-compose.prod.yml exec nginx nginx -s reload
