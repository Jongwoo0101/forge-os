#!/usr/bin/env bash
# ForgeOS 실행 스크립트
#
# JavaFX 애플리케이션은 모듈 경로에 플랫폼별 런타임이 필요하다. 그 구성을
# Gradle 플러그인이 맡고 있으므로 jar 를 직접 실행하지 않고 run 태스크로 띄운다.
set -e
cd "$(dirname "$0")/.."

exec ./gradlew run --console=plain "$@"
