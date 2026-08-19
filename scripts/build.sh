#!/usr/bin/env bash
# ForgeOS 빌드 스크립트
#
# 커널(io.github.jongwoo0101:forgeframework)과 명령어 계층
# (io.github.jongwoo0101:forgecli)이 로컬 Maven 저장소에 없으면 의존성 해석
# 단계에서 실패한다. 그럴 때는 각 저장소에서 publishToMavenLocal 을 먼저 실행한다.
#
#   forge-framework $ ./scripts/publish.sh
#   forge-cli       $ ./gradlew publishToMavenLocal
set -e
cd "$(dirname "$0")/.."

./gradlew build

echo
echo "산출물:"
find build/libs -name "*.jar" -print 2>/dev/null | sed 's|^|  |'
