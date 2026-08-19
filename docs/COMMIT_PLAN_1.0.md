# 커밋 플랜 — ForgeOS 1.0 (최초 릴리즈)

새 저장소이므로 커밋 하나로 끝내도 된다. 그래도 계층별로 끊는 쪽을 권한다 — 나중에
"이건 왜 이렇게 했지"를 찾을 때 `git log` 가 답을 갖고 있게 된다.

> **트레이드오프:** 계층별로 끊으면 중간 커밋은 컴파일되지 않는다(1번 커밋 시점에
> 소스가 없다). 모든 커밋이 빌드되길 원하면 아래 1~9를 하나로 합쳐
> `feat: ForgeOS 1.0 — ForgeFramework 커널의 JavaFX 데스크탑 환경` 으로 커밋한다.

---

## 0. 저장소 초기화

```bash
cd forge-os
git init
git branch -M main
git remote add origin https://github.com/Jongwoo0101/forge-os.git
```

`.gitignore` 를 **가장 먼저** 커밋해야 `build/` · `.gradle/` · `.idea/` ·
`hs_err_pid*.log` · `_to_delete/` 가 딸려 들어가지 않는다.

---

## 1. `chore(build): Gradle · JavaFX 프로젝트 구성`

```
.gitignore
LICENSE
settings.gradle.kts
gradle.properties
build.gradle.kts
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
scripts/build.sh
scripts/run.sh
src/main/java/module-info.java
```

- 커널과 명령어 계층을 소스가 아니라 **Maven 아티팩트**로 가져온다
  (`forgeframework:1.0`, `forgecli:1.0.1`)
- JavaFX 는 `org.openjfx.javafxplugin` 이 플랫폼별 런타임을 알아서 가져온다.
  `javafx.media` 가 반드시 있어야 부팅 애니메이션이 산다
- `-Xlint:all -Werror` — 커널·CLI 와 같은 경고 0건 기준선
- `mainClass` 는 `ForgeOsApp` 이 아니라 `Launcher` 다. Application 서브클래스를 직접
  main 으로 지정하면 JavaFX 런타임 누락 시 원인을 알 수 없는 한 줄만 남기고 죽는다

---

## 2. `feat(core): 커널-UI 경계와 스프링 모션 기반`

```
src/main/java/forgeos/core/KernelService.java
src/main/java/forgeos/ui/SpringValue.java
src/main/java/forgeos/ui/Motion.java
src/main/java/forgeos/ui/Styles.java
src/main/java/forgeos/ui/Glyphs.java
src/main/java/forgeos/ui/ToggleSwitch.java
```

백그라운드 → FX 스레드 경계는 `KernelService` **한 곳뿐**이다. 커널 로그는 부팅
스레드·타이머 장치 스레드에서 올라오므로 여기서 전부 `Platform.runLater` 로 넘긴다.
반대로 시스템 콜은 인메모리 연산이라 FX 스레드에서 바로 부른다 — 백그라운드로 빼면
왕복이 생겨 UI 가 한 박자 늦게 반응한다.

애니메이션은 `Transition` 이 아니라 스프링이다. 고정 시간 스크립트는 재생 도중 목표가
바뀌면 속도를 버리고 처음부터 다시 시작해서, 움직이는 창을 다시 잡아채는 순간 눈에
보이는 턱이 생긴다.

---

## 3. `feat(boot): 시네마틱 부팅 시퀀스`

```
src/main/java/forgeos/boot/BootConsole.java
src/main/java/forgeos/boot/BootSequence.java
src/main/java/forgeos/boot/BootVideo.java
src/main/java/forgeos/Launcher.java
src/main/java/forgeos/ForgeOsApp.java
```

단계 전환을 시간이 아니라 **사건**으로 잇는다. 1 → 2 는 *커널 부팅 완료 AND 타이핑 큐
소진*, 2 → 3 은 *MediaPlayer 재생 종료 이벤트*가 신호다.

`BootVideo` 가 리소스를 임시 파일로 복사하는 것은 JavaFX `Media` 가 `jar:` URL 을
재생하지 못하기 때문이다. IDE 에서는 되고 배포 jar 에서만 조용히 실패하는 종류의 버그다.

---

## 4. `feat(wm): Custom MDI 창 관리 시스템`

```
src/main/java/forgeos/wm/WindowManager.java
src/main/java/forgeos/wm/ForgeWindow.java
src/main/java/forgeos/wm/TrafficLights.java
```

`Stage` 를 여러 개 띄우지 않고 전부 노드로 만든 이유는 창이 데스크탑 영역 안에 갇혀야
하고, Dock 으로 빨려 들어가야 하고, 반투명 재질 위에 겹쳐 보여야 하기 때문이다.

드래그는 잡은 지점을 유지하고, 경계를 넘으면 고무줄처럼 저항하다 속도를 이어받아
되돌아온다.

---

## 5. `feat(desktop): 바탕화면 · 메뉴바 · Dock`

```
src/main/java/forgeos/desktop/DesktopPane.java
src/main/java/forgeos/desktop/Wallpaper.java
src/main/java/forgeos/desktop/MenuBarView.java
src/main/java/forgeos/desktop/DockView.java
src/main/java/forgeos/ui/ForgeMark.java
```

- 배경화면은 이미지가 아니라 `assets/forgeOS-logo.svg` 의 패스를 `SVGPath` 로 다시
  세운 벡터다(`ForgeMark`). JavaFX 는 SVG 파일을 읽지 못하고, PNG 로 구우면 배경화면
  크기에서 뭉개지며 테마에 따라 색을 못 바꾼다
- 장식 레이어는 `setMinSize(0,0)` + `setPrefSize(0,0)` 이다. 리사이즈 불가 노드(Circle,
  Group)를 그냥 넣으면 그 크기가 컨테이너의 최소 크기가 되고, 그 값이 Scene 루트까지
  전파되어 **메뉴바가 화면 위로, Dock 이 화면 아래로 밀려 잘린다**
- Dock 은 확대하지 않는다. 가리킨 아이콘 바로 위 가운데에 이름표만 띄운다

---

## 6. `feat(apps): 내장 앱 4종`

```
src/main/java/forgeos/app/ForgeApp.java
src/main/java/forgeos/app/AppContext.java
src/main/java/forgeos/app/AppInstance.java
src/main/java/forgeos/app/AppCatalog.java
src/main/java/forgeos/app/terminal/TerminalApp.java
src/main/java/forgeos/app/terminal/TerminalView.java
src/main/java/forgeos/app/monitor/ActivityMonitorApp.java
src/main/java/forgeos/app/monitor/ActivityMonitorView.java
src/main/java/forgeos/app/monitor/DonutChart.java
src/main/java/forgeos/app/finder/FinderApp.java
src/main/java/forgeos/app/finder/FinderView.java
src/main/java/forgeos/app/deadlock/DeadlockResolverApp.java
src/main/java/forgeos/app/deadlock/DeadlockResolverView.java
src/main/java/forgeos/app/deadlock/WaitForGraphView.java
```

커널의 Record DTO 를 문자열로 만들지 않고 그대로 `TableView` 와 도형에 바인딩한다.
CLI 가 텍스트로 포맷한 것을 GUI 가 다시 파싱하면 포맷이 바뀔 때마다 화면이 깨진다.

터미널은 명령어 파서를 한 줄도 새로 쓰지 않는다 — `forgecli` 의 `CommandRegistry` 를
그대로 쓴다.

`AppInstance(view, dispose)` 로 정리 작업을 반환 타입에 박아 둔 것은 구독 해지를
잊을 수 없게 하려는 것이다.

---

## 7. `style(theme): 디자인 토큰과 라이트/다크 테마`

```
src/main/java/forgeos/ui/ThemeManager.java
src/main/resources/forgeos/css/tokens-dark.css
src/main/resources/forgeos/css/tokens-light.css
src/main/resources/forgeos/css/theme.css
src/main/resources/forgeos/css/apps.css
```

스타일시트를 **토큰**(색만)과 **구조**(치수·레이아웃)로 나눴다. 테마 전환은 토큰 파일을
통째로 갈아 끼우는 것이다. 루트에 `.light` 클래스를 붙이는 방식은 콘텍스트 메뉴·툴팁·
콤보 목록이 자기 Scene 을 갖기 때문에 따라오지 않는다.

자바 코드에 `setStyle()` 은 한 줄도 없다.

---

## 8. `chore(assets): 부팅 애니메이션과 로고 리소스`

```
assets/forgeOS-banner.svg
assets/forgeOS-logo.svg
assets/forge-framework-logo.svg
assets/forge-cli-logo.svg
src/main/resources/assets/forgeOS-Booting-Animation.mp4
src/main/resources/assets/forgeOS-Booting-Animation2.mp4
src/main/resources/assets/forgeOS-banner.svg
src/main/resources/assets/forgeOS-logo.svg
```

루트 `assets/` 는 README·릴리즈용, `src/main/resources/assets/` 는 런타임용이다
(forge-cli 도 같은 관례). 런타임이 실제로 읽는 것은 MP4 뿐이다 — 로고는 `ForgeMark` 가
패스를 코드로 들고 있다.

**커밋 전에 판단할 것** — 아래 셋은 지금 그대로 커밋해도 문제는 없지만, 저장소와
배포 jar 를 가볍게 하려면 정리할 수 있다.

| 파일 | 상황 | 정리한다면 |
|---|---|---|
| `src/main/resources/assets/forgeOS-{banner,logo}.svg` | 루트 `assets/` 와 중복. 런타임이 읽지 않음 | 리소스 쪽만 제외 |
| `src/main/resources/assets/forgeOS-Booting-Animation.mp4` (2.7MB) | 미사용. 부팅은 `...Animation2.mp4` 를 쓴다 | 제외하거나 루트 `assets/` 로 이동 |
| `SKILL.MD` (루트, 22KB) | Apple 디자인 가이드 참고 문서. 형제 저장소에는 없는 파일 | 커밋하려면 `docs/` 로 옮기는 편이 자연스럽다 |

---

## 9. `docs: README · 커밋 플랜 · 릴리즈 노트`

```
README.md
README.en.md
docs/COMMIT_PLAN_1.0.md          (이 문서)
docs/RELEASE_NOTES_1.0.md
```

기존 `docs/COMMIT_PLAN_initial.md` 는 이 문서로 대체된다. 커밋된 적이 없으므로 지우고
이 문서만 남기면 된다.

소스 덤프(`dump_sources.py`, `forgeOS_source_dump.md`)를 형제 저장소처럼 함께 두려면
별도 커밋으로 끊는다 — `chore: 소스 덤프 스크립트 및 결과 추가`.

---

## 커밋 후 — 릴리즈

```bash
./gradlew clean build              # 경고 0건 확인
./gradlew run                      # 실제 구동 확인

git push -u origin main
git tag -a v1.0 -m "ForgeOS 1.0"
git push origin v1.0
```

GitHub → Releases → **Draft a new release**

| 항목 | 값 |
|---|---|
| Tag | `v1.0` (새로 만든다) |
| Target | `main` |
| Title | `ForgeOS 1.0` |
| 본문 | `docs/RELEASE_NOTES_1.0.md` 전체 붙여넣기 |
| 첨부 | 없음 — JavaFX 는 플랫폼별 런타임이 필요해 단일 실행 jar 를 만들지 않는다 |

`Set as the latest release` 체크. Pre-release 아님.

> **선행 조건:** ForgeCLI `1.0.1` 을 **먼저** 릴리즈해야 한다. ForgeOS 는 그 버전의
> 명령어 계층에 의존하므로, 순서가 뒤바뀌면 README 를 따라온 사람이 빌드에 실패한다.

## 검증 (이번 세션에서 확인한 것)

- `javac -Xlint:all -Werror` 모듈 컴파일 통과 (46 클래스, 경고 0건)
- CSS 4종 `javafx.css.CssParser` 파싱 통과
- 두 토큰 파일의 토큰 이름 집합 일치(41개), 구조 시트에 리터럴 색 없음
- `forgecli:1.0.1` 좌표로 컴파일 확인

GUI 실행 검증은 사용자 화면에서 수행했다(부팅 → 데스크탑 → Dock → 테마 전환).
