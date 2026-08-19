# ForgeOS 1.0

**ForgeFramework 커널이 화면을 갖게 되었습니다**

ForgeOS는 [ForgeFramework](https://github.com/Jongwoo0101/forge-framework) 커널 위에
올라가는 JavaFX 데스크탑 환경입니다. 프로세스가 스케줄러를 오가는 것을 표로 보고,
메모리 사용량을 도넛 게이지로 읽고, **교착 상태에 빠진 프로세스들이 서로를 기다리는
고리를 그림으로** 볼 수 있습니다.

같은 커널, 같은 DTO입니다. ForgeCLI가 표를 찍는다면 ForgeOS는 도형을 그립니다.

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

---

## 실행하기

JavaFX 애플리케이션은 모듈 경로에 플랫폼별 런타임이 필요하므로, ForgeCLI와 달리
`java -jar`로 바로 실행되는 단일 jar를 제공하지 않습니다. 소스에서 실행해 주세요.

```bash
# 1) 커널
git clone https://github.com/Jongwoo0101/forge-framework.git
cd forge-framework && ./gradlew publishToMavenLocal

# 2) 명령어 계층
git clone https://github.com/Jongwoo0101/forge-cli.git
cd ../forge-cli && ./gradlew publishToMavenLocal

# 3) ForgeOS
git clone https://github.com/Jongwoo0101/forge-os.git
cd ../forge-os && ./scripts/run.sh
```

**요구 사항**

| 항목 | 버전 |
|---|---|
| JDK | **21** 이상 |
| ForgeFramework | `1.0` |
| ForgeCLI | **`1.0.1` 이상** — 명령어 계층을 공개한 것이 이 버전부터입니다 |
| JavaFX | `21.0.5` (Gradle 플러그인이 자동으로 가져옵니다. 별도 설치 불필요) |

---

## 시네마틱 부팅

실행하면 곧바로 바탕화면이 뜨지 않습니다.

| 단계 | 내용 |
|---|---|
| 1 | **터미널 부팅 로그** — 검은 화면에 커널 로그가 한 글자씩 찍힙니다 |
| 2 | **MP4 부팅 애니메이션** — 전체 화면 재생 |
| 3 | **데스크탑** — 바탕화면 · 상단 메뉴바 · 하단 Dock으로 페이드 |

1단계의 로그는 **연출된 텍스트가 아니라 백그라운드에서 실제로 부팅되는 커널이 흘리는
로그**입니다.

```text
=================================================
 ForgeFramework v1.0
 Operating System Kernel Architecture Engine
=================================================
[10:32:11.153] [INFO] 하드웨어 점검 중...
[10:32:11.607] [INFO] 서브시스템 초기화 중...
[10:32:11.612] [INFO] ProcessManager initialized [Scheduler: Round Robin (RR), timeQuantum: 3]
...
```

단계 전환은 시간이 아니라 **사건**으로 이어집니다. 고정된 초로 이어 붙이면 반드시
어긋납니다 — 커널 부팅 시간은 기계마다 다르고, 타이핑 시간은 로그 길이에 따라 다릅니다.
1 → 2 는 *커널 부팅 완료 **그리고** 타이핑 큐 소진*, 2 → 3 은 *재생 종료 이벤트*가
신호입니다.

`ESC` · `Space` · 클릭으로 건너뛸 수 있습니다.

---

## 내장 앱 4종

| 앱 | 하는 일 | 쓰는 시스템 콜 |
|---|---|---|
| **터미널** | ForgeCLI 명령어 33종을 GUI 창 안에서 그대로 | `forgecli` 의 `CommandRegistry` |
| **활성 상태 보기** | 프로세스 표 + 메모리·TLB 도넛 게이지 (1초 갱신) | `PS` · `MEMINFO` · `SCHEDULER` · `EXEC` · `KILL` |
| **Finder** | 파일 시스템 다단 컬럼 뷰 + 내용 미리보기 | `LS` · `CAT` · `MKDIR` · `TOUCH` |
| **교착 상태 관리자** | 은행원 알고리즘 토글 + Wait-For 그래프 | `RES_INFO` · `RES_REQ` · `RES_FREE` · `RES_MAX` · `BANKER` · `DETECT` · `RECOVER` |

터미널은 명령어 파서를 **한 줄도 새로 쓰지 않았습니다.** ForgeCLI `1.0.1`이 공개한
명령어 계층을 그대로 씁니다. 같은 문자열을 넣으면 CLI와 정확히 같은 결과가 나오고,
CLI에 명령이 추가되면 여기에도 자동으로 생깁니다.

교착 상태 관리자는 왼쪽에 **숫자**(Allocation · Max · Need), 오른쪽에 **관계**(누가
누구를 기다리는가)를 나란히 놓았습니다. 은행원 알고리즘은 행렬을 보는 알고리즘이고
교착 탐지는 그래프를 보는 알고리즘이라, 같은 상황을 두 방식으로 동시에 볼 수 있습니다.
교착에 얽힌 노드만 Ember로 물들고 후광이 맥박치므로, 사이클을 표에서 찾아 헤맬 필요가
없습니다.

---

## 3분 안에 교착 상태를 눈으로 보기

**교착 상태 관리자**를 열고 은행원 알고리즘 토글을 **끕니다**(회피가 켜져 있으면 교착이
만들어지지 않습니다). 그다음 터미널에서:

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

회피가 요청을 막는 순간을 보고 싶다면 토글을 **켠 채로**:

```text
exec p1 30
exec p2 30
res_max 1 7 5 3
res_max 2 3 2 2
res_req 1 7 4 3        ← GRANTED
res_req 2 3 2 2        ← BLOCKED_UNSAFE : 자원은 남아 있지만 주면 불안전해진다
```

자원 표의 Available에는 아직 여유가 있는데도 요청이 막힙니다. 회피의 값어치가 가장 잘
드러나는 비교입니다.

---

## 창과 데스크탑

`Stage`를 여러 개 띄우는 대신 창을 데스크탑 위의 **노드**로 만들었습니다(Custom MDI).
그래야 창이 데스크탑 영역 안에 갇히고, Dock으로 빨려 들어가고, 반투명 재질 위에 겹쳐
보입니다.

| 동작 | 방법 |
|---|---|
| 이동 | 제목 표시줄 드래그. 잡은 지점이 유지되고, 화면 밖으로 끌면 고무줄처럼 저항하다 되돌아옵니다 |
| 크기 조절 | 가장자리 8방향 드래그 |
| 전체 화면 | 초록 버튼 또는 제목 표시줄 더블클릭 |
| 최소화 | 노랑 버튼 → Dock 아이콘으로 날아갑니다 |
| 단축키 | `Cmd/Ctrl+W` 닫기 · `Cmd/Ctrl+M` 최소화 · `Cmd/Ctrl+Shift+L` 테마 전환 |

신호등 버튼은 macOS의 배치와 동작을 따르되 색은 Forge 팔레트입니다 —
🔴 Forge Ember · 🟡 Molten Gold · 🟢 Electric Cyan.

Dock은 아이콘을 확대하지 않습니다. 가리킨 아이콘 **바로 위 가운데**에 앱 이름만 뜹니다.

---

## 라이트 / 다크 테마

메뉴바의 해·달 아이콘 또는 `Cmd/Ctrl + Shift + L`로 전환합니다. 아이콘은 현재 상태가
아니라 **누르면 갈 곳**을 보여 줍니다.

스타일시트를 **토큰**(색만)과 **구조**(치수)로 나누고, 전환할 때 토큰 파일을 통째로
갈아 끼웁니다. 루트에 클래스를 붙이는 흔한 방식은 콘텍스트 메뉴·툴팁·콤보 목록이 각자
자기 `Scene`을 갖기 때문에 따라오지 않습니다.

부팅 화면, 터미널, 로고 마크는 의도적으로 테마를 따르지 않습니다. 전원을 넣은 기계의
콘솔에 라이트 모드는 없고, 셸이 하얘지면 부팅 콘솔과 같은 물건이라는 감각이 끊기며,
브랜드는 테마가 바뀌어도 같은 물건이기 때문입니다.

배경화면은 이미지 파일이 아니라 `assets/forgeOS-logo.svg`의 패스를 그대로 옮긴
**벡터**입니다. JavaFX는 SVG 파일을 읽지 못하고, PNG로 구우면 배경화면 크기에서 뭉개지는
데다 테마에 따라 색을 바꿀 수 없습니다.

---

## 1.0의 내용

### 커널과 완전히 분리되었습니다

이 저장소에는 커널 코드도, 명령어 파서 코드도 한 줄이 없습니다. 둘 다 Maven 아티팩트
(`forgeframework:1.0`, `forgecli:1.0.1`)로 가져오며, **의존은 단방향**입니다 —
ForgeOS가 사라져도 커널은 아무 영향을 받지 않습니다.

### 커널은 문장을 만들지 않습니다

커널은 불변 record DTO만 돌려주고, ForgeCLI는 그것으로 표를 찍고 ForgeOS는 그것으로
도형을 그립니다. 같은 DTO, 다른 표현입니다. CLI가 포맷한 텍스트를 GUI가 다시 파싱하면
포맷이 바뀔 때마다 화면이 깨집니다.

### 백그라운드 → FX 스레드 경계는 한 곳뿐입니다

커널 로그는 부팅 스레드와 타이머 장치 스레드에서 올라오므로 `KernelService` 한 곳에서
전부 `Platform.runLater`로 넘깁니다. 반대로 시스템 콜은 인메모리 연산이라 FX 스레드에서
바로 부릅니다 — 백그라운드로 빼면 왕복이 생겨 UI가 한 박자 늦게 반응합니다.

### 색과 치수는 CSS에만 있습니다

자바 코드는 `styleClass`와 의사 클래스를 붙였다 뗄 뿐 `setStyle()`을 쓰지 않습니다.
디자인 변경에 컴파일이 필요해지면 안 됩니다.

### 애니메이션은 Transition이 아니라 스프링입니다

고정 시간 스크립트는 재생 도중 목표가 바뀌면 속도를 버리고 처음부터 다시 시작해서,
움직이는 창을 다시 잡아채는 순간 눈에 보이는 턱이 생깁니다. 스프링은 위치와 속도를
계속 들고 있으므로 언제 방향이 바뀌어도 궤적이 이어집니다.

### 앱 추가는 인터페이스 하나 + 목록 한 줄

`ForgeApp`을 구현하고 `AppCatalog.defaults()`에 넣으면 Dock에도 창 관리에도 자동으로
편입됩니다.

---

## 알려진 문제

**macOS 26 + JavaFX 21.0.5에서 앱을 닫을 때 JVM이 크래시할 수 있습니다.**

| 항목 | 내용 |
|---|---|
| 증상 | 창을 닫는 순간 `hs_err_pid*.log` 가 생깁니다 (`libglass.dylib` 의 `NSWindow _close` → `resignKeyWindow` 처리 중 SIGSEGV) |
| 원인 | JVM 셧다운과 macOS 창 해제의 경쟁. 자바 프레임이 잡히지 않는 것으로 보아 ForgeOS 코드가 아니라 JavaFX 네이티브 계층의 문제입니다 |
| 영향 | **종료 시점에만** 발생하므로 사용에는 지장이 없습니다 |
| 우회 | `gradle.properties` 의 `javafxVersion` 을 더 최신 버전으로 올려 보세요 |

---

## 구조

```text
forgeos
├── ForgeOsApp        창 하나 만들고 부팅 시퀀스를 돌린다. 그게 전부다
├── core/             커널과 UI 사이의 유일한 통로 (스레드 경계 · 갱신 펄스)
├── boot/             시네마틱 부팅 3단계
├── desktop/          바탕화면 · 메뉴바 · Dock
├── wm/               Custom MDI — 창 · 신호등 · 드래그 · 리사이즈 · 최소화
├── app/              내장 앱 4종
└── ui/               스프링 애니메이션 · 아이콘 · 테마 · 로고 마크
```

자세한 사용법은 [README](README.md)에 정리되어 있습니다.

---

## 관련 저장소

| 저장소 | 설명 |
|---|---|
| [forge-framework](https://github.com/Jongwoo0101/forge-framework) | 커널 엔진 · [API 문서](https://github.com/Jongwoo0101/forge-framework/blob/master/docs/api/README.md) |
| [forge-cli](https://github.com/Jongwoo0101/forge-cli) | 커널의 명령줄 클라이언트 — 터미널 앱이 이 저장소의 명령어 계층을 재사용합니다 |
| ForgeStudio | 운영체제 교육 · 시각화 플랫폼 (예정) |

---

<details>
<summary><b>English</b></summary>

## ForgeOS 1.0

**The ForgeFramework kernel now has a screen.**

ForgeOS is a JavaFX desktop environment that runs on top of the
[ForgeFramework](https://github.com/Jongwoo0101/forge-framework) kernel. Watch processes move
through the scheduler in a table, read memory pressure off donut gauges, and see **deadlocked
processes waiting on each other as a picture** rather than a list of PIDs.

Same kernel, same DTOs. ForgeCLI renders them as tables; ForgeOS renders them as shapes.

### Run it

A JavaFX application needs a platform-specific runtime on the module path, so unlike ForgeCLI
this project does not ship a single `java -jar` runnable jar. Run it from source.

```bash
git clone https://github.com/Jongwoo0101/forge-framework.git
cd forge-framework && ./gradlew publishToMavenLocal

git clone https://github.com/Jongwoo0101/forge-cli.git
cd ../forge-cli && ./gradlew publishToMavenLocal

git clone https://github.com/Jongwoo0101/forge-os.git
cd ../forge-os && ./scripts/run.sh
```

| Requirement | Version |
|---|---|
| JDK | **21** or newer |
| ForgeFramework | `1.0` |
| ForgeCLI | **`1.0.1` or newer** — that is the release that exported the command layer |
| JavaFX | `21.0.5`, fetched automatically by the Gradle plugin |

### The cinematic boot

1. **Terminal boot log** — kernel log lines typed out one character at a time. This is not
   staged text; it is the real log emitted while the kernel boots on a background thread.
2. **MP4 boot animation** — full screen.
3. **Desktop** — cross-fade to wallpaper, menu bar and Dock.

Stages are joined by **events, not time**. Fixed durations are guaranteed to drift — boot time
varies by machine, typing time varies with log length. Stage 1 → 2 fires when *the kernel has
booted **and** the typing queue has drained*; stage 2 → 3 fires on the *end-of-media event*.
`ESC`, `Space` or a click skips it.

### Built-in apps

| App | What it does | System calls |
|---|---|---|
| **Terminal** | All 33 ForgeCLI commands, inside a GUI window | `forgecli`'s `CommandRegistry` |
| **Activity Monitor** | Process table + memory/TLB donut gauges (1s refresh) | `PS` · `MEMINFO` · `SCHEDULER` · `EXEC` · `KILL` |
| **Finder** | Multi-column file system browser with preview | `LS` · `CAT` · `MKDIR` · `TOUCH` |
| **Deadlock Resolver** | Banker's algorithm toggle + wait-for graph | `RES_INFO` · `RES_REQ` · `RES_FREE` · `RES_MAX` · `BANKER` · `DETECT` · `RECOVER` |

The Terminal contains **no command parser of its own** — it uses the command layer ForgeCLI
`1.0.1` exported. The same string produces exactly the same result as in the CLI, and a
command added to the CLI appears here for free.

### What's in 1.0

- **Fully separated from the kernel.** Neither kernel nor parser code lives here; both arrive
  as Maven artifacts, and the dependency is one-way.
- **The kernel composes no sentences.** It returns immutable record DTOs; ForgeCLI draws
  tables from them and ForgeOS draws shapes.
- **One background → FX thread boundary.** Kernel logs arrive from the boot thread and the
  timer device thread and are all handed over with `Platform.runLater` in `KernelService`.
  System calls run on the FX thread directly — they are in-memory operations, and a
  background hop only makes the UI respond a beat late.
- **Colours and dimensions live only in CSS.** Java never calls `setStyle()`.
- **Animation is springs, not `Transition`.** A fixed-duration script discards its velocity
  when the target changes mid-flight, producing a visible hitch the moment you grab a moving
  window.
- **Light / dark theme** — toggled by swapping the token stylesheet wholesale, so context
  menus, tooltips and combo popups follow along. The boot screen, Terminal and logo mark
  deliberately ignore the theme.

### Known issue

**On macOS 26 with JavaFX 21.0.5 the JVM may crash while the app is closing.** An
`hs_err_pid*.log` appears, with SIGSEGV inside `libglass.dylib` during `NSWindow _close` →
`resignKeyWindow`. No Java frames are captured, so this is JavaFX's native layer racing the
JVM shutdown, not ForgeOS code. It happens **only at exit**, so it does not affect use. Try
raising `javafxVersion` in `gradle.properties`.

</details>
