#!/bin/sh
# 인증서+키 쌍을 /tls에 원자적으로 반영한다. nginx는 심볼릭 링크 /tls/current를 통해서만 두 파일을
# 읽으므로(/etc/nginx/certs/current/...), 이 링크 하나를 바꿔치기하는 순간에만 버전이 바뀐다 —
# fullchain.pem·privkey.pem을 따로따로 교체하지 않아 두 파일이 서로 다른 버전으로 섞일 틈이 없다.
# 사용: publish-cert.sh <fullchain.pem 원본 경로> <privkey.pem 원본 경로>
set -eu
cert_src=$1
key_src=$2

version="v-$(date +%s)-$$"
dir="/tls/$version"
mkdir -p "$dir"
cp -L "$cert_src" "$dir/fullchain.pem"
cp -L "$key_src" "$dir/privkey.pem"
chmod 600 "$dir/privkey.pem"

# -n(no-dereference)이 없으면 current가 가리키는 디렉터리 "안"에 새 링크를 만들어버리므로 반드시 같이 쓴다.
# -f만으로는 심볼릭 링크가 디렉터리를 가리킬 때 "그 디렉터리 안으로 이동"으로 해석되는 mv/ln의 공통 함정이 있다.
ln -sfn "$version" /tls/current

# 지금 가리키는 버전만 남기고 이전 버전 디렉터리를 정리한다 (실패해도 치명적이지 않다).
keep=$(basename "$(readlink /tls/current)")
for d in /tls/v-*; do
	[ -d "$d" ] || continue
	name=$(basename "$d")
	[ "$name" = "$keep" ] || rm -rf "$d"
done
