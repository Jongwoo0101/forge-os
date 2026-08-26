# ForgeOS 1.1.1 — 커밋 플랜

커밋 **열하나**입니다. 성능 개편 일곱, 개명 둘, 문서·버전 둘.

> **먼저 할 일:** forge-framework `1.1.1` 과 forge-cli `1.1.1` 을 `publishToMavenLocal`
> 로 설치해 두세요. 5번 커밋이 `EventLogger.setLevelEnabled` 를 부릅니다.
> 순서: **forge-framework → forge-cli → forge-os**.

순서에 강제되는 의존이 셋 있습니다.

| 이 커밋은 | 이것보다 뒤여야 |
|---|---|
| 2 (`ForgeWindow`) | 1 (`SpringValue.onFrame` 이 생김) |
| 6 (앱 뷰들) | 5 (`callCached` · `onRefresh(Node, …)` 가 생김) |
| 9 (README) | 8 (개명이 끝난 뒤라야 문서가 사실이 됨) |

---

## 1. 스프링을 타이머 하나로

```bash
git add src/main/java/forgeos/ui/SpringValue.java
git commit -m "perf(ui): 스프링 전부를 타이머 하나로 모으고 프레임 후처리를 추가한다"
```

`AnimationTimer` 상속을 걷어내고 정적 드라이버 하나가 활성 스프링 전부를 한 프레임에
적분합니다. 활성 스프링이 없으면 타이머가 멈추므로 정지 상태의 비용은 0입니다.

`onFrame(Runnable)` 이 이 커밋의 핵심입니다 — 여러 스프링이 같은 후처리를 공유해도
**프레임당 한 번만** 실행됩니다. 2번이 이것을 씁니다.

적분 서브스텝도 적응형으로 바꿉니다. 명시적 오일러의 안정 조건은 절대 시간이 아니라
`omega * dt` 에 걸리므로, 느슨한 스프링은 더 큰 걸음으로 걸어도 안전합니다.

> 이 커밋만으로도 빌드가 통과합니다. `ToggleSwitch` · `DonutChart` · `ForgeWindow` 가
> 쓰는 API(`tune` · `reset` · `setTarget` · `handoffVelocity` · `value`)는 그대로입니다.

## 2. 창: 위치와 크기를 가른다

```bash
git add src/main/java/forgeos/wm/ForgeWindow.java
git commit -m "perf(wm): 위치만 바뀐 프레임에는 레이아웃을 돌리지 않고, 이동 중에는 창을 캐시한다"
```

세 가지입니다.

1. 스프링 sink 는 값만 받아 적고, 화면 반영은 `onFrame` 후처리가 프레임당 한 번.
   **후처리는 반드시 필드에 담은 인스턴스 하나여야 합니다** — `this::commitBounds` 를
   넷에 각각 넘기면 서로 다른 객체가 되어 프레임당 네 번 돕니다.
2. `setPrefSize`/`resize` 는 크기가 **실제로 달라졌을 때만**. 창을 1px 미는 동안 창
   안쪽 전체를 다시 배치하던 것이 드래그 끊김의 직접 원인이었습니다.
3. 이동 중에만 비트맵 캐시. **리사이즈·확대·최소화에서는 켜지 않습니다** — 매 프레임
   캐시가 무효화되어 굽는 비용만 늘어납니다.

## 3. 여닫기 전환에도 캐시

```bash
git add src/main/java/forgeos/ui/Motion.java
git commit -m "perf(ui): 여닫기 전환 동안 노드를 캐시하고 장면 페이드를 400ms로 줄인다"
```

`materialize` · `dematerialize` 는 유리 표면과 큰 그림자를 스케일과 함께 움직입니다.
240ms 동안 프레임마다 그림자를 다시 굽던 자리이고, "앱이 늦게 뜬다"는 체감의 상당 부분이
실은 이 시간이었습니다.

`FADE_SCENE` 620 → 400ms 는 부팅에서 세 번 쓰이므로 7번과 짝입니다.

## 4. 그림자와 흐림

```bash
git add src/main/resources/forgeos/css/theme.css \
        src/main/java/forgeos/ui/ForgeMark.java \
        src/main/java/forgeos/desktop/Wallpaper.java
git commit -m "perf(ui): 그림자 반경을 줄이고 배경화면을 비트맵으로 캐시한다"
```

가우시안 흐림의 비용은 반경에 비례합니다. 열 자리를 다시 골랐습니다(활성 창 54 → 30,
Dock 42 → 24, 로고 발광 64 → 40 …). 깊이감은 반경보다 오프셋과 색이 만들므로 오프셋
비율은 유지했습니다.

배경화면의 마크와 동심원은 창 크기가 바뀔 때 말고는 변하지 않는데, 캐시가 없으면 위를
지나가는 창이 만든 더러운 영역마다 흐림이 다시 계산됩니다.

> **CSS 를 건드렸으므로 헤드리스 검증을 돌리세요.** 네 시트 파싱 + 다크/라이트 토큰
> 40개 이름 집합 일치 + 구조 시트가 참조하는 토큰이 전부 정의됐는지.

## 5. 커널 서비스: 같은 질문은 한 번만

```bash
git add src/main/java/forgeos/core/KernelService.java
git commit -m "perf(core): 읽기 전용 시스템 콜을 펄스 단위로 캐시하고 보이지 않는 화면은 건너뛴다"
```

**커널 1.1.1 이 mavenLocal 에 있어야 합니다** (`setLevelEnabled`).

- `callCached` — 읽기 전용 시스템 콜의 답을 한 펄스 동안만 붙듭니다. 상태를 바꾸는
  `call` 이 지나가면 즉시 버리고, 매 펄스 시작에도 버립니다. 읽기 전용이 아닌 종류는
  캐시하지 않고 그대로 실행하므로 호출자의 실수가 커널 상태를 어긋내지 않습니다.
- `onRefresh(Node, Runnable)` — 노드가 화면에 보이지 않으면 갱신을 건너뜁니다.
  `Node.isVisible()` 은 자기 플래그일 뿐이라 조상을 따라 올라가며 확인합니다.
- DEBUG 차단 — `logger.setLevelEnabled(LogLevel.DEBUG, false)` + 리스너 쪽 방어.
  DEBUG 를 보여 주는 화면이 하나도 없는데 시스템 콜마다 `Platform.runLater` 가 붙고
  있었습니다.
- 구독 목록을 `CopyOnWriteArrayList` 로 — 펄스마다 뜨던 복사본이 사라집니다.

## 6. 앱 뷰들: 달라진 것이 없으면 그리지 않는다

```bash
git add src/main/java/forgeos/app/monitor/ActivityMonitorView.java \
        src/main/java/forgeos/app/deadlock/DeadlockResolverView.java \
        src/main/java/forgeos/desktop/MenuBarView.java \
        src/main/java/forgeos/app/AppProcessTable.java \
        src/main/java/forgeos/app/terminal/TerminalView.java
git commit -m "perf(app): 스냅샷이 같으면 표를 다시 만들지 않고, 조회는 펄스 캐시를 쓴다"
```

- 넷 다 `callCached` 로 조회. 펄스당 시스템 콜 **7 → 4**.
- 활성 상태 보기 · 교착 상태 관리자는 `onRefresh(this, …)` 로 구독 — 최소화하면 멈춥니다.
- `setAll` 은 record 값 비교로 달라졌을 때만. `ObservableList.setAll` 은 내용이 같아도
  `TableView` 에게 "표가 통째로 바뀌었다"로 읽혀 셀이 전부 다시 만들어집니다.
- 상태 배지 셀이 `Label` 하나를 재사용, 준비 큐 칩은 문구가 달라졌을 때만 다시 생성,
  메모리 스냅샷이 그대로면 문자열 문단 전체를 건너뜀.
- 터미널 출력 상한 4000 → 1500 조각.

## 7. 부팅

```bash
git add src/main/java/forgeos/boot/BootVideo.java \
        src/main/java/forgeos/boot/BootSequence.java \
        src/main/java/forgeos/boot/BootConsole.java
git commit -m "perf(boot): 영상을 미리 준비하고 앞부분만 재생해 부팅을 6초대로 줄인다"
```

- `BootVideo.prepare` — 2.4MB 리소스를 임시 파일로 풀고 디코더를 세우는 일을 **부팅과
  동시에 백그라운드에서**. `1.1.0` 은 이것을 전환 순간에 FX 스레드에서 했습니다.
- `stopTime` 으로 10.0초 → 3.6초. 재생 종료 이벤트는 그 지점에서도 똑같이 옵니다.
- 타이핑 6.5 → 2.2ms/글자, 줄 사이 55 → 18ms, 커널 단계 지연 260 → 80ms,
  콘솔 최소 체류 1200 → 500ms.
- `BootConsole.pump` 가 글자마다가 아니라 **프레임마다** 텍스트를 반영합니다.

계산상 약 17초 → 약 6초. 건너뛰기(`ESC` · `Space` · 클릭)는 그대로입니다.

## 8. Firefox → ForgeWeb

```bash
git mv src/main/java/forgeos/app/browser/FirefoxApp.java \
       src/main/java/forgeos/app/browser/ForgeWebApp.java
git add src/main/java/forgeos/app/browser/ForgeWebApp.java \
        src/main/java/forgeos/app/browser/StartPage.java \
        src/main/java/forgeos/app/AppCatalog.java \
        src/main/java/forgeos/app/AppProcessTable.java \
        src/main/java/forgeos/ui/Glyphs.java \
        src/main/java/module-info.java \
        src/main/resources/forgeos/css/apps.css \
        build.gradle.kts
git commit -m "refactor(browser): 기본 브라우저의 이름을 ForgeWeb으로 바꾼다"
```

**`git mv` 를 쓰세요.** 그래야 이력이 이어집니다.

앱 id 가 `firefox` → `forgeweb` 입니다. 이것이 이 릴리즈의 **유일한 깨지는 변경**입니다 —
활성 상태 보기의 프로세스 표와 터미널에서 이름으로 프로세스를 찾던 습관이 영향을 받습니다.

CSS 클래스는 원래부터 `browser-*` 였으므로 구역 주석 한 줄만 바뀝니다.

## 9. README

```bash
git add README.md README.en.md
git commit -m "docs(readme): ForgeWeb 개명과 1.1.1 아티팩트 버전을 반영한다"
```

한/영 두 벌에서: 목차 항목, 앱 절 제목, 프로세스 표 예시(`firefox` → `forgeweb`),
시나리오 8, 그리고 아티팩트 좌표·jar 이름. 기능 이력(`1.1.0에서 …`)은 그대로 둡니다.

## 10. 버전

```bash
git add gradle.properties
git commit -m "chore(release): OS와 의존 아티팩트 버전을 1.1.1로 올린다"
```

`version` · `forgeFrameworkVersion` · `forgeCliVersion` 셋 다.

## 11. 문서

```bash
git add docs/RELEASE_NOTES_1.1.1.md docs/COMMIT_PLAN_1.1.1.md
git commit -m "docs: 1.1.1 릴리즈 노트와 커밋 플랜을 추가한다"
```

## 12. 태그와 배포

```bash
./gradlew clean build
./gradlew run          # 눈으로 확인 — 아래 목록

git tag -a v1.1.1 -m "ForgeOS 1.1.1 — 보이지 않는 일을 그만두고, 보이는 일은 한 번만"
git push origin main
git push origin v1.1.1
```

`v1.1.1` 태그가 릴리즈 워크플로를 돌려 러너 넷에서 설치본을 굽습니다.

---

## 확인 목록

컴파일과 CSS 는 자동으로 확인됩니다. **아래 다섯은 사람이 봐야 합니다.**

- [ ] 표가 든 창(활성 상태 보기)을 끌 때 끊기지 않는지
- [ ] 확대 · 최소화의 궤적이 `1.1.0` 과 같은 느낌인지 (캐시를 켜지 않는 구간입니다)
- [ ] 얕아진 그림자에서도 창이 떠 보이는지 — 라이트 테마에서 특히
- [ ] 부팅 영상이 3.6초에서 어색하게 끊기지 않는지
- [ ] 활성 상태 보기를 최소화했다 다시 열었을 때 값이 곧바로 맞는지
      (갱신이 멈춰 있었으므로, 다음 펄스가 아니라 **즉시** 맞아야 합니다)

그 밖에:

- [ ] `./gradlew build` — `-Xlint:all -Werror` 경고 0건
- [ ] Dock 의 브라우저 아이콘 이름표가 **ForgeWeb**
- [ ] 프로세스 표에 `forgeweb` 행이 뜨고, 강제 종료하면 창이 닫히는지
- [ ] 터미널에서 `shutdown` 후 데스크탑이 종료 오버레이를 띄우는지
