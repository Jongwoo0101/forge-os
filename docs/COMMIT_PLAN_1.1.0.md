# 커밋 플랜 — ForgeOS 1.1.0

커널·CLI가 `1.1.0`으로 올라가면서 생긴 것을 화면에 반영하고(활성 상태 보기 · Finder ·
메뉴바), 데스크탑에 없던 두 앱(메모장 · Firefox)을 붙였다. 이어서 앱 창을 커널
프로세스와 묶고, 배포용 실행 이미지를 만들었다.

**대상: 신규 13개 · 수정 18개 (총 31개) → 커밋 15개.**

순서는 **아래에서 위로 쌓는다.** 빌드 설정 → 원자재(아이콘 · 토큰 · 스타일) →
기존 앱 개편 → 새 앱 → 등록 → 창↔프로세스 배선 → 배포 → 문서.

## 이 순서가 지키는 두 가지 규칙

1. **모든 커밋 시점에 `./gradlew build`가 통과한다.** 컴파일이 깨지는 커밋이 하나라도
   있으면 나중에 `git bisect`로 회귀를 찾을 수 없다.
2. **한 파일은 정확히 한 커밋에만 등장한다.** 그래서 `git add <경로>`만으로 그대로
   따라 할 수 있다 — `git add -p`로 hunk를 쪼갤 필요가 없다.

두 번째 규칙 때문에 몇 군데는 "이야기"보다 "빌드"를 우선했다. 예를 들어
`build.gradle.kts`에는 버전 상향 · `javafx.web` 추가 · jlink 설정 셋이 함께 들어 있는데,
`javafx.web`을 Firefox 커밋까지 미루면 그 커밋에서 `module-info.java`가 참조하는 모듈이
모듈 경로에 없어 컴파일이 깨진다. 그래서 셋을 1번 커밋에 함께 둔다. jlink 설정이 먼저
들어와도 문제가 없는 이유는, `icon = "assets/icons/…"` 같은 값이 **jpackage를 실제로
부를 때만** 읽히기 때문이다.

파일명을 ASCII로 둔 이유는 형제 저장소와 같다 — macOS NFD 파일명이 Linux 쪽 git에서
NFC로 트래킹된 항목과 중복 등록되는 문제.

> AI가 직접 커밋하지 않는다는 원칙은 그대로다 — 아래는 그대로 붙여 넣을 수 있는 제안이다.

---

## 시작하기 전에

```bash
cd "$HOME/Documents/Java Projects/forge-os"

git status                 # 31개가 다 보이는지 확인
git branch --show-current  # 어느 가지에 쌓을지 확인
```

커밋 하나를 만들 때마다 실수로 딴 파일이 딸려 가지 않았는지 보려면
`git status --short` 를 한 번씩 끼워 넣으면 된다. 스테이징을 잘못했으면
`git restore --staged <경로>` 로 되돌린다(파일 내용은 그대로다).

---

## 1. 빌드 설정

```bash
git add gradle.properties build.gradle.kts
git commit -m "chore(build): 커널·CLI를 1.1.0으로 올리고 javafx.web·jlink를 빌드에 추가한다"
```

`1.0` 코드가 `1.1.0` 커널에서 **그대로 컴파일된다.** record에 필드가 붙었을 뿐이고
ForgeOS는 전부 접근자로 읽기 때문이다. 생성자를 직접 부르는 코드가 한 줄도 없다는
사실이 여기서 값을 한다.

- `forgeFrameworkVersion=1.1.0` · `forgeCliVersion=1.1.0` · `version=1.1.0` —
  세 저장소의 번호를 맞춘다. "어느 커널에 맞는 CLI인가"를 표에서 찾게 하지 않으려는 것
- `javafx.web` — Firefox 앱의 렌더링 엔진. 배포 크기가 100MB 가까이 는다
- `org.beryx.jlink` — OpenJFX가 공식 문서에서 권하는 배포 조합

**검증:** 이 커밋만으로 `./gradlew build`가 통과해야 한다. 통과하지 않으면 아래
커밋들이 전부 "커널 변경 때문인가, 내 변경 때문인가"를 구별할 수 없게 된다.

---

## 2. 아이콘과 토큰

```bash
git add src/main/java/forgeos/ui/Glyphs.java \
        src/main/resources/forgeos/css/tokens-dark.css \
        src/main/resources/forgeos/css/tokens-light.css
git commit -m "feat(ui): 새 아이콘 열 개와 브랜드 보라 토큰을 추가한다"
```

아직 아무 데서도 쓰지 않는 원자재만 넣는다. 아이콘 열(`NOTEPAD` · `BROWSER` · `SAVE` ·
`FORK` · `DISK` · `ARROW_LEFT` · `ARROW_RIGHT` · `HOME` · `CLOSE` · `LOCK`)과
토큰 하나(`-forge-violet`).

- 보라는 **로고의 불티와 같은 값**(`#d946ef`)이다. 새 색을 지어내지 않고 브랜드가
  이미 갖고 있던 네 번째 색에 이름을 붙였다. 게이지가 넷이 되면서 스왑이 자기 색을
  가져야 했고, 프레임(Ember)·힙(Gold)·TLB(Cyan) 옆에 또 하나를 끼우면 셋이 섞여 보인다
- **두 토큰 파일에 같은 이름이 다 있어야 한다.** 하나라도 빠지면 그 토큰만 해석되지
  않아 검은 사각형이 남는다 (현재 각 40개)

---

## 3. 공통 컨트롤 스타일

```bash
git add src/main/resources/forgeos/css/theme.css
git commit -m "feat(ui): 탭·여러 줄 입력·진행 막대를 공통 컨트롤에 추가한다"
```

`TabPane` · `TextArea` · `ProgressBar`가 셋 다 이번에 처음 쓰인다. 앱별 스타일시트가
아니라 **공통 컨트롤 구역**에 넣는 이유는, 활성 상태 보기의 탭과 브라우저의 탭이
같은 물건으로 보여야 하기 때문이다.

- 선택된 탭은 배경이 아니라 **밑줄**로 표시한다. 배경을 칠하면 탭 줄이 두 겹으로
  보이고 창 제목 표시줄과 색이 겹쳐 어디까지가 창인지 흐려진다
- JavaFX가 선택된 탭에 그리는 점선 초점 테두리는 `.focus-indicator`로 지운다
- 리터럴 색은 쓰지 않는다 — 토큰 이름만 참조한다

---

## 4. 앱 스타일

```bash
git add src/main/resources/forgeos/css/apps.css
git commit -m "feat(ui): 활성 상태 보기 확장·메모장·Firefox 구역 스타일을 추가한다"
```

세 구역을 한 커밋에 둔다. 한 파일이라 쪼개려면 hunk 단위로 나눠야 하는데, 스타일시트는
"어느 앱의 규칙인가"가 구역 주석으로 이미 분리돼 있어 얻는 것이 적다.

- 모니터: 탭 안쪽 · 준비 큐 띠(`.queue-chip:on`) · 사이드바 스크롤 · `.accent-violet`
- 메모장: 사이드바 · 고정폭 편집기 · 저장 안 된 파일의 점(`.entry-size:on`)
- Firefox: 얇은 크롬 · 주소창 안의 자물쇠 자리

---

## 5. 도넛 게이지 리팩터링

```bash
git add src/main/java/forgeos/app/monitor/DonutChart.java
git commit -m "refactor(monitor): 도넛 게이지의 지름을 생성자 인자로 뺀다"
```

사이드바에 게이지가 셋에서 넷으로 늘어난다. 112px짜리 넷은 세로가 모자란다.
`SIZE` 상수를 생성자 인자로 바꾸되 **두께(11px)는 그대로 둔다** — 지름을 따라
얇아지면 색이 안 읽힌다.

기능 변화가 없는 커밋을 따로 두는 이유는, 다음 커밋의 diff에서 "게이지가 하나 늘었다"와
"게이지 클래스가 바뀌었다"가 섞이지 않게 하려는 것이다.

---

## 6. 활성 상태 보기 전면 개편

```bash
git add src/main/java/forgeos/app/monitor/ActivityMonitorView.java \
        src/main/java/forgeos/app/monitor/ActivityMonitorApp.java
git commit -m "feat(monitor): 1.1.0의 스케줄러·스왑·fork를 활성 상태 보기에 전면 반영한다"
```

이 릴리즈에서 가장 큰 커밋이다. 앱이 답해야 할 질문이 하나에서 둘로 갈라졌고,
그래서 탭도 둘이 되었다.

- **프로세스 탭** — PPID · 우선순위 · 큐 등급 열, 우선순위 입력칸과 복제 버튼과
  스케줄러 6종 선택기, 행 우클릭 메뉴, 표 아래 준비 큐 띠(`SchedulerDto.queues`)
- **메모리 탭** — 프레임 테이블(`FRAMETABLE`)과 선택 프로세스의 페이지
  테이블(`PAGETABLE`). 메모리 탭이 선택되었을 때만 두 시스템 콜을 부른다
- **사이드바** — 스왑 게이지, 교체 정책 선택기(`SWAPINFO policy …`), 폴트 통계.
  게이지 넷 + 정책 상자가 작은 창에서 잘리지 않도록 `ScrollPane`으로 감쌌다
- 창 기본 크기 920×540 → 1020×660

주의할 점 둘:

- **`syncing` 플래그.** 선택기는 커널 값을 되비추면서 동시에 입력을 받는다. 되비추는
  `setValue`도 변경 이벤트를 쏘므로 플래그가 없으면 1초마다 "지금 값으로 바꿔라"는
  시스템 콜이 날아간다
- **커널 이름을 인자 접두사로 맞추지 않는다.** `scheduler rr`의 이름은
  `"Round Robin (RR)"`이라 `"rr"`로 시작하지 않는다. `Algorithm(token, kernelPrefix, label)`로
  분리한 이유다

---

## 7. Finder

```bash
git add src/main/java/forgeos/app/finder/FinderView.java
git commit -m "feat(finder): 디스크 이미지 기록 버튼을 추가한다"
```

`SYNC` 한 번. 결과를 경로 표시줄에 쓴다(`disk.img · 1024바이트 · 블록 3/16 · inode 2/16`).

디스크 이미지가 없을 때를 **실패로 보고하지 않는다.** 이미지가 없는 것은 오류가 아니라
기본 설정(`diskImagePath` 기본값 `null`)이고, 대신 무엇을 해야 영속되는지를 말해 준다.

---

## 8. 메뉴바

```bash
git add src/main/java/forgeos/desktop/MenuBarView.java
git commit -m "feat(desktop): 메뉴바에 스왑·페이지 폴트 칩을 추가한다"
```

`MEMINFO`를 이미 1초마다 부르고 있으므로 시스템 콜이 늘지 않는다. 스왑이 꺼진 커널
(`swapSlots = 0`)에서는 `setVisible(false)` + `setManaged(false)`로 칩 자체를 감춘다 —
항상 `0`인 숫자는 자리만 먹고 아무것도 알려 주지 않는다.

---

## 9. `ForgeApp` 인터페이스 확장

```bash
git add src/main/java/forgeos/app/ForgeApp.java
git commit -m "feat(app): ForgeApp에 메모리 발자국을 default 메서드로 추가한다"
```

**이 커밋이 11번(Firefox)보다 먼저 와야 한다.** `FirefoxApp`이 `memoryFootprint()`를
`@Override`하는데, 인터페이스에 그 메서드가 없으면 컴파일이 깨진다.

`default` 메서드라 기존 구현체 다섯은 한 줄도 고칠 필요가 없다. 기본값 4바이트는
프레임 딱 한 장이다 — 기본 커널의 물리 메모리가 4바이트 프레임 16장, 즉 64바이트뿐이라
값이 이렇게 작아야 한다.

---

## 10. 메모장

```bash
git add src/main/java/forgeos/app/notepad/
git commit -m "feat(notepad): 커널 파일 시스템 위에서 도는 메모장을 추가한다"
```

`AppCatalog` 등록은 12번에서 한다. 이 시점에는 Dock에 뜨지 않지만 컴파일은 통과한다.

- **대화상자를 쓰지 않는다.** 새 파일 이름은 툴바 입력칸으로 받고, 저장하지 않은
  변경은 경로별 초안(`drafts`/`saved` 두 맵)으로 들고 있는다. 창을 하나라도 덜 여는
  것은 `1.0` 릴리즈 노트의 "알려진 문제"(종료 시 네이티브 창 크래시)에 대한 조치이기도 하다
- `WRITE`는 내용을 통째로 덮어쓴다(커널 `FileSystemManager.write`가 블록을 재할당한다).
  부분 저장이 아니므로 편집기 전체 텍스트를 그대로 보낸다

---

## 11. Firefox

```bash
git add src/main/java/forgeos/app/browser/ src/main/java/module-info.java
git commit -m "feat(browser): WebView 기반 기본 브라우저 Firefox를 추가한다"
```

`module-info.java`의 `requires javafx.web`가 이 커밋에 함께 있어야 한다. 1번에서
`build.gradle.kts`에 모듈을 이미 얹어 뒀으므로 여기서 모듈 경로가 맞아떨어진다.

- `BrowserTab`은 **`final`이어야 한다.** 생성자가 자기 메서드(`setContent` 등)를 부르는데
  상속 가능한 클래스에서 그러면 `-Xlint:this-escape`가 경고를 낸다. 이 프로젝트의
  기준선은 경고 0건이다
- `setCreatePopupHandler`로 `window.open`과 `target="_blank"`를 **새 탭**으로 받는다.
  데스크탑 안에서 네이티브 창을 또 띄우면 우리 창 관리자가 모르는 창이 생긴다
- 마지막 탭이 닫히면 빈 탭을 다시 연다. 빈 창은 브라우저가 아니라 고장으로 보인다
- 시작 페이지는 원격 주소가 아니라 내장 문서다. 네트워크가 없는 자리에서 앱을 열자마자
  오류 화면이 뜨면, 처음 쓰는 사람에게는 앱이 고장 난 것과 구별되지 않는다

---

## 12. Dock 등록

```bash
git add src/main/java/forgeos/app/AppCatalog.java
git commit -m "feat(desktop): 메모장과 Firefox를 Dock에 등록한다"
```

한 줄짜리 커밋 둘을 하나로 합친 것이다. `AppCatalog`는 두 앱을 동시에 참조하므로
10번이나 11번 어느 한쪽에 넣으면 아직 없는 클래스를 import하게 되어 컴파일이 깨진다.

목록의 **순서가 곧 Dock의 순서**다. 메모장은 Finder 바로 뒤 — 파일을 찾는 일과 파일을
쓰는 일은 이어진 하나의 동작이다. Firefox는 커널과 무관한 유일한 앱이라 커널 계열 앱들과
교착 상태 관리자 사이에 선을 긋듯 놓았다.

---

## 13. 앱 창 ↔ 커널 프로세스

```bash
git add src/main/java/forgeos/app/AppProcessTable.java \
        src/main/java/forgeos/wm/WindowManager.java
git commit -m "feat(wm): 앱 창을 커널 프로세스와 1:1로 묶는다"
```

여기까지 오면 앱이 여섯 개인데 **커널은 앱이 하나도 열려 있다는 사실을 모른다.**
Firefox를 띄워도 활성 상태 보기의 표가 비어 있는 상태를 고친다.

- 창을 열면 `EXEC` + `MALLOC`, 닫으면 `KILL`. 메모리는 따로 반납하지 않는다 —
  커널이 종료 리스너에서 `releaseProcess`로 주소 공간을 통째로 회수한다
- **앱 프로세스는 만들자마자 `io_req … keyboard`로 재운다.** 재우지 않으면 FCFS에서
  첫 번째로 열린 앱이 CPU를 영원히 붙들어 사용자가 만든 프로세스가 하나도 진행되지
  않는다. GUI 앱이 입력을 기다리는 것과 같은 모델이라 편법도 아니다
- 매 갱신 펄스마다 깨어난 앱 프로세스를 **다시 재운다**. `type` 명령이 키보드 대기열
  맨 앞을 깨우기 때문이다
- 프로세스가 사라지면(사용자가 표에서 죽였으면) 창을 닫는다. 표와 화면이 양방향으로
  통해야 표가 장식이 아니게 된다

> `WindowManager.open`에서 **창을 화면에 올린 다음** 프로세스를 만든다. 커널이 없거나
> 내려간 상태에서도 창은 떠야 하기 때문이다.

---

## 14. 배포

```bash
git add .github/workflows/release.yml scripts/package.sh assets/icons/
git commit -m "build(release): jlink·jpackage 산출물을 GitHub Actions로 굽는다"
```

아이콘 셋은 `assets/forgeOS-logo.svg`를 그대로 래스터화한 것이다. 이 SVG는 이미
둥근 사각형 배경을 가진 앱 아이콘 형태라 따로 합성할 것이 없었다.

- **jlink 이미지는 크로스 컴파일이 안 되므로** 워크플로가 러너 넷(mac arm64 · mac x64 ·
  win · linux)에서 각자 굽는다. 로컬에서는 `./scripts/package.sh`가 이 플랫폼용 하나만 만든다
- 워크플로가 저장소 **셋**을 체크아웃한다. 커널과 CLI가 Maven Central에 없어
  `publishToMavenLocal`을 먼저 돌려야 하기 때문
- 의존 저장소의 `ref`는 **비워 둔다.** `actions/checkout`이 그 저장소의 기본 브랜치를
  받는다 — forge-framework는 `master`, forge-cli는 `main`이라 이름을 박아 두면 한쪽이
  조용히 깨진다
- `--win-upgrade-uuid`는 **절대 바꾸지 않는다.** 바꾸면 새 버전이 이전 버전을
  덮어쓰지 않고 나란히 설치된다
- 서명·공증은 하지 않는다. 워크플로에 자리만 비워 두고, 우회 방법을 릴리즈 본문에 적는다

> `.github/` 아래 파일을 푸시하려면 인증 수단에 **workflow 권한**이 있어야 한다.
> HTTPS + Personal Access Token을 쓴다면 그 토큰에 `workflow` 스코프가 필요하고,
> 없으면 `refusing to allow a Personal Access Token to create or update workflow`로
> 거절당한다. SSH나 `gh auth login`은 그대로 된다.

---

## 15. 문서

```bash
git add README.md README.en.md docs/RELEASE_NOTES_1.1.0.md docs/COMMIT_PLAN_1.1.0.md
git commit -m "docs: README를 1.1.0 기준으로 고치고 릴리즈 노트·커밋 플랜을 추가한다"
```

README 두 벌을 **같이** 고친다. `1.0` 작업에서 Dock 확대를 없앨 때도, 배경화면을
중앙으로 옮길 때도 한쪽에 옛 설명이 그대로 남을 뻔했다.

| 위치 | 내용 |
|---|---|
| 빠른 시작 | **빌드 없이 받기** — 릴리즈 설치본 표와 macOS 격리 속성 우회 안내 |
| 요구 사항 · 설치 | `1.0`/`1.0.1` → `1.1.0` (세 저장소 번호 통일 안내 포함) |
| 부팅 로그 예시 | `ForgeFramework v1.0` → `v1.1.0` |
| 내장 앱 | "네 앱" → "여섯 앱 중 다섯", **앱은 커널 프로세스입니다** 절 신설 |
| 터미널 | 명령어 33종 → 42종, 아홉 개가 코드 변경 없이 들어온 경위 |
| 활성 상태 보기 | 탭 둘 · 새 열 · 게이지 넷 · 교체 정책으로 전면 교체 |
| Finder · 신규 절 | 메모장 · Firefox 추가, Finder에 `SYNC` 행 |
| 시나리오 | `fork`/COW · 페이지 폴트 · 영속화 · 앱 프로세스를 더해 8개로 |
| 프로젝트 구조 | `notepad/` · `browser/` · `AppProcessTable`, `.github/`, `assets/icons/` |

---

## 마지막 확인

```bash
git status                 # "nothing to commit, working tree clean" 이어야 한다
git log --oneline -15      # 15개가 위 순서의 역순으로 보인다
./gradlew build            # 경고 0건
```

---

## 푸시와 태그

```bash
git push origin <현재-가지>
```

태그는 **푸시가 끝난 뒤에** 붙인다. 태그가 가리키는 커밋이 원격에 없으면 Actions가
체크아웃할 것이 없다.

```bash
git tag -a v1.1.0 -m "ForgeOS 1.1.0 — 메모장 · Firefox · 활성 상태 보기 전면 개편"
git push origin v1.1.0
```

`v1.0`은 그대로 둔다.

### 태그를 밀기 전에 한 번 시험 실행할 것

워크플로에 `workflow_dispatch`를 넣어 둔 이유가 이것이다. 태그 없이 산출물만 굽고
릴리즈는 만들지 않는다.

1. GitHub의 **Actions** 탭 → 왼쪽 목록에서 **릴리즈 빌드** → **Run workflow**
2. 두 입력칸은 비워 둔 채 실행 (비우면 기본 브랜치를 받는다)
3. 러너 넷이 전부 초록이면 각 잡의 **Artifacts**에서 산출물을 내려받아 확인
4. 확인이 끝나면 위의 태그 명령을 실행

> **Run workflow 버튼이 안 보이면** 워크플로 파일이 저장소의 **기본 브랜치**에 아직
> 없는 것이다. `workflow_dispatch`는 기본 브랜치에 있는 워크플로만 목록에 띄운다.
> 태그로 도는 것(`on: push: tags`)은 기본 브랜치와 무관하게, **태그가 가리키는 커밋에
> 들어 있는 워크플로 파일**로 실행된다.

태그 실행이 끝나면 **Releases**에 드래프트가 생긴다. 내용을 확인하고 직접 게시하면 된다.

### 실패했을 때 보는 곳

| 증상 | 원인 |
|---|---|
| `Repository not found` (forge-framework/forge-cli) | 두 저장소가 비공개다. 공개로 바꾸거나 토큰을 넘겨야 한다 |
| `Could not resolve io.github.jongwoo0101:forgeframework` | `publishToMavenLocal` 단계가 실패했다. 그 단계 로그를 먼저 본다 |
| Windows에서 `candle.exe` 관련 오류 | WiX 설치 단계가 실패했다. 러너 이미지가 바뀐 것 |
| `release` 잡의 403 | 저장소 **Settings → Actions → General → Workflow permissions**를 확인 |

### 태그를 잘못 밀었을 때

```bash
git tag -d v1.1.0                  # 로컬에서 지우고
git push origin :refs/tags/v1.1.0  # 원격에서도 지운다
```

이미 만들어진 드래프트 릴리즈는 GitHub 웹에서 지운다. 지우기 전에 태그를 다시 밀면
같은 이름의 릴리즈가 하나 더 생긴다.
