# 릴리즈 절차

버전이 바뀔 때마다 따라 하는 순서. 특정 버전에 매인 문서가 아니므로
`RELEASE_NOTES_*.md` · `COMMIT_PLAN_*.md` 와 달리 **한 벌만 유지한다.**

---

## 0. 먼저 정할 것 — 저장소 몇 개가 움직이는가

Forge Ecosystem은 저장소 셋이고 의존 방향이 한쪽이다.

```
forge-framework (커널)  ←  forge-cli (명령어 계층)  ←  forge-os (이 저장소)
```

| 무엇이 바뀌었나 | 릴리즈할 저장소 | 순서 |
|---|---|---|
| 커널에 기능이 추가됐다 | 셋 다 | framework → cli → os |
| CLI 명령어만 늘었다 | cli → os | cli → os |
| ForgeOS 화면·앱만 바뀌었다 | os 하나 | — |

**순서를 지켜야 하는 이유**는 문서 때문이다. ForgeOS의 README가 "커널 `X`를
`publishToMavenLocal` 하세요"라고 말하는데 그 태그가 아직 없으면, README를 따라온
사람이 첫 단계에서 막힌다. 코드가 아니라 **읽는 사람의 경로**가 깨지는 문제다.

번호는 `1.1.0`부터 셋을 맞춰 두고 있다. 맞출 수 없는 상황(커널은 그대로인데 OS만
고칠 때)이면 OS만 올리고, `gradle.properties`의 의존 버전 두 줄은 건드리지 않는다.

---

## 1. 버전 숫자

`gradle.properties` 한 파일이 전부다.

```properties
version=2.0.0                  # 이 저장소의 버전
forgeFrameworkVersion=2.0.0    # 커널이 안 바뀌었으면 그대로 둔다
forgeCliVersion=2.0.0          # CLI가 안 바뀌었으면 그대로 둔다
```

**세 자리를 유지한다**(`2.0`이 아니라 `2.0.0`). jpackage의 `--app-version`은
플랫폼마다 형식 요구가 조금씩 다른데, `major.minor.patch`는 어디서나 통한다.
major는 1 이상이어야 한다 — macOS는 `0.x`를 거부한다.

`build.gradle.kts`나 워크플로에는 **버전이 하드코딩되어 있지 않다.**
`appVersion = project.version.toString()`이고, 워크플로도 `gradle.properties`에서
읽는다. 그래서 버전 상향에 손댈 파일은 이 하나뿐이다.

---

## 2. 코드와 문서

| 대상 | 확인할 것 |
|---|---|
| `README.md` · `README.en.md` | **두 벌을 같이** 고친다. 버전 표기, 앱 목록과 개수, 터미널 명령어 수, 시나리오, 프로젝트 구조 트리 |
| `docs/RELEASE_NOTES_<버전>.md` | 한국어 본문 + `<details>` 접이식 영문 요약. 끝은 `## 파일` · `## 관련 저장소` 표 |
| `docs/COMMIT_PLAN_<버전>.md` | 커밋 순서. **한 파일은 한 커밋에만** 넣어야 `git add <경로>`로 그대로 따라 할 수 있다 |

README 두 벌 중 한쪽에 옛 설명이 남는 사고가 반복해서 났다. 고칠 때마다
두 파일을 같이 열 것.

파일명은 ASCII로 둔다 — macOS NFD 파일명이 Linux 쪽 git에서 NFC로 트래킹된 항목과
중복 등록되는 문제 때문이다.

---

## 3. 로컬 검증

```bash
./gradlew build            # 경고 0건(-Xlint:all -Werror)이 기준선
./scripts/run.sh           # 실제로 띄워 본다
./scripts/package.sh       # .dmg 가 나오는지
```

**GUI는 사람이 봐야 한다.** 컴파일과 스타일시트 검사로는 레이아웃 회귀가 잡히지
않는다. 지금까지 발견된 회귀는 전부 실제 화면에서만 드러났다 — 배경화면의 최소
크기가 메뉴바와 Dock을 잘라 낸 것, Dock 확대가 겨냥을 방해한 것, 둘 다 그랬다.

`package.sh`까지 로컬에서 돌려 보는 이유는, 여기서 깨지면 CI에서도 똑같이 깨지는데
**태그를 지웠다 다시 미는 것보다 로컬에서 고치는 쪽이 훨씬 빠르기** 때문이다.

---

## 4. 커밋과 푸시

`docs/COMMIT_PLAN_<버전>.md`의 순서대로. 끝나면 확인:

```bash
git status                 # working tree clean
./gradlew build            # 마지막으로 한 번 더
git push origin <가지>
```

**푸시가 태그보다 먼저다.** 태그가 가리키는 커밋이 원격에 없으면 Actions가
체크아웃할 것이 없다.

`.github/` 아래를 건드렸다면 인증 수단에 **`workflow` 스코프**가 필요하다.
HTTPS + Personal Access Token을 쓰는데 스코프가 없으면
`refusing to allow a Personal Access Token to create or update workflow`로 거절당한다.

---

## 5. 시험 실행 (권장)

태그를 밀기 전에 릴리즈 없이 산출물만 굽는다.

**Actions** 탭 → **릴리즈 빌드** → **Run workflow** → 입력칸 둘은 비워 둔 채 실행.

입력칸(`framework_ref` · `cli_ref`)은 **의존 저장소를 아직 릴리즈하지 않았을 때**
쓴다. 커널 2.0을 `feature/xxx` 가지에서 개발 중이라면 거기에 그 가지 이름을 넣어
ForgeOS가 그 커널로 빌드되는지 미리 볼 수 있다. 비워 두면 각 저장소의 기본
브랜치를 받는다.

> **Run workflow 버튼이 안 보이면** 워크플로 파일이 저장소의 **기본 브랜치**에
> 아직 없는 것이다. `workflow_dispatch`는 기본 브랜치의 워크플로만 목록에 띄운다.

---

## 6. 태그

```bash
git tag -a v2.0.0 -m "ForgeOS 2.0.0 — <한 줄 요약>"
git push origin v2.0.0
```

- 태그 이름은 `v` + 세 자리. 워크플로의 `on: push: tags: ["v*"]`가 이걸 본다
- 릴리즈 본문의 문서 링크는 태그에서 `v`를 뗀 값을 쓴다
  (`v2.0.0` → `docs/RELEASE_NOTES_2.0.0.md`). 노트 파일명을 그 규칙에 맞춰 둘 것
- **이전 태그는 지우지 않는다.** `v1.0`도 `v1.1.0`도 그대로 둔다

러너 넷이 각자 10~20분쯤 굽는다. `fail-fast: false`라 하나가 빨개져도 나머지는
끝까지 가므로 **넷을 다 확인**할 것.

---

## 7. 릴리즈 게시

넷이 끝나면 `릴리즈에 첨부` 잡이 **드래프트**를 만든다. Releases에서 확인하고,
본문에 릴리즈 노트 요약을 붙인 뒤 직접 게시한다.

드래프트로 두는 이유는, 산출물이 실제로 실행되는지 한 번 받아 보고 게시하라는
것이다. 특히 서명하지 않은 macOS 앱은 격리 속성 우회 안내가 본문에 남아 있는지
확인할 것.

---

## 절대 바꾸지 말 것

| 값 | 위치 | 이유 |
|---|---|---|
| `--win-upgrade-uuid` | `build.gradle.kts` | 바꾸는 순간 새 버전이 이전 버전을 덮어쓰지 않고 **나란히 설치**된다. 버전이 아무리 올라가도 이 값은 고정 |
| `--mac-package-identifier` | `build.gradle.kts` | macOS가 같은 앱으로 인식하는 기준. 바꾸면 별개 앱이 된다 |

---

## 태그를 잘못 밀었을 때

```bash
git tag -d v2.0.0                  # 로컬
git push origin :refs/tags/v2.0.0  # 원격
```

이미 만들어진 드래프트 릴리즈는 GitHub 웹에서 먼저 지운다. 안 지우고 태그를 다시
밀면 같은 이름의 릴리즈가 하나 더 생긴다.

---

## 실패했을 때 보는 곳

| 증상 | 원인 |
|---|---|
| `Repository not found` (forge-framework / forge-cli) | 두 저장소가 비공개다. 공개로 바꾸거나 토큰을 넘겨야 한다 |
| `Could not resolve io.github.jongwoo0101:forgeframework:<버전>` | 의존 저장소에 그 버전이 없다. `gradle.properties`의 버전과 그쪽 `gradle.properties`의 `version`이 같은지 확인 |
| Windows에서 `candle.exe` 관련 오류 | WiX 설치 단계가 실패했다. 러너 이미지가 바뀐 것 |
| `릴리즈에 첨부` 잡의 403 | **Settings → Actions → General → Workflow permissions** 확인 |
| `.dmg`는 나왔는데 열리지 않음 | 서명하지 않은 앱의 격리 속성. `xattr -dr com.apple.quarantine /Applications/ForgeOS.app` |
