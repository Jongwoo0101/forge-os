# ForgeOS 1.1.1

**창을 끄는 동안 무엇을 하고 있었는지 찾아서 전부 그만뒀습니다**

새 앱도, 새 화면도 없습니다. 이 릴리즈의 내용은 한 문장으로 줄일 수 있습니다 —
**보이지 않는 일을 그만두고, 보이는 일은 한 번만 합니다.**

| 고친 것 | 무엇이 문제였나 |
|---|---|
| **창 드래그·리사이즈** | 창 하나가 타이머 넷을 돌리고, 1px 움직일 때마다 창 <b>안쪽 전체</b>를 다시 배치했습니다 |
| **큰 그림자와 흐림** | 반경 54px 짜리 창 그림자와 배경화면의 가우시안 흐림을 매 프레임 다시 구웠습니다 |
| **1초 갱신 펄스** | 세 화면이 같은 펄스에 `PS`를 각자 불렀고, 최소화한 창도 계속 표를 다시 그렸습니다 |
| **부팅 시퀀스** | 데스크탑까지 약 17초. 그중 10초가 부팅 영상이었습니다 |
| **부팅 2단계** | MP4 한 편을 재생하는 것이 전부였습니다. 리소스 2.4MB · `javafx.media` · 전환 순간 굳는 화면 |

| 바뀐 이름 | |
|---|---|
| **Firefox → ForgeWeb** | 엔진이 Gecko가 아니라 WebKit인데 이름만 빌리고 있었습니다. 앱 id도 `firefox` → `forgeweb` |

> **화면의 생김새와 조작 방법은 그대로입니다.** 창을 끄는 느낌, 스프링의 궤적, 테마,
> 단축키가 모두 같습니다. 그림자가 조금 얕아졌고, 부팅이 짧아졌고, 브라우저의 이름이
> 바뀌었습니다.

---

## 1. 창을 옮기는 것은 레이아웃이 아닙니다

`1.1.0`의 `ForgeWindow`는 x · y · 폭 · 높이에 스프링을 하나씩 두었고, **스프링 하나가
곧 `AnimationTimer` 하나**였습니다. 그리고 넷 중 무엇이 바뀌든 같은 메서드를 불렀습니다.

```java
private void applyBounds() {
    setLayoutX(x);
    setLayoutY(y);
    setPrefSize(w, h);   // ← 창 안쪽 전체의 레이아웃을 다시 돌리게 만드는 호출
    resize(w, h);
}
```

창을 **옆으로 1px 미는 동안에도** 창 안의 표·툴바·사이드바가 통째로 다시 배치됐다는
뜻입니다. 게다가 스프링이 넷이므로 확대·최소화처럼 넷이 동시에 움직이는 전환에서는
**한 프레임에 네 번** 그 일이 일어났습니다. 표가 든 창을 끌 때 눈에 띄게 끊기던
원인이 정확히 여기입니다.

### 고친 방법 셋

**(1) 타이머를 하나로 모았습니다.** `SpringValue`는 더 이상 `AnimationTimer`가 아닙니다.
정적 드라이버 하나가 활성 스프링 전부를 한 프레임에 몰아서 적분합니다. 창을 여섯 개
열면 타이머가 스물넷에서 하나가 됩니다. 활성 스프링이 없으면 타이머 자체가 멈추므로
**아무것도 움직이지 않을 때의 비용은 정확히 0**입니다.

**(2) 위치와 크기를 갈랐습니다.** 스프링의 sink는 이제 값만 받아 적고, 화면 반영은
프레임 끝에 <b>한 번만</b> 도는 후처리(`onFrame`)가 합니다. 그 후처리 안에서 크기는
**실제로 달라졌을 때만** 건드립니다.

```java
private void applyBounds() {
    setLayoutX(x);
    setLayoutY(y);
    if (w != appliedW || h != appliedH) {   // ← 위치만 바뀐 프레임에는 들어오지 않는다
        appliedW = w;
        appliedH = h;
        setPrefSize(w, h);
        resize(w, h);
    }
}
```

후처리는 스프링 넷이 **같은 인스턴스**를 공유해야 합니다. `this::commitBounds`를 넷에
각각 넘기면 서로 다른 객체가 되어 프레임당 네 번 실행되고, 없애려던 문제가 그대로
돌아옵니다 — 그래서 필드 하나에 담아 씁니다.

**(3) 움직이는 동안 창을 비트맵으로 굳힙니다.** 창에는 큰 가우시안 그림자가 걸려 있어서,
캐시가 없으면 1px 움직일 때마다 그 흐림을 다시 계산합니다. 이동은 이미 구운 그림을 옮기는
것으로 충분합니다.

반대로 **크기가 바뀌는 동안에는 켜지 않습니다.** 매 프레임 캐시가 무효화되므로 굽는
비용만 한 번 더 드는 꼴이 됩니다. 그래서 드래그와 "위치만 움직이는 스프링"에서만 켜고,
리사이즈와 확대·최소화에서는 끕니다.

> 서브스텝 적분도 손봤습니다. `1.1.0`은 스프링의 뻣뻣함과 무관하게 항상 240Hz로 쪼갰지만,
> 적분의 안정성과 정확도는 절대 시간이 아니라 `omega * dt`에 걸립니다. 이제 고유 진동수에
> 맞춰 쪼개므로, 창이 쓰는 표준 스프링(`response = 0.4`)에서는 60fps 한 프레임의 반복이
> **네 번에서 두 번으로** 줍니다. 이 크기에서 주기 오차는 걸음당 0.1% 미만이라 궤적은
> 눈으로 구별되지 않습니다.

## 2. 그림자는 얕아도 그림자입니다

가우시안 흐림의 비용은 반경에 따라 커집니다. 값을 다시 골랐습니다.

| 자리 | 1.1.0 | 1.1.1 |
|---|---|---|
| 활성 창 | 54px | 30px |
| 비활성 창 | 26px | 16px |
| Dock | 42px | 24px |
| 메뉴바 | 18px | 12px |
| 팝오버·콘텍스트 메뉴 | 26 · 24 · 18 · 16px | 18 · 16 · 12 · 11px |
| 로고 불꽃의 발광 | 64px | 40px (대신 확산 0.35 → 0.45) |
| 배경화면 발광 원반 | 54 · 42px | 34 · 26px |

깊이감은 반경보다 **오프셋과 색**이 만듭니다. 반경을 줄이면서 오프셋 비율을 유지했기
때문에 "떠 있다"는 느낌은 그대로입니다.

배경화면은 여기에 더해 **비트맵으로 캐시**합니다. 마크와 동심원은 창 크기가 바뀔 때
말고는 변하지 않는데, 캐시가 없으면 위를 지나가는 창이 만든 더러운 영역마다 흐림이
다시 계산됩니다. 창을 끌 때 배경이 조용히 CPU를 먹고 있던 자리입니다.

## 3. 같은 질문을 한 펄스에 세 번 하지 않습니다

`1.1.0`에서 메뉴바 · 앱 프로세스 표 · 활성 상태 보기는 **각자** `PS`를 불렀습니다.
1초에 한 번씩, 셋이 따로.

| 펄스 한 번 | 1.1.0 | 1.1.1 |
|---|---|---|
| 시스템 콜 | `PS`×3 · `MEMINFO`×2 · `UPTIME` · `SCHEDULER` = **7** | `PS` · `MEMINFO` · `UPTIME` · `SCHEDULER` = **4** |
| 커널 DEBUG 로그 | 7줄 (전부 버려짐) | **0줄** |
| `Platform.runLater` | 7회 | **0회** |

`KernelService.callCached`가 **읽기 전용** 시스템 콜의 답을 한 펄스 동안만 붙들어 둡니다.
상태를 바꾸는 `call`이 한 번이라도 지나가면 즉시 버리고, 매 펄스 시작에도 버립니다.
읽기 전용이 아닌 종류를 넘기면 캐시하지 않고 그대로 실행하므로, 호출자가 실수해도
커널 상태가 어긋나지 않습니다.

덤이 하나 있습니다. 셋이 같은 순간의 답을 보게 되어 **메뉴바가 "프로세스 5"라고 하는데
표에는 여섯 줄이 있는** 어긋남이 사라졌습니다.

### 보이지 않는 창은 갱신하지 않습니다

`kernelService.onRefresh(this, this::refresh)` — 노드를 함께 넘기면, 그 노드가 화면에
보이지 않는 동안 갱신을 통째로 건너뜁니다. `Node.isVisible()`은 자기 자신의 플래그일
뿐이라 조상을 따라 올라가며 확인합니다. 창을 최소화하면 숨겨지는 것은 앱 화면이 아니라
그것을 담은 창이기 때문입니다.

`1.1.0`에서는 Dock으로 최소화한 활성 상태 보기가 매초 표를 다시 채우고 게이지 스프링을
돌렸습니다. 사용자에게는 보이지 않는 일이었습니다.

### 달라진 것이 없으면 표를 건드리지 않습니다

`ObservableList.setAll`은 내용이 완전히 같아도 `TableView`에게 "표가 통째로 바뀌었다"로
읽힙니다. 셀이 전부 다시 만들어지고 다시 배치되며, 선택이 풀렸다가 다시 걸립니다.
이제 record의 값 비교로 먼저 확인합니다.

```java
if (processes.equals(snapshot)) {
    return;
}
```

같은 이유로 상태 배지 셀이 `Label`을 다시 만들지 않고 하나를 다시 쓰고, 준비 큐 칩은
문구가 달라졌을 때만 다시 만들고, 메모리 스냅샷이 그대로면 문자열 예닐곱 개를 만드는
문단 전체를 건너뜁니다.

## 4. 부팅 2단계를 다시 만들었습니다

`1.1.0`의 2단계는 10초짜리 MP4였습니다. 영상은 세 가지 값을 치렀습니다.

- 리소스 **2.4MB**
- `javafx.media` 모듈과 그에 딸린 **네이티브 라이브러리**
- 전환 순간 디코더를 세우느라 **화면이 굳는 시간** (그것도 FX 스레드에서)

그 대가로 얻은 것은 매번 똑같이 재생되는 10초였습니다. 새 2단계
(`BootSplash`)에는 **도형밖에 없습니다.**

### 무엇을 보여 주는가

위에서 아래로 **후광 → 로고 → 워드마크 → 스피너 → 상태 문구**입니다.

| 요소 | 움직임 | 왜 그렇게 |
|---|---|---|
| **후광** | 점선 원 두 겹(r=268 · 188)이 초당 11° · 17°로 시계 방향 | 배경화면의 동심원과 같은 자리·같은 성격입니다. 부팅이 끝나고 배경이 드러날 때 두 화면이 "같은 세계"로 이어집니다 |
| **로고** | `ForgeMark` 132px, 0.55초 동안 커지며 밝아짐 | 배경화면이 쓰는 것과 **같은 벡터**입니다. 어느 해상도에서도 깨지지 않습니다 |
| **스피너** | 초당 210° 시계 방향, 호의 길이가 34° ↔ 292°를 1.5초 주기로 오감 | 고리가 일정한 길이로만 돌면 그것은 시계입니다. 길이가 숨을 쉬어야 "진행 중"으로 읽힙니다 |
| **안쪽 호** | 초당 335°, 청록 실선 66° | 하나만 돌면 시계, 둘이 다른 속도로 돌면 기계입니다 |
| **불티** | 호의 머리에 붙어 돎 | 지금 어디까지 왔는지 알려 주는 유일한 점입니다 |
| **상태 문구** | 0.3초 → 1.0초 → 1.6초에 교체 | 커널은 1단계에서 이미 다 떴습니다. 그러니 여기서 "커널 초기화 중"이라고 쓰면 거짓말입니다 — 이 구간에 실제로 일어나는 일만 적습니다 |

**끝맺음이 중요합니다.** 마지막 0.5초에 호가 **완전한 원으로 닫히면서** 전체가 살짝
커집니다. 로딩이 흐지부지 사라지는 대신 "채워졌다"로 끝나야, 다음에 오는 데스크탑이
그 결과로 읽힙니다.

**전환도 겹칩니다.** `1.1.0`은 콘솔이 완전히 사라진 *뒤에* 다음 장면을 시작했습니다.
두 시간이 더해지고, 그 0.4초 동안 화면에는 검은색밖에 없었습니다. 둘 다 검은 배경 위에
있으므로 겹쳐 두면 **글자가 흐려지는 자리에서 로고가 떠오르는 한 장면**이 됩니다.

### 1단계도 시간 예산을 다시 짰습니다

| 자리 | 1.1.0 | 1.1.1 | 왜 |
|---|---|---|---|
| 글자당 타이핑 간격 | 6.5ms | 2.2ms | 로그가 800자 안팎이라 이 값 하나가 부팅의 절반을 정합니다 |
| 줄 사이 숨 | 55ms | 18ms | |
| 커널 단계 지연 | 260ms × 5 | 80ms × 5 | 이 1초 동안 화면에서는 <b>아무 일도 일어나지 않습니다</b> — 타이핑은 이미 쌓인 줄을 처리하느라 바쁩니다 |
| 콘솔 최소 체류 | 1200ms | 500ms | 깜빡임을 막는 하한이지 연출 시간이 아닙니다 |
| 2단계 | 10.0초 (MP4) | 2.4초 (스플래시) | |
| 장면 페이드 | 620ms | 400ms | |

**타이핑은 프레임 단위로 반영합니다.** 2.2ms 간격이면 60fps에서 한 프레임에 일고여덟
글자가 찍히는데, 화면에 그려지는 것은 마지막 상태 하나뿐입니다. 그런데 `1.1.0`은 글자마다
`setText`를 불러 그 횟수만큼 라벨 크기 계산과 부모 `VBox`의 배치 무효화를 일으켰습니다.
아무도 보지 못하는 중간 상태에 값을 온전히 치르고 있었던 셈입니다.

> 계산상 데스크탑까지 **약 17초 → 약 4.3초**입니다. 기계마다 다르므로 실제 값은 직접
> 확인해 주세요. `ESC` · `Space` · 클릭으로 건너뛰는 길은 그대로 있습니다.

### 남은 것

`assets/` 의 MP4 두 개는 **지우지 않고 그대로 두었습니다.** 쓰는 곳은 없습니다 —
되살리고 싶을 때를 위해 남겨 둔 것이고, 배포본에서 5MB를 되찾고 싶으면 두 파일을
지우면 됩니다. 영상을 되살리려면 `module-info.java` 의 `requires javafx.media` 와
`build.gradle.kts` 의 `javafx.modules` 양쪽을 되돌려야 합니다.

## 5. Firefox → ForgeWeb

`1.1.0`에서 이 앱의 이름은 `Firefox`였습니다. 그런데 이 앱은 Mozilla의 Gecko를 품고
있지 않습니다 — JavaFX가 들고 있는 렌더링 엔진은 `javafx.web`의 **WebKit** 하나뿐이고,
자바 프로세스 안에서 Gecko를 띄울 방법은 없습니다.

남의 이름을 빌린 채로 "사실 그 엔진이 아닙니다"를 각주로 다는 것보다 자기 이름을 갖는
편이 정직하고, ForgeOS의 다른 앱들이 전부 제 이름인 것과도 맞습니다.

| | 1.1.0 | 1.1.1 |
|---|---|---|
| 이름 | Firefox | **ForgeWeb** |
| 앱 id (프로세스 표에 뜨는 이름) | `firefox` | `forgeweb` |
| 클래스 | `FirefoxApp` | `ForgeWebApp` |
| 시작 페이지 워드마크 | Fire**fox** | Forge**Web** |

시작 페이지의 타일 하나(mozilla.org)는 W3C로 바꿨습니다. 나머지 — 탭, 주소창 규칙,
오프라인에서도 뜨는 내장 시작 페이지, WebKit 엔진 — 는 전부 그대로입니다.

---

## 그 밖에

- **터미널 출력 상한 4000 → 1500 조각.** `TextFlow`는 자식이 늘어날수록 append 한 번의
  비용이 커집니다. 1500이면 명령 400개어치의 기록이 남습니다.
- **갱신 구독 목록을 `CopyOnWriteArrayList`로.** `1.1.0`은 콜백 안에서 구독을 해지하는
  경우 때문에 펄스마다 `List.copyOf`로 복사본을 떴습니다. 읽기는 초당 한 번, 쓰기는 창을
  여닫을 때뿐인 전형적인 COW 대상입니다.
- **창 여닫기 애니메이션에도 캐시를 겁니다.** 240ms 동안 유리 표면과 그림자를 프레임마다
  다시 굽던 자리입니다 — "앱이 늦게 뜬다"는 체감의 상당 부분이 실은 이 시간이었습니다.

## 호환성

- 화면 구성, 조작 방법, 단축키, 테마 토큰 40개가 **전부 그대로**입니다.
- **깨지는 변경 하나:** 브라우저 앱의 id가 `firefox` → `forgeweb`입니다. 터미널에서
  `kill` 할 프로세스를 이름으로 찾던 습관이 있다면 새 이름을 쓰세요.
- **`javafx.media` 의존이 사라졌습니다.** 배포 이미지에서 미디어 네이티브 라이브러리가
  빠집니다. 이 모듈을 쓰던 곳은 부팅 영상 하나뿐이었습니다.
- ForgeFramework `1.1.1` · ForgeCLI `1.1.1`이 필요합니다. 세 저장소의 번호는 계속 맞춰
  둡니다. JavaFX 21.0.5, JDK 21 — 변경 없음.

## 검증

- `javac -Xlint:all -Werror` 경고 0건 (커널 · CLI · OS 셋 다).
- CSS 헤드리스 검증 통과 — 네 시트 파싱, 다크/라이트 **토큰 40개 이름 집합 일치**,
  구조 시트가 참조하는 토큰 57개 전부 정의됨.
- 커널 검증 스위트 **313개 항목 전부 통과**.

> **사람이 봐야 하는 것:** GUI 실행은 컨테이너에서 불가능합니다. 창을 끌 때의 부드러움,
> 확대·최소화의 궤적, 얕아진 그림자가 어색하지 않은지, 스피너가 **시계 방향**으로
> 도는지(각도 부호를 한 번 틀리면 반대로 돕니다), 콘솔에서 스플래시로 넘어가는 순간이
> 겹쳐 보이는지, 최소화한 활성 상태 보기를 다시 열었을 때 값이 곧바로 맞는지 —
> 이 여섯은 직접 확인해 주세요.

## 파일

| 파일 | 변경 |
|---|---|
| `ui/SpringValue.java` | `AnimationTimer` 상속을 걷어내고 정적 드라이버 하나로 · `onFrame` 후처리 · 적응형 서브스텝 |
| `wm/ForgeWindow.java` | 위치/크기 분리 · 프레임당 한 번 커밋 · 이동 중 비트맵 캐시 |
| `ui/Motion.java` | 여닫기 전환에 캐시 · `FADE_SCENE` 620 → 400ms |
| `core/KernelService.java` | `callCached` · `onRefresh(Node, …)` · DEBUG 차단 · COW 구독 목록 |
| `app/monitor/ActivityMonitorView.java` | 스코프 구독 · 스냅샷 비교 후 갱신 · 배지 셀 재사용 · 큐 칩 diff |
| `app/deadlock/DeadlockResolverView.java` | 스코프 구독 · 캐시된 조회 · 표 diff |
| `desktop/MenuBarView.java` · `app/AppProcessTable.java` | 캐시된 조회 |
| `desktop/Wallpaper.java` · `ui/ForgeMark.java` | 비트맵 캐시 · 흐림 반경 축소 |
| `boot/BootSplash.java` | **신규** (`BootVideo` 를 대신함) — 로고 · 스피너 · 후광, 타이머 하나 |
| `boot/BootSequence.java` · `boot/BootConsole.java` | 2단계 교체 · 콘솔 페이드와 스플래시 등장을 겹침 · 시간 예산 재조정 · 프레임 단위 타이핑 반영 |
| `module-info.java` · `build.gradle.kts` | `javafx.media` 제거 |
| `app/browser/ForgeWebApp.java` | `FirefoxApp` 에서 이름 변경 (id `forgeweb`) |
| `app/browser/StartPage.java` · `app/AppCatalog.java` · `ui/Glyphs.java` · `module-info.java` | ForgeWeb 개명 반영 |
| `app/terminal/TerminalView.java` | 출력 상한 4000 → 1500 |
| `resources/forgeos/css/theme.css` · `apps.css` | 그림자 반경 10곳 · 구역 이름 |
| `gradle.properties` · `build.gradle.kts` | `1.1.1` · 주석의 앱 이름 |

## 관련 저장소

| 저장소 | 버전 | 이 릴리즈에서 |
|---|---|---|
| [forge-framework](https://github.com/Jongwoo0101/forge-framework) | `1.1.1` | 로그 수준 스위치 · 뜨거운 경로 아홉 곳 정리 |
| [forge-cli](https://github.com/Jongwoo0101/forge-cli) | `1.1.1` | DEBUG 차단 · 명령어 조회 경로 정리 |

<details>
<summary><b>English summary</b></summary>

### No new screens. Just less work.

**Moving a window is not a layout.** In `1.1.0` each window ran four springs and each spring
*was* an `AnimationTimer`; any of the four changing called `setPrefSize` + `resize`, which
re-lays out everything inside the window. Nudging a window one pixel sideways re-laid out its
tables and toolbars — four times per frame during a zoom, since four springs moved at once.
That is exactly why dragging a window containing a table stuttered.

Three fixes. (1) `SpringValue` is no longer an `AnimationTimer`; one static driver integrates
every active spring per frame, and stops entirely when nothing is moving — six open windows
go from twenty-four timers to one. (2) Sinks now only record values; a single per-frame
commit applies them, and size is only pushed when it actually changed. The four springs must
share *one* `Runnable` instance for that, or the commit runs four times again. (3) While a
window is translating it is cached as a bitmap, so its large gaussian shadow is not re-baked
every frame — and deliberately *not* cached while resizing, where the cache would be
invalidated every frame anyway. Sub-stepping is now adaptive: accuracy and stability are
bounded by `omega * dt`, not by absolute time, so the step is sized from the spring's own
frequency — for the standard window spring (`response = 0.4`) that halves the per-frame
integration from four iterations to two, with a period error under 0.1% per step.

**Shadows got shallower.** Active window 54 → 30px, inactive 26 → 16, Dock 42 → 24, menu bar
18 → 12, the logo flame's glow 64 → 40 (with diffusion raised to compensate), wallpaper glow
discs 54/42 → 34/26. Depth comes from offset and colour more than from radius, and offsets
were kept proportional. The wallpaper mark and rings are also cached as a bitmap: they only
change when the window resizes, but without a cache every dirty region a dragged window
created re-ran the blur.

**The refresh pulse asks each question once.** The menu bar, the app-process table and the
Activity Monitor each called `PS` separately, once a second. `KernelService.callCached` now
holds read-only system-call results for the duration of one pulse, dropping them the moment
any state-changing `call` goes through and again at the start of each pulse; non-read-only
types are passed straight through, so a caller's mistake cannot desynchronise the kernel.
Per pulse: 7 system calls → 4, and (with the kernel's `DEBUG` level off) 7 discarded log
entries and 7 `Platform.runLater` hops → zero. A side benefit: all three views now read the
same instant, so the menu bar can no longer say "5 processes" while the table shows six.

Subscribing with `onRefresh(node, …)` skips the refresh entirely while that node is not
visible — walking ancestors, because minimising hides the *window*, not the app view. And
`setAll` is only called when a record-by-record comparison shows something changed:
`ObservableList.setAll` reads to `TableView` as "the whole table changed" even when it did
not. Badge cells reuse one `Label`, queue chips are rebuilt only when their text differs, and
an unchanged memory snapshot skips a paragraph that builds half a dozen strings.

**Boot stage 2 was rebuilt, and boot went from ~17s to ~4.3s** (estimated; confirm on your
machine). The 10-second MP4 is gone. It cost a 2.4MB resource, the `javafx.media` module and
its native libraries, and a hitch at the transition while a decoder was built *on the FX
thread* — in exchange for the same ten seconds every time. `BootSplash` is nothing but
shapes: the same vector logo the wallpaper draws, two dotted halo rings turning clockwise at
11°/s and 17°/s, and a spinner beneath the logo running clockwise at 210°/s whose arc length
breathes between 34° and 292° on a 1.5s cycle, with a faster thin cyan arc inside it and an
ember riding the arc's head. A ring turning at a constant length is a clock; a breathing one
is a loader. In the final 0.5s the arc closes into a full circle while everything scales up
slightly — a loader that fizzles out reads as an interruption, one that completes reads as a
result. The console fade and the splash entrance now *overlap* rather than queue, so there is
no interval of plain black. Stage 1 was retimed too: per-character typing 6.5 → 2.2ms, line
pause 55 → 18ms, kernel stage delay 260 → 80ms ×5 (that second bought nothing — the console
was busy draining queued lines), minimum dwell 1200 → 500ms, scene fades 620 → 400ms, and
typed text is flushed once per frame rather than once per character. `javafx.media` is no
longer required, so the media natives leave the packaged image; the two MP4s stay in
`assets/` unused, in case you want the video back. Skip with `ESC` / `Space` / click still
works.

**Firefox is now ForgeWeb.** The app never embedded Gecko — JavaFX ships WebKit and only
WebKit — and borrowing a name you then have to footnote is worse than having your own. The
app id changed from `firefox` to `forgeweb`, so the process table shows the new name; this is
the release's one breaking change. Tabs, address-bar rules, the offline start page and the
engine are unchanged.

### Compatibility and verification

Layout, interactions, shortcuts and all 40 theme tokens are unchanged. Requires
ForgeFramework `1.1.1` and ForgeCLI `1.1.1`; JavaFX 21.0.5 and JDK 21 unchanged. Zero
warnings under `-Xlint:all -Werror` across all three modules; headless CSS check passes (four
sheets parsed, dark/light token name sets identical at 40, all 57 referenced tokens defined);
the kernel suite passes all 313 checks. GUI behaviour cannot be verified in a container —
please eyeball drag smoothness, zoom/minimise trajectories, the shallower shadows, whether
the video cuts awkwardly at 3.6s, and that a re-opened minimised Activity Monitor shows
correct values immediately.

</details>
