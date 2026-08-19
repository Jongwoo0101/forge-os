<div align="center">

<img src="assets/forgeOS-banner.svg" alt="ForgeOS — JavaFX Desktop Environment for the ForgeFramework Kernel" width="860">

**🇰🇷 한국어 문서** · [English](README.en.md)

</div>

---

# ForgeOS

ForgeOS는 [ForgeFramework](https://github.com/Jongwoo0101/forge-framework) 커널 위에
올라가는 JavaFX 데스크탑 환경입니다. 프로세스가 스케줄러를 오가는 것을 표로 보고,
메모리 사용량을 도넛 게이지로 읽고, 교착 상태에 빠진 프로세스들이 서로를 기다리는
고리를 **그림으로** 볼 수 있습니다.

이 저장소에는 커널 코드도, 명령어 파서 코드도 없습니다. 둘 다 Maven 아티팩트로
가져오며, **의존은 단방향**입니다 — ForgeOS가 사라져도 커널은 아무 영향을 받지 않습니다.

```text
                       ForgeFramework          ← 커널 (별도 저장소)
                              ▲
                              │  requires
                ┌─────────────┼─────────────┐
                │             │             │
          ★ ForgeOS ★     ForgeCLI      ForgeStudio
                │             ▲
                └─────────────┘
                   requires — 터미널 앱이 CLI의 명령어 계층을 그대로 씁니다
```

터미널 앱은 명령어 파서를 새로 만들지 않고 ForgeCLI의 것을 재사용합니다. 파서를 두 벌
유지하면 "CLI에서는 되는데 GUI에서는 안 되는" 명령이 반드시 생기기 때문입니다.

---

## 목차

- [빠른 시작](#빠른-시작)
- [시네마틱 부팅](#시네마틱-부팅)
- [데스크탑 사용법](#데스크탑-사용법)
  - [메뉴바](#메뉴바)
  - [Dock](#dock)
  - [창 다루기](#창-다루기)
  - [단축키](#단축키)
- [내장 앱](#내장-앱)
  - [터미널](#터미널)
  - [활성 상태 보기](#활성-상태-보기)
  - [Finder](#finder)
  - [교착 상태 관리자](#교착-상태-관리자)
- [테마와 배경화면](#테마와-배경화면)
- [시나리오로 배우기](#시나리오로-배우기)
- [프로젝트 구조](#프로젝트-구조)
- [License](#license)

---

## 빠른 시작

### 요구 사항

- **JDK 21** 이상
- ForgeFramework 커널 `1.0`이 로컬 Maven 저장소(`~/.m2`)에 설치되어 있을 것
- ForgeCLI `1.0.1` 이상이 로컬 Maven 저장소에 설치되어 있을 것

JavaFX는 따로 설치하지 않습니다. Gradle 플러그인(`org.openjfx.javafxplugin`)이 현재
플랫폼(mac-aarch64 · win · linux)에 맞는 런타임을 알아서 가져옵니다.

### 1. 커널을 먼저 설치합니다

```bash
git clone https://github.com/Jongwoo0101/forge-framework.git
cd forge-framework
./gradlew publishToMavenLocal      # 또는 ./scripts/publish.sh
```

### 2. 명령어 계층(ForgeCLI)을 설치합니다

```bash
git clone https://github.com/Jongwoo0101/forge-cli.git
cd forge-cli
./gradlew publishToMavenLocal
```

`io.github.jongwoo0101:forgeframework:1.0`과 `io.github.jongwoo0101:forgecli:1.0.1`이
`~/.m2/repository`에 설치됩니다.

### 3. ForgeOS를 실행합니다

```bash
git clone https://github.com/Jongwoo0101/forge-os.git
cd forge-os

./scripts/run.sh                   # 또는 ./gradlew run
```

> 의존성 해석에서 실패한다면 1·2번 단계의 `publishToMavenLocal`을 건너뛰었을
> 가능성이 큽니다. ForgeCLI는 반드시 `1.0.1` 이상이어야 합니다 — 명령어 계층을
> 외부에 공개한 것이 그 버전부터입니다.

### 산출물

| 명령 | 설명 |
|---|---|
| `./gradlew run` | 개발 중 실행. 가장 빠릅니다. |
| `./gradlew build` | 컴파일과 검증. 경고 0건(`-Xlint:all -Werror`)이 기준선입니다. |

JavaFX 애플리케이션은 모듈 경로에 플랫폼별 런타임이 필요하므로, ForgeCLI와 달리
`java -jar`로 바로 실행되는 단일 jar를 만들지 않습니다.

---

## 시네마틱 부팅

실행하면 곧바로 바탕화면이 뜨지 않습니다. 세 단계를 거칩니다.

### 1단계 — 터미널 부팅 로그

검은 화면에 커널 로그가 한 글자씩 찍힙니다. **연출된 텍스트가 아니라 실제 커널
로그입니다.** 백그라운드 스레드에서 커널이 부팅되는 동안 `EventLogger`가 흘리는
줄을 그대로 받아 씁니다.

```text
=================================================
 ForgeFramework v1.0
 Operating System Kernel Architecture Engine
=================================================
[10:32:11.153] [INFO] 하드웨어 점검 중...
[10:32:11.306] [INFO] 이벤트 로거 초기화 중...
[10:32:11.457] [INFO] 커널 초기화 중...
[10:32:11.607] [INFO] 서브시스템 초기화 중...
[10:32:11.612] [INFO] ProcessManager initialized [Scheduler: Round Robin (RR), timeQuantum: 3]
...
```

### 2단계 — MP4 부팅 애니메이션

텍스트가 페이드 아웃되면 `src/main/resources/assets/forgeOS-Booting-Animation2.mp4`가
`MediaView`로 전체 화면 재생됩니다.

### 3단계 — 데스크탑

영상이 끝나면(`setOnEndOfMedia`) 부드럽게 페이드되며 바탕화면 · 상단 메뉴바 ·
하단 Dock으로 전환됩니다.

> **단계 전환은 시간이 아니라 사건으로 이어집니다.** 고정된 초로 이어 붙이면 반드시
> 어긋납니다 — 커널 부팅 시간은 기계마다 다르고, 타이핑 시간은 로그 길이에 따라
> 달라지기 때문입니다. 1 → 2 는 *커널 부팅 완료 **그리고** 타이핑 큐 소진*,
> 2 → 3 은 *재생 종료 이벤트*가 신호입니다.

`ESC` · `Space` · 클릭 중 아무거나로 건너뛸 수 있습니다. 커널 부팅 자체는 건너뛸 수
없으므로, 부팅이 끝나기 전에 누르면 완료 직후 곧바로 데스크탑으로 넘어갑니다.

---

## 데스크탑 사용법

### 메뉴바

화면 최상단의 반투명 바입니다. 왼쪽은 "지금 무엇을 쓰고 있는가", 오른쪽은
"지금 커널이 어떤 상태인가"를 보여 줍니다.

| 위치 | 내용 |
|---|---|
| 왼쪽 | Forge 마크 · 활성 창 이름 · `창` 메뉴 |
| 오른쪽 | 프로세스 수 · 메모리 사용률 · 가동 시간 · 테마 토글 · 시계 |

오른쪽 세 칩은 1초마다 `PS` · `MEMINFO` · `UPTIME` 시스템 콜로 갱신됩니다. 어느 앱을
쓰고 있든 커널 상태가 항상 한 줄로 보이는 것이 이 시뮬레이터의 핵심이라고 보고
자리를 내줬습니다.

### Dock

하단 중앙의 유리 재질 바. 아이콘에 포인터를 올리면 **바로 위 가운데에 앱 이름**이
뜹니다. 아이콘 크기는 변하지 않습니다.

- 아이콘 클릭 → 앱 실행. 이미 열려 있으면 앞으로 가져옵니다.
- 활성 창의 아이콘을 다시 클릭 → 최소화.
- 실행 중인 앱은 테두리가 청록으로 바뀌고 아래에 점이 찍힙니다.

처음에는 macOS처럼 포인터에 가까운 아이콘을 키웠지만, 어떤 감쇠 곡선을 써도 겨냥한
것 말고 **옆 아이콘까지 함께 들리는** 느낌을 지울 수 없었습니다. 아이콘이 네 개뿐이고
간격이 넓은 Dock에서는 확대가 겨냥을 돕기는커녕 "무엇을 가리키고 있는가"를 오히려
흐립니다. 그래서 크기는 건드리지 않고 이름만 띄웁니다 — 답해야 할 질문에 정확히
답하면서 화면은 가만히 있습니다.

이름표는 `Tooltip`이 아니라 Dock 안의 라벨입니다. JavaFX 툴팁은 포인터를 따라
아래·오른쪽에 뜨는데, Dock은 화면 맨 아래에 있으므로 그러면 아이콘을 가리거나 화면
밖으로 밀립니다.

### 창 다루기

JavaFX `Stage`를 여러 개 띄우는 대신 데스크탑 위의 **노드**로 창을 구현했습니다
(Custom MDI). 그래야 창이 데스크탑 영역 안에 갇히고, Dock으로 빨려 들어가고,
반투명 재질 위에 겹쳐 보일 수 있습니다.

| 동작 | 방법 |
|---|---|
| 이동 | 제목 표시줄 드래그. 잡은 지점이 유지되며, 화면 밖으로 끌면 고무줄처럼 저항하다 되돌아옵니다. |
| 크기 조절 | 창 가장자리 8방향 드래그 |
| 전체 화면 | 초록 버튼 또는 제목 표시줄 더블클릭 |
| 최소화 | 노랑 버튼 → Dock으로 빨려 들어갑니다. 아이콘을 다시 누르면 복원. |
| 닫기 | 빨강 버튼 |
| 앞으로 가져오기 | 창 아무 곳이나 클릭 |

신호등 버튼은 macOS의 배치와 동작을 따르되 색은 Forge 팔레트입니다 —
🔴 Forge Ember · 🟡 Molten Gold · 🟢 Electric Cyan. 글리프는 세 버튼 중 하나에라도
포인터가 올라가면 다 같이 나타납니다.

### 단축키

| 키 | 동작 |
|---|---|
| `Cmd/Ctrl + W` | 활성 창 닫기 |
| `Cmd/Ctrl + M` | 활성 창 최소화 |
| `Cmd/Ctrl + Shift + L` | 라이트 ↔ 다크 테마 전환 |
| `ESC` · `Space` · 클릭 | 부팅 연출 건너뛰기 |

---

## 내장 앱

네 앱 모두 커널이 돌려주는 **불변 record DTO를 문자열로 만들지 않고 그대로** 표와
도형에 바인딩합니다. CLI가 텍스트로 포맷한 것을 GUI가 다시 파싱하면, 포맷이 바뀔
때마다 화면이 깨집니다.

### 터미널

ForgeCLI의 명령어 **33종을 그대로** 쓸 수 있는 앱입니다. 같은 문자열을 넣으면 CLI와
정확히 같은 결과가 나옵니다 — 같은 `CommandRegistry`를 쓰기 때문입니다.

| 기능 | 방법 |
|---|---|
| 명령 실행 | 입력 후 `Enter` |
| 이전 명령 | `↑` · `↓` |
| 명령어 자동 완성 | `Tab` (후보가 여럿이면 목록을 출력) |
| 화면 정리 | `clear` 또는 `Cmd/Ctrl + L` |

```text
forgeframework:/> exec worker 20
forgeframework:/> ps
PID   | STATE      | CPU_TIME | BURST      | NAME
-------------------------------------------------------
1     | RUNNING    | 3        | 20         | worker
```

실패한 명령은 Ember 색으로, 프롬프트는 Cyan으로 구분됩니다. `shutdown`을 입력하면
커널이 내려가고 데스크탑 전체에 종료 화면이 덮입니다.

> 터미널은 라이트 테마에서도 어둡습니다. 셸이 하얘지면 고정폭 출력이 문서처럼
> 보이고, 무엇보다 부팅 콘솔과 같은 물건이라는 감각이 끊깁니다.

### 활성 상태 보기

프로세스 표와 메모리 게이지. 1초마다 갱신됩니다.

| 영역 | 내용 | 쓰는 시스템 콜 |
|---|---|---|
| 표 | PID · 이름 · 상태 배지 · CPU 사용 · 진행 막대 | `PS` |
| 툴바 | 새 프로세스 실행 · 선택 프로세스 강제 종료 · 현재 스케줄러 | `EXEC` `KILL` `SCHEDULER` |
| 도넛 | 물리 프레임 사용률 · 힙 사용량 · TLB 적중률 | `MEMINFO` |

상태 배지는 색이 곧 의미입니다 — `RUNNING`/`READY`는 Cyan, `WAITING`은 Gold,
`TERMINATED`는 Ember. 텍스트를 읽기 전에 상태가 보입니다.

도넛의 강조색도 무엇을 재는지에 따라 다릅니다. 물리 프레임은 소진되면 곤란하므로
Ember, 힙은 주의 신호라 Gold, TLB 적중률은 높을수록 좋은 값이라 Cyan입니다.

> 표가 1초마다 통째로 갈리지만 **선택은 유지됩니다.** 프로세스를 고르고 종료 버튼으로
> 손을 옮기는 사이에 선택이 풀리는 표는 쓸 수가 없습니다.

### Finder

파일 시스템을 macOS 다단 컬럼 뷰로 탐색합니다. 트리 뷰 대신 컬럼 뷰를 고른 이유는
이 파일 시스템이 inode 기반이기 때문입니다. 컬럼 뷰는 "지금 어느 디렉터리 안에
있는가"와 "그 디렉터리에 무엇이 함께 있는가"를 동시에 보여 줍니다.

| 동작 | 결과 | 쓰는 시스템 콜 |
|---|---|---|
| 디렉터리 선택 | 오른쪽에 새 컬럼이 붙습니다 | `LS` |
| 파일 선택 | 오른쪽에 내용 미리보기가 뜹니다 | `CAT` |
| 새 폴더 · 새 파일 | 현재 컬럼 안에 만듭니다 | `MKDIR` `TOUCH` |

### 교착 상태 관리자

왼쪽은 **숫자**(Allocation · Max · Need), 오른쪽은 **관계**(누가 누구를 기다리는가)입니다.
은행원 알고리즘은 행렬을 보는 알고리즘이고 교착 탐지는 그래프를 보는 알고리즘이라,
둘을 나란히 두면 같은 상황을 두 방식으로 동시에 볼 수 있습니다.

| 영역 | 내용 | 쓰는 시스템 콜 |
|---|---|---|
| 툴바 | 은행원 알고리즘 토글 · 희생자 정책 · 탐지 · 복구 | `BANKER` `DETECT` `RECOVER` |
| 왼쪽 표 | 가용/전체 벡터 + 프로세스별 Allocation · Max · Need · 대기 요청 | `RES_INFO` |
| 오른쪽 | Wait-For 그래프 — 프로세스를 원주에 배치하고 대기 간선을 화살표로 | `DETECT` |
| 아래 | PID와 벡터를 넣어 최대 선언 · 요청 · 반납 | `RES_MAX` `RES_REQ` `RES_FREE` |

교착에 얽힌 노드만 Ember로 물들고 후광이 천천히 맥박칩니다. 다른 노드가 전부
무채색이라 눈이 곧바로 그리로 갑니다 — 사이클을 표에서 찾아 헤맬 필요가 없습니다.

> 토글 스위치는 **커널의 값을 되비출 뿐**입니다. 터미널에서 `banker off`를 쳐도 스위치가
> 따라 움직입니다. 스위치가 자기 상태를 들고 있으면 화면과 커널이 어긋납니다.

---

## 테마와 배경화면

메뉴바 오른쪽의 해·달 아이콘 또는 `Cmd/Ctrl + Shift + L`로 라이트 ↔ 다크를 전환합니다.
아이콘은 현재 상태가 아니라 **누르면 갈 곳**을 보여 줍니다.

전환 방식은 루트에 클래스를 붙이는 것이 아니라 **토큰 스타일시트를 통째로 교체**하는
것입니다. 콘텍스트 메뉴 · 툴팁 · 콤보 목록은 각자 자기 `Scene`을 갖기 때문에 루트
클래스로는 따라오지 않지만, 소유 Scene의 스타일시트는 물려받기 때문입니다.

```text
tokens-dark.css / tokens-light.css   색만. 토큰 이름 집합이 정확히 같아야 합니다.
theme.css / apps.css                 구조·치수. 리터럴 색을 쓰지 않습니다.
```

의도적으로 테마를 따르지 않는 곳이 셋 있습니다 — **부팅 화면**(전원을 넣은 기계의
콘솔에 라이트 모드는 없습니다), **터미널**, **로고 마크**(브랜드는 테마가 바뀌어도 같은
물건입니다).

배경화면은 이미지 파일이 아니라 `assets/forgeOS-logo.svg`의 패스를 그대로 옮긴
벡터입니다(`ui/ForgeMark`). JavaFX는 SVG 파일을 읽지 못하고, PNG로 구우면 배경화면
크기에서 뭉개지는 데다 테마에 따라 색을 바꿀 수 없습니다. 크기는 화면 짧은 변에
비례해 따라가고, 마크는 화면 **정중앙**에 놓입니다. 창에 가려지는 것은 배경화면의
정상적인 처지이고, 창이 하나도 없을 때 화면의 축이 잡혀 있는 편이 더 중요합니다.

---

## 시나리오로 배우기

### 시나리오 1 — 스케줄러가 프로세스를 굴리는 것 보기

1. Dock에서 **활성 상태 보기**를 엽니다.
2. 툴바에 이름과 버스트를 넣고 `실행`을 여러 번 눌러 프로세스를 3~4개 만듭니다.
3. 가만히 둡니다. 1초에 한 번 타이머 인터럽트가 돌면서 상태 배지가
   `READY` ↔ `RUNNING`으로 바뀌고 진행 막대가 찹니다.
4. 터미널을 열어 `scheduler fcfs`를 입력하고 표를 다시 봅니다. 선점이 사라져
   한 프로세스가 끝날 때까지 CPU를 놓지 않습니다.

### 시나리오 2 — 교착을 만들고 그래프로 확인하기

**교착 상태 관리자**를 열고 은행원 알고리즘 토글을 **끕니다**(회피가 켜져 있으면
교착이 만들어지지 않습니다). 터미널에서 프로세스를 두 개 만든 뒤 아래 순서로
자원을 요청합니다.

```text
exec p1 30
exec p2 30
res_req 1 6 0 0
res_req 2 4 4 0
res_req 1 4 0 0        ← 대기
res_req 2 0 2 0        ← 대기 → 순환 대기 성립
```

교착 상태 관리자로 돌아오면 두 노드가 Ember로 물들고 서로를 향한 화살표가 고리를
이루고 있습니다. `복구`를 누르면 희생자가 강제 종료되고 고리가 풀립니다.

### 시나리오 3 — 회피가 요청을 막는 순간

같은 앱에서 토글을 **켠 채로** 아래를 실행합니다.

```text
exec p1 30
exec p2 30
res_max 1 7 5 3
res_max 2 3 2 2
res_req 1 7 4 3        ← GRANTED
res_req 2 3 2 2        ← BLOCKED_UNSAFE : 자원은 남아 있지만 주면 불안전해진다
```

자원 표의 Available은 아직 여유가 있는데도 요청이 막힙니다. 이것이 회피가 실제로
일하는 순간입니다. 토글을 끄고 같은 요청을 다시 하면 통과하고, 그 뒤 시나리오 2의
교착이 가능해집니다.

### 시나리오 4 — TLB 적중률이 오르는 것 보기

**활성 상태 보기**를 열어 둔 채 터미널에서 아래를 실행합니다.

```text
exec app 20
malloc 1 8
translate 1 0          ← MISS
translate 1 0          ← HIT
translate 1 0
```

TLB 도넛이 차오릅니다. TLB 용량이 4이므로 서로 다른 페이지를 5개 이상 훑으면 다시
적중률이 떨어집니다 — 교체가 일어나는 것을 눈으로 확인할 수 있습니다.

---

## 프로젝트 구조

```text
forge-os/
├── assets/                              # 로고·배너 SVG (README 및 릴리즈용)
├── build.gradle.kts                     # 커널·CLI를 Maven 아티팩트로 의존, JavaFX 플러그인
├── settings.gradle.kts
├── gradle.properties                    # forgeFrameworkVersion · forgeCliVersion · javafxVersion
├── docs/
│   └── COMMIT_PLAN_initial.md
├── scripts/
│   ├── build.sh
│   └── run.sh
└── src/main/
    ├── java/
    │   ├── module-info.java             # requires forgeframework, forgeframework.cli, javafx.*
    │   └── forgeos/
    │       ├── Launcher.java            # 진입점 — JavaFX 런타임 누락 시 원인을 볼 수 있게 한 겹
    │       ├── ForgeOsApp.java          # 창 하나 만들고 부팅 시퀀스를 돌린다. 그게 전부다
    │       ├── core/
    │       │   └── KernelService.java   # 커널과 UI 사이의 유일한 통로 (스레드 경계·갱신 펄스)
    │       ├── boot/
    │       │   ├── BootSequence.java    # 3단계 오케스트레이션
    │       │   ├── BootConsole.java     # 타이프라이터 (유입과 출력을 큐로 분리)
    │       │   └── BootVideo.java       # MediaView (jar 안 미디어 문제 처리)
    │       ├── desktop/
    │       │   ├── DesktopPane.java     # 레이어 순서와 작업 영역 계산
    │       │   ├── Wallpaper.java       # 로고 배경화면
    │       │   ├── MenuBarView.java     # 상단 메뉴바
    │       │   └── DockView.java        # 하단 Dock (확대)
    │       ├── wm/
    │       │   ├── WindowManager.java   # 창 레이어 — 열기·포커스·최소화·닫기
    │       │   ├── ForgeWindow.java     # 창 하나 (드래그·리사이즈·전체화면)
    │       │   └── TrafficLights.java   # 신호등 버튼
    │       ├── app/
    │       │   ├── ForgeApp.java        # id() · title() · iconPath() · launch()
    │       │   ├── AppInstance.java     # (화면, 정리 작업) 한 쌍
    │       │   ├── AppCatalog.java      # Dock 순서 = 목록 순서
    │       │   ├── terminal/            # ForgeCLI 명령어 계층 재사용
    │       │   ├── monitor/             # TableView + 도넛 게이지
    │       │   ├── finder/              # 다단 컬럼 뷰
    │       │   └── deadlock/            # 은행원 토글 + Wait-For 그래프
    │       └── ui/
    │           ├── SpringValue.java     # 스프링 애니메이션
    │           ├── Motion.java          # 전역 모션 상수
    │           ├── ForgeMark.java       # 로고 SVG를 벡터로 다시 세운 것
    │           ├── ThemeManager.java    # 라이트/다크 전환
    │           ├── Glyphs.java          # 아이콘 (SVG 패스)
    │           ├── Styles.java          # CSS 의사 클래스
    │           └── ToggleSwitch.java
    └── resources/
        ├── assets/                      # 부팅 애니메이션 MP4 등 런타임 리소스
        └── forgeos/css/
            ├── tokens-dark.css          # 색만
            ├── tokens-light.css         # 색만
            ├── theme.css                # 셸 구조·치수
            └── apps.css                 # 앱 구조·치수
```

### 설계 원칙

- **커널은 문장을 만들지 않습니다.** 커널은 불변 record DTO만 돌려주고, ForgeCLI는
  그것으로 표를 찍고 ForgeOS는 그것으로 도형을 그립니다. 같은 DTO, 다른 표현입니다.
- **명령어 계층은 새로 만들지 않습니다.** 터미널 앱은 ForgeCLI의 `CommandRegistry`를
  그대로 씁니다. CLI에 명령이 하나 추가되면 GUI에도 자동으로 생깁니다.
- **백그라운드 → FX 스레드 경계는 `KernelService` 한 곳뿐입니다.** 커널 로그는 부팅
  스레드와 타이머 장치 스레드에서 올라오므로 여기서 전부 `Platform.runLater`로
  넘깁니다. 반대로 시스템 콜은 인메모리 연산이라 FX 스레드에서 바로 호출합니다 —
  백그라운드로 빼면 왕복이 생겨 오히려 한 박자 늦게 반응합니다.
- **색과 치수는 CSS에만 있습니다.** 자바 코드는 `styleClass`와 의사 클래스를 붙였다
  뗄 뿐 `setStyle()`을 쓰지 않습니다. 디자인 변경에 컴파일이 필요해지면 안 됩니다.
- **애니메이션은 `Transition`이 아니라 스프링입니다.** 고정 시간 스크립트는 재생 도중
  목표가 바뀌면 속도를 버리고 처음부터 다시 시작해서, 움직이는 창을 다시 잡아채는
  순간 눈에 보이는 턱이 생깁니다. 스프링은 위치와 속도를 계속 들고 있으므로 언제
  방향이 바뀌어도 궤적이 이어집니다.
- **앱 추가는 인터페이스 하나 + 목록 한 줄**입니다. `ForgeApp`을 구현하고
  `AppCatalog.defaults()`에 넣으면 Dock에도 창 관리에도 자동으로 편입됩니다.

---

## 관련 저장소

<table>
  <tr>
    <td width="64" align="center"><img src="assets/forge-framework-logo.svg" alt="" width="48"></td>
    <td>
      <b><a href="https://github.com/Jongwoo0101/forge-framework">forge-framework</a></b><br>
      <sub>커널 엔진 — 본 프로젝트가 의존합니다 ·
      <a href="https://github.com/Jongwoo0101/forge-framework/blob/master/docs/api/README.md">API 문서</a></sub>
    </td>
  </tr>
  <tr>
    <td width="64" align="center"><img src="assets/forge-cli-logo.svg" alt="" width="48"></td>
    <td>
      <b><a href="https://github.com/Jongwoo0101/forge-cli">forge-cli</a></b><br>
      <sub>커널의 명령줄 클라이언트 — 터미널 앱이 이 저장소의 명령어 계층을 재사용합니다</sub>
    </td>
  </tr>
  <tr>
    <td width="64" align="center"><img src="assets/forgeOS-logo.svg" alt="" width="48"></td>
    <td><b>forge-os</b><br><sub>본 저장소 — JavaFX 기반 GUI 운영체제 시뮬레이터</sub></td>
  </tr>
  <tr>
    <td width="64" align="center"></td>
    <td><b>ForgeStudio</b><br><sub>운영체제 교육 · 시각화 플랫폼 (예정)</sub></td>
  </tr>
</table>

---

## License

MIT License
