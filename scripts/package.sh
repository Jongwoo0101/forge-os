#!/usr/bin/env bash
#
# ForgeOS 배포본 만들기 — 지금 이 컴퓨터의 플랫폼용으로만 만들어진다.
#
# jlink 런타임 이미지는 크로스 컴파일이 안 된다. macOS 에서 돌리면 .dmg 만,
# Windows 에서 돌리면 .msi 만 나온다. 세 플랫폼을 한 번에 굽는 것은
# .github/workflows/release.yml 이 GitHub Actions 에서 한다.
#
# 선행 조건: 커널과 CLI 가 mavenLocal 에 있어야 한다.
#
#     forge-framework $ ./gradlew publishToMavenLocal
#     forge-cli       $ ./gradlew publishToMavenLocal
#
# 산출물:
#     build/jpackage/ForgeOS-<버전>.dmg|.msi|.deb   설치본
#     build/jpackage/ForgeOS.app | ForgeOS/          설치 전 앱 이미지
#     build/image.zip                                설치 없이 쓰는 런타임 이미지
set -e
cd "$(dirname "$0")/.."

echo "==> jpackage · jlinkZip (javafx.web 때문에 몇 분 걸리고 산출물이 300MB 안팎입니다)"
./gradlew jpackage jlinkZip "$@"

echo
echo "==> 산출물"
ls -la build/jpackage 2>/dev/null || true
ls -la build/image.zip 2>/dev/null || true

case "$(uname -s)" in
  Darwin)
    echo
    echo "macOS 안내: 이 앱은 서명·공증되어 있지 않습니다."
    echo "받는 사람이 '손상되었습니다' 경고를 보면 아래를 한 번 실행하면 됩니다."
    echo "    xattr -dr com.apple.quarantine /Applications/ForgeOS.app"
    ;;
esac
