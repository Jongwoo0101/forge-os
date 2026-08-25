# ForgeOS 1.1.0

**커널이 넓어진 만큼 화면도 넓어졌고, 데스크탑에 없던 두 앱이 생겼습니다**

ForgeFramework `1.1.0`은 `fork`(COW) · 스케줄러 4종 추가 · 스왑/페이지 폴트 ·
`disk.img` 영속화를 들여왔습니다. `1.0`의 ForgeOS는 그 커널 위에서 **컴파일은
되지만 아무것도 보여 주지 못하는** 상태였습니다 — 새로 생긴 필드도, 새로 생긴
시스템 콜 7종도 화면에 자리가 없었기 때문입니다. 이번 릴리즈는 그 자리를 만드는
일과, 데스크탑이라면 당연히 있어야 할 두 앱을 붙이는 일입니다.

| 넣은 것 | 한 줄 요약 |
|---|---|
| **메모장** | 커널 파일 시스템 위에서 도는 텍스트 편집기. 대화상자 없이, 저장하지 않은 변경은 초안으로 |
| **Firefox** | `javafx.web` WebView 기반 기본 브라우저. 탭 · 주소창 · 오프라인에서도 뜨는 내장 시작 페이지 |
| **활성 상태 보기 개편** | 탭 둘(프로세스 · 메모리). PPID · 우선순위 · 큐 등급 · fork · 스케줄러 6종 · 프레임/페이지 테이블 · 스왑 게이지 · 교체 정책 |
| **Finder · 메뉴바** | `disk.img` 기록 버튼, 스왑/페이지 폴트 칩 |
| **앱 = 커널 프로세스** | 창을 열면 프로세스가 생기고 메모리가 할당된다. 죽이면 창이 닫힌다 |
| **배포본** | jlink + jpackage. macOS `.dmg` · Windows `.msi` · Linux `.deb` · 포터블 zip 을 GitHub Actions 가 굽는다 |

> **터미널은 한 줄도 고치지 않았습니다.** 늘어난 명령어 9종이 그대로 들어왔습니다.
> [터미널 절](#5-터미널--고친-것이-없다)에 그 이유를 적어 두었습니다.

---

## 1. 메모장 — 커널 디스크 위의 편집기

```text
Dock → 메모장 → 이름을 치고 [만들기] → 글을 쓰고 [저장] → [디스크에 기록]
```

맥의 실제 폴더에 `.txt`를 떨구는 편이 구현은 훨씬 쉽습니다. 그렇게 하지 않은 이유는
이 앱이 하는 일이 "글을 적는 것"이 아니라 **커널의 파일 시스템을 손으로 만져 보는
것**이기 때문입니다. 메모장에서 저장한 파일은

- Finder의 컬럼에 곧바로 나타나고,
- 터미널의 `cat`으로 읽히며,
- **디스크에 기록**을 누르면 `disk.img`에 내려앉아 재부팅을 견딥니다.

`1.1.0` 커널이 들여온 영속화를 명령어 없이 눈으로 확인할 수 있는 자리가 여기입니다.

| 동작 | 시스템 콜 |
|---|---|
| 파일 선택 | `CAT` |
| 폴더 이동 · 상위 폴더 | `LS` |
| 만들기 | `TOUCH` |
| 저장 (`Cmd/Ctrl + S`) | `WRITE` |
| 되돌리기 | `CAT` |
| 디스크에 기록 | `SYNC` |

### 대화상자를 하나도 쓰지 않았습니다

"이름을 입력하세요", "저장하지 않고 닫을까요?" 같은 질문을 `Dialog`로 띄우면 그때마다
**진짜 네이티브 창**이 하나 열립니다. ForgeOS는 "데스크탑 안의 모든 것은 노드"라는
원칙 위에 서 있고, 실제로 네이티브 창 해제는 `1.0` 릴리즈 노트의 "알려진 문제"에서
종료 시 크래시의 원인으로 지목한 자리이기도 합니다.

그래서 새 파일 이름은 툴바의 입력칸으로 받습니다. 저장하지 않은 변경은 **질문 대신
초안으로 들고 있습니다** — 경로별로 편집 중인 내용(`drafts`)과 마지막으로 디스크와
일치했던 내용(`saved`)을 따로 두고, 둘이 다르면 목록에 점(•)이 붙습니다. 파일을 옮겨
다녀도 초안은 그대로 남으므로 **물을 이유 자체가 없어집니다.** 질문을 잘 만드는 것보다
질문이 필요 없게 만드는 쪽이 낫습니다.

---

## 2. Firefox — 엔진에 관한 정직한 설명

이 앱은 Mozilla의 **Gecko를 품고 있지 않습니다.** JavaFX가 들고 있는 렌더링 엔진은
`javafx.web`의 **WebKit** 하나뿐이고, 자바 프로세스 안에서 Gecko를 띄울 방법은
없습니다. 그래서 이 앱은 "ForgeOS 창 안에서 도는 브라우저"이며, 이름과 자리
(기본 브라우저)를 Firefox에게 준 것입니다.

호스트에 설치된 진짜 Firefox를 `Desktop.browse()`로 실행하는 길도 있었습니다.
구현은 다섯 줄이면 끝납니다. 그러나 그러면 창이 ForgeOS 바깥으로 튀어나가고,
**가상 데스크탑이라는 전제가 그 순간 깨집니다.** 우리 창 관리자가 모르는 창이
화면 위에 뜨는 것은 데스크탑 환경의 실패입니다.

| 기능 | 동작 |
|---|---|
| 주소창 | `://`가 있으면 그대로, 점이 박힌 한 덩어리면 `https://`를 붙이고, 나머지는 검색어 |
| 뒤로 · 앞으로 | `WebHistory`의 현재 위치를 보고 자동으로 비활성화 |
| 새로고침 · 정지 | 같은 버튼. 로딩 중이면 정지가 됩니다 |
| 홈 | 내장 시작 페이지 (`forge://start`) |
| 탭 | `+`로 추가. `target="_blank"`와 `window.open`도 새 창이 아니라 **새 탭**으로 받습니다 |

### 시작 페이지가 내장 문서인 이유

홈을 실제 사이트로 두면 네트워크가 없는 자리(발표장 · 기내 · 사내망)에서 앱을 열자마자
오류 화면이 뜹니다. 브라우저를 처음 켠 사람이 가장 먼저 보는 화면이 오류인 것은
**앱이 고장 난 것과 구별되지 않습니다.** 내장 문서는 오프라인에서도 항상 뜨고, 링크를
누르는 순간에야 네트워크가 필요해집니다.

### 도구 모음은 "선택된 탭"만 읽습니다

탭마다 프로퍼티를 바인딩하면 탭을 옮길 때마다 바인딩을 끊고 다시 걸어야 합니다.
대신 각 탭이 자기 상태가 바뀔 때마다 도구 모음에 알리고, 도구 모음은 **지금 선택된
탭만** 읽습니다. 바인딩이 없으니 끊을 것도 없습니다.

> `javafx.web`은 모듈 중 유일하게 무게가 다릅니다. WebKit 네이티브 라이브러리가
> 통째로 딸려 와서 배포 크기가 100MB 가까이 늘어납니다. 그럼에도 넣은 이유는,
> 브라우저 없이 "데스크탑 환경"이라고 부르기 어렵기 때문입니다.

---

## 3. 활성 상태 보기 — 질문이 둘로 갈라졌습니다

`1.0`에서 이 앱이 답하던 질문은 하나였습니다. "누가 CPU를 기다리는가."
커널이 스케줄러 6종 · 스왑 · `fork`를 갖게 되면서 질문이 둘이 되었습니다.

| 탭 | 답하는 질문 |
|---|---|
| **프로세스** | 왜 저 프로세스가 먼저 뽑혔는가 (스케줄러 · 우선순위 · 큐 등급) |
| **메모리** | 메모리가 어디로 갔는가 (프레임 · 스왑 · COW 공유) |

게이지는 두 질문에 공통이라 사이드바에 남았습니다.

### 프로세스 탭

표에 세 열이 붙었습니다 — **PPID**(`fork`가 생기며 의미를 얻었습니다), **우선순위**,
**큐 등급**(MLFQ에서만 뜻이 있고, 나머지 스케줄러에서는 가로줄이 뜹니다).

툴바에는 우선순위 입력칸과 **복제** 버튼이, 행 우클릭에는 복제 · 우선순위 올리기/내리기 ·
강제 종료가 있습니다. 복제를 누르면 상태줄에 이렇게 뜹니다.

```text
PID 1 → PID 2 복제 완료. 페이지 2장을 COW 로 공유합니다 — 프레임은 늘지 않았습니다.
```

그리고 **물리 프레임 게이지가 정말로 움직이지 않습니다.** COW를 문장으로 설명하는 것과
게이지가 가만히 있는 것을 보는 것은 다른 일입니다.

표 아래 띠에는 준비 큐가 뜹니다. MLFQ로 바꾸면 `Q0` `Q1` `Q2`가 각자의 퀀텀
(두 배씩 깁니다)과 함께 셋으로 갈라집니다. **비어 있는 큐도 감추지 않습니다** —
"Q2가 비어 있다"는 것 자체가 MLFQ를 이해하는 데 필요한 정보입니다.

### 메모리 탭

| 표 | 열 | 시스템 콜 |
|---|---|---|
| 프레임 테이블 | 프레임 · USED/FREE · PID · 페이지 · **REF** · **플래그(D · R · COW)** | `FRAMETABLE` |
| 페이지 테이블 | 페이지 · **MEM/SWAP** · 프레임 · **스왑 슬롯** · 권한 · **COW** · REF | `PAGETABLE` |

페이지 테이블은 프로세스 탭에서 고른 프로세스를 따라갑니다. 두 표를 나란히 두면
`fork` 직후 부모와 자식의 페이지가 **같은 프레임 번호**를 가리키고 프레임 쪽 REF가
2인 것이 한눈에 보입니다. `mem_write` 한 번에 **그 페이지 한 장만** 갈라지는 것도요.

### 사이드바

게이지가 넷이 되었습니다. 네 번째가 **스왑 슬롯**이고, 색은 브랜드 3색 밖의
**보라**(`-forge-violet`)입니다. 스왑은 "메모리가 디스크로 밀려났다"는 다른 층위의
사건이라 프레임 · 힙 · TLB와 같은 색 계열에 두면 오히려 섞여 보입니다. 로고의 불티와
같은 색이므로 시스템 밖으로 튀지도 않습니다.

아래에 **페이지 교체 정책**(`FIFO` · `LRU` · `CLOCK`) 선택기와 폴트 통계
(페이지 폴트 · 스왑 인 · 스왑 아웃 · COW 폴트)가 붙었습니다. 스왑이 꺼진 커널
(`swapSlots = 0`)에서는 게이지가 "스왑이 꺼져 있음"으로 뜹니다 — 항상 0인 숫자를
보여 주는 것은 자리만 먹고 아무것도 알려 주지 않습니다.

### 되비추기와 입력이 부딪히는 자리

스케줄러 · 교체 정책 선택기는 커널 값을 되비추면서 **동시에** 사용자 입력을 받습니다.
되비추는 순간에도 `setValue`는 변경 이벤트를 쏘므로, 그대로 두면 1초마다 커널에
"스케줄러를 지금 값으로 바꿔라"는 시스템 콜이 날아갑니다. `syncing` 플래그가 그
순환을 끊습니다. 교착 상태 관리자의 은행원 토글이 `syncSilently`로 푼 문제와 같은
종류이고, 콤보 상자에는 그런 메서드가 없어 플래그로 풀었습니다.

---

## 4. Finder와 메뉴바

**Finder**에 `disk.img` 기록 버튼이 생겼습니다. 누르면 경로 표시줄이 이렇게 바뀝니다.

```text
disk.img · 1024바이트 · 블록 3/16 · inode 2/16
```

커널을 디스크 이미지 없이 띄웠다면 "메모리에만 남습니다"라고 알려 줍니다.
**실패로 보고하지 않습니다** — 이미지가 없는 것은 오류가 아니라 기본 설정입니다
(`diskImagePath` 기본값 `null`).

**메뉴바**에 스왑 칩이 붙었습니다(`스왑 3/32 · 폴트 12`). 스왑이 꺼진 커널에서는
칩 자체가 사라집니다.

---

## 5. 터미널 — 고친 것이 없다

`1.1.0`에서 CLI 명령어가 33종에서 **42종**으로 늘었습니다. 터미널 앱은 **한 줄도
고치지 않았고**, 아홉 개가 그대로 들어왔습니다.

```text
fork · priority · mem_read · mem_write · pagetable · swapinfo · sync · type · diskfinish
```

`1.0` 작업에서 forge-cli의 `command` · `shell` 패키지를 export하고
`StandardCommands.createRegistry(ShellContext)`로 등록 지점을 한 곳에 모아 둔 결과입니다.
GUI가 자체 파서를 가졌다면 이번에 명령어를 아홉 개 더 구현했어야 했고, 그중 하나쯤은
CLI와 미묘하게 다르게 동작했을 것입니다.

> 터미널의 `ps` 출력에도 PPID · PRI 열이 붙어 있습니다. 그것 역시 CLI가 한 일이고,
> 터미널은 같은 `CommandRegistry`를 부를 뿐입니다.

---

## 6. 앱을 열면 프로세스가 생깁니다

`1.0`부터 `1.1.0` 개발 중반까지, ForgeOS의 앱 창은 **순수 JavaFX 노드**였습니다.
Firefox를 열어도 활성 상태 보기의 표는 비어 있었고 메모리 게이지는 0이었습니다.
운영체제 시뮬레이터의 데스크탑에서 앱을 실행했는데 **그 운영체제가 모른다**는 것은
설명하기 어려운 상태입니다.

이제 창을 열면 `EXEC` + `MALLOC`, 닫으면 `KILL`입니다.

| 앱 | 힙 |
|---|---|
| Firefox | 8바이트 (프레임 2장) |
| 나머지 다섯 | 각 4바이트 (프레임 1장) |

기본 커널의 물리 메모리가 4바이트짜리 프레임 16장, 즉 **64바이트뿐**이라 값이 이렇게
작습니다. 여기서 넉넉하게 잡으면 앱 몇 개를 여는 것만으로 물리 메모리가 차서 사용자가
만드는 프로세스가 곧바로 스왑으로 밀립니다. 여섯 앱을 다 열면 7장(16장 중)을 씁니다.

### 앱 프로세스는 CPU를 두고 다투지 않습니다

커널의 프로세스는 전부 "버스트 시간만큼 CPU를 쓰고 끝나는 배치 작업"입니다. GUI 앱은
그런 물건이 아닙니다 — 끝나지 않고, 대부분의 시간을 **입력을 기다리며** 보냅니다.
그래서 앱 프로세스는 만들자마자 `io_req … keyboard`로 키보드 대기열에 넣어 `WAITING`으로
재웁니다.

이것은 편법이 아니라 실제 모델과 같습니다. 그리고 재우지 않으면 **FCFS에서 첫 번째로
열린 앱이 CPU를 영원히 붙들고** 사용자가 만든 프로세스가 하나도 진행되지 않습니다.
비선점 스케줄러에 끝나지 않는 프로세스를 올리면 그렇게 됩니다. 우선순위도 가장 낮은
19로 두어 어쩌다 깨어나도 사용자 프로세스를 밀지 않습니다.

터미널에서 `type hello`를 치면 키보드 인터럽트가 대기열 맨 앞을 깨우므로 앱 프로세스
하나가 `READY`로 돌아옵니다. 그래서 매 갱신 펄스마다 **다시 재웁니다** — 입력을 처리하고
기다림으로 돌아가는 GUI 앱의 실제 동작과 같습니다.

> 커널이 이미 이 경우를 대비해 두었습니다. `DeviceManager.wakeOneWaiter`는 이미 종료된
> 프로세스를 깨우려다 실패하면 경고 로그만 남기고 넘어갑니다. 창을 닫아 죽은 앱 프로세스가
> 키보드 대기열에 남아 있어도 `type` 명령이 깨지지 않는 것은 그 덕분입니다.

### 프로세스를 죽이면 창이 닫힙니다

활성 상태 보기에서 `firefox` 프로세스를 강제 종료하면 Firefox 창이 닫힙니다.
표와 화면이 같은 사실을 가리키게 하려면 방향이 양쪽으로 다 통해야 합니다.
한쪽으로만 통하면 표는 장식이 됩니다.

---

## 7. 배포본 — 자바 없이 실행되는 설치 파일

ForgeOS는 `java -jar`로 도는 단일 jar를 만들 수 없습니다. JavaFX 애플리케이션은 모듈
경로에 **플랫폼별 네이티브 런타임**이 필요하기 때문입니다. 그래서 **jlink**로 필요한
JDK 모듈만 담은 런타임 이미지를 만들고, **jpackage**로 그것을 설치본으로 굽습니다.
받는 사람은 자바를 따로 깔지 않아도 됩니다.

| 산출물 | 대상 |
|---|---|
| `ForgeOS-1.1.0-macos-arm64.dmg` | Apple Silicon Mac |
| `ForgeOS-1.1.0-macos-x64.dmg` | Intel Mac |
| `ForgeOS-1.1.0-windows-x64.msi` | Windows 10 · 11 |
| `ForgeOS-1.1.0-linux-x64.deb` | Debian · Ubuntu |
| `ForgeOS-1.1.0-*-portable.zip` | 설치 없이 압축을 풀어 `bin/ForgeOS` 실행 |

**jlink 이미지는 크로스 컴파일이 되지 않습니다.** macOS에서 구우면 `.dmg`만 나옵니다.
그래서 `.github/workflows/release.yml`이 러너 넷(mac arm64 · mac x64 · win · linux)에서
각자 굽고 한데 모아 드래프트 릴리즈에 붙입니다. 로컬에서는 `./scripts/package.sh`가
지금 이 플랫폼용 하나만 만듭니다.

워크플로가 저장소 **셋**을 체크아웃하는 이유가 있습니다. 커널과 CLI는 Maven Central에
없고 mavenLocal로만 배포되므로, 빌드 전에 두 저장소를 받아 `publishToMavenLocal`을
돌려야 합니다.

> **크기.** `javafx.web`(WebKit)이 들어 있어 산출물이 300MB 안팎입니다. 브라우저 앱을
> 넣기로 한 순간 감수하기로 한 비용입니다. `--strip-debug`와 `zip-6` 압축으로 줄일 만큼은
> 줄였습니다.

> **macOS 서명.** 서명·공증하지 않았습니다. Apple Developer 계정($99/년)이 필요하고,
> 개인 프로젝트에서 그 비용을 들이지 않기로 했습니다. 받는 사람은 처음 한 번
> `xattr -dr com.apple.quarantine /Applications/ForgeOS.app`을 실행해야 합니다.
> 워크플로에 서명 단계를 넣을 자리는 비워 두었습니다.

> **Windows.** `.msi`는 WiX Toolset 3이 있어야 굽힙니다. 러너 이미지에 대개 들어 있지만,
> 없으면 워크플로가 chocolatey로 설치합니다. `--win-upgrade-uuid`는 **절대 바꾸면 안 됩니다** —
> 바꾸는 순간 새 버전이 이전 버전을 덮어쓰지 않고 나란히 설치됩니다.

---

## 호환성

- **커널 `1.1.0` · CLI `1.1.0`이 필요합니다.** `1.0` 커널로는 빌드되지 않습니다 —
  이 버전이 `swapTotalSlots` · `parentPid` · `queueLevel` 같은 새 필드를 직접 읽습니다.
- `javafx.web` 모듈이 추가되었습니다. `build.gradle.kts`의 `modules` 목록과
  `module-info.java`의 `requires` 양쪽에 있어야 합니다.
- 토큰이 하나 늘었습니다(`-forge-violet`). **`tokens-dark.css`와 `tokens-light.css`에
  같은 이름이 다 있어야 합니다** — 하나라도 빠지면 그 토큰만 해석되지 않아 검은
  사각형이 남습니다.
- `DonutChart` 생성자가 지름을 받습니다(패키지 내부 클래스라 외부 영향 없음).
- 저장된 설정이나 파일 형식은 바뀌지 않았습니다. `1.0`에서 만든 `disk.img`는 그대로 열립니다.
- `ForgeApp`에 `memoryFootprint()`가 생겼습니다. **default 메서드**라 기존 구현체는
  고칠 필요가 없습니다(기본 4바이트).
- 빌드에 `org.beryx.jlink` 플러그인이 추가되었습니다. `./gradlew build`에는 영향이
  없고, `jpackage`·`jlinkZip` 태스크를 부를 때만 동작합니다.

---

## 검증

GUI는 헤드리스 환경에서 실행할 수 없으므로 자동 검증의 범위가 정해져 있습니다.

| 검증 | 방법 | 결과 |
|---|---|---|
| 컴파일 | `javac -Xlint:all -Werror`, 모듈 경로에 커널 · CLI · JavaFX | 경고 0건 |
| 스타일시트 구문 | `javafx.css.CssParser`로 네 파일 파싱 | 오류 0건 |
| 토큰 이름 집합 | 다크/라이트 토큰 이름 비교 | 40개 · 완전 일치 |
| 토큰 참조 | 구조 시트가 참조하는 이름이 전부 정의되어 있는가 | 미해결 0건 |
| 리터럴 색 | 새로 추가한 구역에 `#RRGGBB`가 없는가 | 0건 |

**컨테이너에서 확인할 수 없는 것이 둘 더 있습니다.** Maven Central이 막혀 있어
`jpackage`·`jlinkZip`을 실제로 돌려 보지 못했습니다 — 설정은 OpenJFX가 공식 문서에서
권하는 구성(`org.beryx.jlink`)을 그대로 따랐지만, **첫 실행은 사람이 확인해야 합니다.**
앱 프로세스 배선도 GUI를 띄워야 확인됩니다.

**레이아웃과 상호작용은 컴파일로 잡히지 않습니다.** `1.0`에서 발견된 회귀(배경화면의
최소 크기가 데스크탑을 밀어 메뉴바와 Dock을 잘라 낸 것, Dock 확대가 겨냥을 방해한 것)는
전부 실제 화면에서만 드러났습니다. 이번 변경도 창을 띄워 확인해야 합니다 — 특히
활성 상태 보기의 사이드바(게이지 넷 + 정책 상자)가 작은 창에서 스크롤로 처리되는지,
브라우저 도구 모음이 좁은 창에서 접히지 않는지, 앱을 여닫을 때 프로세스 표와 힙 게이지가
따라 움직이는지, 표에서 앱 프로세스를 죽이면 그 창이 닫히는지를 보십시오.

---

## 알려진 문제

- **종료 시 JVM 크래시(`libglass.dylib`)** — `1.0`부터 이어지는 문제입니다. macOS의
  `NSWindow` 해제와 JVM 셧다운의 경쟁으로 보이며, 자바 프레임이 하나도 잡히지 않습니다.
  `gradle.properties`의 `javafxVersion`을 올려 보는 것이 1순위 조치입니다.
  이번 릴리즈에서 메모장을 대화상자 없이 만든 것은 **네이티브 창을 하나라도 덜 만들려는**
  의도이기도 합니다(Finder의 `TextInputDialog`는 아직 남아 있습니다).
- **WebView는 첫 실행이 느립니다.** WebKit 네이티브 라이브러리를 처음 적재하는 비용이며,
  탭을 처음 열 때 한 번만 나타납니다.
- **메모장은 큰 파일을 다루지 못합니다.** 커널 파일 시스템이 기본 16블록이라
  `디스크 공간이 부족합니다`가 뜨는데, 이것도 정상 동작입니다.

---

## 파일

```bash
./gradlew run           # 개발 중 실행 (또는 ./scripts/run.sh)
./gradlew build         # 컴파일 + 검증. 경고 0건이 기준선입니다
./scripts/package.sh    # 이 플랫폼용 설치본 + 포터블 이미지
```

| 산출물 | 위치 |
|---|---|
| 설치본 | `build/jpackage/ForgeOS-1.1.0.dmg` · `.msi` · `.deb` |
| 앱 이미지 | `build/jpackage/ForgeOS.app` (또는 `ForgeOS/`) |
| 포터블 런타임 | `build/image.zip` |

세 플랫폼을 한 번에 굽는 것은 `v1.1.0` 태그를 밀 때 GitHub Actions가 합니다.

| 선행 조건 | 확인 방법 |
|---|---|
| `io.github.jongwoo0101:forgeframework:1.1.0` | forge-framework에서 `./gradlew publishToMavenLocal` |
| `io.github.jongwoo0101:forgecli:1.1.0` | forge-cli에서 `./gradlew publishToMavenLocal` |

---

## 관련 저장소

| 저장소 | 설명 |
|---|---|
| [forge-framework](https://github.com/Jongwoo0101/forge-framework) | 커널 엔진 · 이번 릴리즈에 맞춘 `1.1.0` 필요 |
| [forge-cli](https://github.com/Jongwoo0101/forge-cli) | 명령어 계층 · 터미널 앱이 이 저장소의 `CommandRegistry`를 재사용합니다 |
| ForgeStudio | 운영체제 교육 · 시각화 플랫폼 (예정) |

---

<details>
<summary><b>English summary</b></summary>

## ForgeOS 1.1.0

**The kernel got wider, so the screen did too — plus the two apps a desktop is expected to have**

ForgeFramework `1.1.0` brought `fork` (copy-on-write), four more schedulers, swap with page
faults, and `disk.img` persistence. ForgeOS `1.0` compiled against that kernel but could not
*show* any of it: the new record fields and the seven new system calls had nowhere to go on
screen. This release makes that room, and adds Notepad and a browser.

### Notepad

A text editor on the **kernel's** file system, not the host's. A file saved here appears in
Finder's columns immediately, reads back through `cat`, and lands in `disk.img` on `sync` —
which is how you can watch `1.1.0`'s persistence work without typing a command.

**There is not one dialog in the app.** A `Dialog` opens a real native window each time, and
native window teardown is exactly where `1.0` reported a shutdown crash. So new file names
come from a toolbar field, and unsaved work is kept as a **draft** per path instead of being
asked about. Move between files freely — edits survive, and a dot marks what is unsaved. The
question was removed rather than answered well.

### Firefox

An honest note: this app does **not** embed Gecko. The only engine JavaFX ships is WebKit, in
`javafx.web`, and Gecko cannot be hosted in a Java process. So this is a browser running
inside a ForgeOS window, given the name and the role of default browser. Launching the host's
real Firefox would have been five lines — and the window would have escaped ForgeOS, which
ends the premise of a virtual desktop.

Tabs, a combined address/search bar, back/forward driven by `WebHistory`, and a **built-in
start page**: point home at a live site and the first thing a user without a network sees is
an error page, indistinguishable from a broken app. The toolbar reads only the selected tab
rather than binding per-tab properties, so there is nothing to unbind when tabs change.

### Activity Monitor, rebuilt around two questions

`1.0` answered one question — *who is waiting for the CPU?* With six schedulers, swap and
`fork`, there are now two, so there are two tabs. **Processes** answers *why was that one
picked* (PPID, priority, queue level, fork, scheduler picker, ready-queue strip that splits
into `Q0 Q1 Q2` under MLFQ). **Memory** answers *where did the memory go* (frame table with
REF and D/R/COW flags, page table with MEM/SWAP and swap slots, following the selected
process). Fork a process and the status line reports the shared COW pages while the
physical-frame gauge visibly does not move.

The sidebar now carries four gauges — the fourth, swap, in violet rather than one of the
three brand colours, because "pushed out to disk" is a different kind of event — plus the
page replacement policy picker and the fault counters. The pickers mirror kernel state *and*
accept input, which would otherwise send "set to the current value" once a second; a
`syncing` flag breaks that loop.

### Finder, menu bar, and a terminal that did not change

Finder gained a **write to disk** button reporting bytes, blocks and inodes; no image
configured is reported as the default it is, not as an error. The menu bar gained a swap
chip that hides itself when swap is off.

The Terminal was **not touched**, and all nine new CLI commands arrived anyway — the payoff
from exporting forge-cli's command layer and funnelling registration through
`StandardCommands` back in `1.0`. A GUI with its own parser would have had to implement nine
commands, one of which would have behaved subtly differently.

### Apps are kernel processes

Until now an app window was a plain JavaFX node: open Firefox and the Activity Monitor table
stayed empty. A desktop where the OS does not know an app is running is hard to explain. Now
opening a window runs `EXEC` + `MALLOC` and closing it runs `KILL` — Firefox takes two of the
kernel's sixteen 4-byte frames, everything else takes one.

App processes are parked in `WAITING` on the keyboard queue the moment they are created.
That mirrors what a GUI app actually does, and it is also load-bearing: an unparked
never-ending process would hold the CPU forever under FCFS and nothing the user spawns would
run. Each refresh re-parks anything that woke. Force-killing an app process closes its
window — the table has to work in both directions or it is decoration.

### Shipping — installers that need no Java

A JavaFX app cannot be a runnable `java -jar`, so **jlink** builds a runtime image with only
the JDK modules needed and **jpackage** bakes it into a `.dmg` / `.msi` / `.deb`, plus a
portable zip. jlink does not cross-compile, so `.github/workflows/release.yml` builds on four
runners (mac arm64, mac x64, Windows, Linux) and attaches everything to a draft release. It
checks out three repositories, because the kernel and CLI live only in mavenLocal and must be
published before ForgeOS can build.

Artifacts run around 300 MB thanks to WebKit. macOS builds are **not signed or notarised** —
recipients run `xattr -dr com.apple.quarantine /Applications/ForgeOS.app` once; the workflow
leaves a place for signing if a certificate ever appears.

### Compatibility

Kernel and CLI `1.1.0` are required; a `1.0` kernel will not build this. `javafx.web` is a
new module (both `build.gradle.kts` and `module-info.java`). One new design token,
`-forge-violet`, which must exist in **both** token stylesheets. On-disk formats are
unchanged — a `disk.img` written by `1.0` still opens. `ForgeApp` gained
`memoryFootprint()` as a **default** method, so existing implementations need no change, and
the `org.beryx.jlink` plugin only affects the packaging tasks.

### Verification

Zero warnings under `-Xlint:all -Werror`; all four stylesheets parse under
`javafx.css.CssParser`; the dark and light token name sets match exactly (40 each) with no
unresolved references and no literal colours in the new sections. **Layout and interaction
are not covered** — every regression found in `1.0` showed up only on a real screen, so the
sidebar's scrolling and the browser toolbar at narrow widths still want a human look.

</details>
