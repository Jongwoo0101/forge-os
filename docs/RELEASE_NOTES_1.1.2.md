# ForgeOS 1.1.2

**부팅 2단계에서 영상을 걷어냈습니다. 이제 로고가 직접 돕니다**

`1.1.1`은 부팅을 **짧게** 만들었습니다 — 타이핑을 빠르게 하고, 커널 단계 지연을 줄이고,
10초짜리 MP4를 앞 3.6초만 쓰도록 잘랐습니다. 그래도 남은 질문이 있었습니다.
**그 3.6초 동안 우리는 왜 영상 파일을 하나 들고 다녀야 하는가?**

이 릴리즈는 그 질문에 답합니다. 2단계를 도형으로 다시 만들고, 영상과 함께
`javafx.media` 모듈까지 배포에서 뺐습니다.

| | 1.1.1 | 1.1.2 |
|---|---|---|
| 2단계 | MP4 앞부분 3.6초 재생 | **2.4초 로고 스플래시** |
| 리소스 | `forgeOS-Booting-Animation2.mp4` 2.4MB | **없음** (전부 벡터) |
| 모듈 | `javafx.media` + 네이티브 | **없음** |
| 1 → 2 전환 | 콘솔이 사라진 <b>뒤에</b> 시작 (검은 화면 0.4초) | **겹침** — 글자가 흐려지는 자리에서 로고가 떠오름 |
| 부팅 전체 | ~5.9초 | **~4.3초** |

> **커널·CLI는 `1.1.1` 그대로입니다.** 이 릴리즈의 변경은 전부 ForgeOS 안에 있습니다.
> `1.1.0`부터 지켜 온 "세 저장소 번호 맞추기"는 여기서 한 번 어긋나는데, 바뀐 것이
> 없는데 태그만 다는 것보다 이쪽이 정직하다고 봤습니다.

---

## 1. 왜 영상을 걷어냈나

`1.1.1`에서 재생 시간은 3.6초로 줄었지만, 영상이 치르던 값은 그대로 남아 있었습니다.

- 리소스 **2.4MB** — 그중 실제로 재생되는 것은 앞 36%뿐
- **`javafx.media` 모듈**과 그에 딸린 네이티브 라이브러리
- `jar:` URL을 재생하지 못해 임시 파일로 풀어야 하는 우회 코드
- 그리고 **매번 똑같은 3.6초**

새 2단계(`BootSplash`)에는 도형밖에 없습니다. 리소스도 디코더도 없으므로 준비 시간이
0이고, 길이는 상수 하나로 정해집니다.

## 2. 무엇을 보여 주는가

위에서 아래로 **후광 → 로고 → 워드마크 → 스피너 → 상태 문구**입니다.

```
        ·   ·   ·   ·   ·          ← 후광: 점선 원 두 겹이 시계 방향으로 천천히
     ·                     ·
   ·        ┌─────┐         ·
  ·         │ 🔥  │  모루 위의 불꽃 — 배경화면과 같은 벡터 로고
   ·        └─────┘         ·
     ·   F O R G E O S    ·
        ForgeFramework v1.1.1 kernel
              ╭───╮
              │ ◜ │        ← 스피너: 시계 방향, 호의 길이가 숨을 쉰다
              ╰───╯           머리에 불티 하나가 붙어 돈다
          데스크탑 구성 중
```

| 요소 | 움직임 | 왜 그렇게 |
|---|---|---|
| **후광** | 점선 원 두 겹(r=268 · 188)이 초당 11° · 17°로 시계 방향 | 배경화면의 동심원과 같은 자리·같은 성격입니다. 부팅이 끝나고 배경이 드러날 때 두 화면이 "같은 세계"로 이어집니다 |
| **로고** | `ForgeMark` 132px, 0.55초 동안 커지며 밝아짐 | 배경화면이 쓰는 것과 **같은 벡터**입니다. 어느 해상도에서도 깨지지 않습니다 |
| **스피너** | 초당 210° 시계 방향, 호의 길이가 34° ↔ 292°를 1.5초 주기로 오감 | 고리가 일정한 길이로만 돌면 그것은 **시계**입니다. 길이가 숨을 쉬어야 "진행 중"으로 읽힙니다 |
| **안쪽 호** | 초당 335°, 청록 실선 66° | 하나만 돌면 시계, 둘이 다른 속도로 돌면 기계입니다 |
| **불티** | 호의 머리에 붙어 돎 | 지금 어디까지 왔는지 알려 주는 유일한 점입니다 |
| **상태 문구** | 0.3초 → 1.0초 → 1.6초에 교체 | 커널은 1단계에서 이미 다 떴습니다. 그러니 여기서 "커널 초기화 중"이라고 쓰면 거짓말입니다 — 이 구간에 **실제로 일어나는 일**만 적습니다 |

**끝맺음이 중요합니다.** 마지막 0.5초에 호가 **완전한 원으로 닫히면서** 전체가 살짝
커집니다. 이때 불티와 안쪽 호는 함께 물러납니다 — 완성된 원 안에서 다른 호가 계속 돌면
아직 진행 중으로 읽히기 때문입니다. 로딩이 흐지부지 사라지는 대신 "채워졌다"로 끝나야,
다음에 오는 데스크탑이 **그 결과로** 읽힙니다.

### 전부 도형이고, 타이머는 하나입니다

`AnimationTimer` 하나가 후광 회전 · 스피너 회전 · 호의 숨쉬기 · 등장 · 끝맺음을 전부
처리합니다. 각도는 경과 시간이 아니라 <b>지난 프레임 시간</b>으로 밀기 때문에, 프레임이
밀려도 회전 속도가 달라지지 않습니다.

> **⚠️ 각도 부호가 두 좌표계에서 반대입니다.** JavaFX `Arc`는 수학 관습을 따릅니다 —
> 0도가 3시 방향이고 **양수가 반시계**입니다. 그래서 시계 방향으로 *그리려면* 길이가
> 음수여야 하고, 시계 방향으로 *돌리려면* 시작 각도가 **줄어들어야** 합니다.
> 반대로 `Node.setRotate`는 화면 좌표계라 **양수가 시계**입니다. 같은 "시계 방향"인데
> 부호가 반대라, 한 번 틀리면 후광과 스피너가 서로 반대로 돕니다.

로고는 **고정 크기 상자**에 담았습니다. `ForgeMark` 안에는 가우시안 흐림과 발광 그림자가
있고, 효과는 노드의 경계를 바깥으로 부풀립니다. 그대로 `VBox`에 넣으면 로고가 차지하는
높이가 "132px과 발광이 번진 만큼"이 되어, 발광 세기를 조절할 때마다 워드마크가 따라
움직입니다. 상자에 크기를 못 박으면 레이아웃은 132px만 보고, 발광은 그대로 번집니다.

## 3. 두 장면을 겹쳤습니다

`1.1.1`은 콘솔이 **완전히 사라진 뒤에** 다음 장면을 시작했습니다. 두 시간이 더해지고,
그 0.4초 동안 화면에는 검은색밖에 없었습니다. 영상일 때는 어쩔 수 없었습니다 —
`MediaPlayer`를 세우는 일이 그 자리에 있었으니까요.

이제 2단계에 준비할 것이 없으므로 겹칠 수 있습니다. 둘 다 검은 배경 위에 있어서,
겹쳐 두면 **글자가 흐려지는 자리에서 로고가 떠오르는 한 장면**이 됩니다.

## 4. 1단계는 그대로입니다

타이핑 2.2ms/글자, 줄 사이 18ms, 커널 단계 지연 80ms, 콘솔 최소 체류 500ms,
장면 페이드 400ms — 전부 `1.1.1`에서 정한 값이고 이 릴리즈에서 바뀌지 않았습니다.

---

## 남겨 둔 것

`assets/`의 MP4 두 개는 **지우지 않았습니다.** 쓰는 곳은 없습니다 — 되살리고 싶을 때를
위해 남겨 둔 것이고, 배포본에서 5MB를 되찾고 싶으면 두 파일을 지우면 됩니다.

영상을 되살리려면 `module-info.java`의 `requires javafx.media`와 `build.gradle.kts`의
`javafx.modules` **양쪽을** 되돌려야 합니다.

## 호환성

- 조작 방법은 그대로입니다. `ESC` · `Space` · 클릭으로 언제든 건너뛸 수 있고, 커널 부팅
  자체는 여전히 건너뛸 수 없습니다(부팅이 끝나기 전에 누르면 완료 직후 곧바로 넘어갑니다).
- **`javafx.media` 의존이 사라졌습니다.** 배포 이미지에서 미디어 네이티브 라이브러리가
  빠집니다. 이 모듈을 쓰던 곳은 부팅 영상 하나뿐이었습니다.
- 커널 `1.1.1` · ForgeCLI `1.1.1`이 필요합니다 — `1.1.1`과 같습니다.
  JavaFX 21.0.5, JDK 21 — 변경 없음.
- 데스크탑 쪽은 아무것도 바뀌지 않았습니다. 창·앱·테마·단축키 전부 `1.1.1`과 같습니다.

## 검증

- `javac -Xlint:all -Werror` 경고 0건.
- CSS 헤드리스 검증 통과 — 네 시트 파싱, 다크/라이트 **토큰 40개 이름 집합 일치**,
  구조 시트가 참조하는 토큰 전부 정의됨.

> **사람이 봐야 하는 것:** GUI 실행은 컨테이너에서 불가능합니다.
>
> 1. 스피너가 **시계 방향**으로 도는지 (위의 각도 부호 주의 참고)
> 2. 콘솔이 흐려지는 자리에서 로고가 떠오르는지 — 검은 화면만 보이는 구간이 없어야 합니다
> 3. 마지막에 호가 완전한 원으로 닫히고 나서 데스크탑이 뜨는지
> 4. 후광 점선이 큰 창에서 너무 튀지 않는지, 최소 창(960×640)에서 잘리지 않는지

## 파일

| 파일 | 변경 |
|---|---|
| `boot/BootSplash.java` | **신규** — `BootVideo` 를 대신합니다. 로고 · 스피너 · 후광 · 상태 문구, 타이머 하나 |
| `boot/BootVideo.java` | **삭제** |
| `boot/BootSequence.java` | 2단계 교체 · 콘솔 페이드와 스플래시 등장을 겹침 |
| `boot/BootConsole.java` | `pump` 가 글자마다가 아니라 프레임마다 텍스트를 반영 |
| `module-info.java` · `build.gradle.kts` | `javafx.media` 제거 |
| `resources/forgeos/css/theme.css` | `.boot-video` → `.boot-splash` 계열 10개 규칙 |
| `README.md` · `README.en.md` | 2단계 설명 교체 |
| `gradle.properties` | `version=1.1.2` (의존 버전은 `1.1.1` 유지) |

## 관련 저장소

| 저장소 | 버전 | 비고 |
|---|---|---|
| [forge-framework](https://github.com/Jongwoo0101/forge-framework) | `1.1.1` | 이 릴리즈에서 변경 없음 |
| [forge-cli](https://github.com/Jongwoo0101/forge-cli) | `1.1.1` | 이 릴리즈에서 변경 없음 |

이전 릴리즈: **[ForgeOS 1.1.1](RELEASE_NOTES_1.1.1.md)** — 창 렌더링 · 갱신 펄스 최적화, 부팅 시간 단축, Firefox → ForgeWeb.

<details>
<summary><b>English summary</b></summary>

### The video is gone; the logo turns instead

`1.1.1` made booting *shorter* — faster typing, smaller kernel stage delays, and only the
first 3.6 seconds of the 10-second MP4. One question was left over: **why carry a video file
around for those 3.6 seconds at all?**

This release answers it. Act two is rebuilt out of shapes, and the `javafx.media` module
leaves the distribution along with the video. Boot goes from ~5.9s to ~4.3s, but the real
gain is what is no longer shipped: a 2.4MB resource of which only 36% ever played, a native
media library, and the workaround code for `Media` not being able to read `jar:` URLs.

**What it shows**, top to bottom: halo, logo, wordmark, spinner, status. The halo is two
dotted circles (r=268 · 188) turning clockwise at 11°/s and 17°/s — same place and same
character as the desktop wallpaper's rings, so the two screens read as one world. The logo is
`ForgeMark`, literally the same vector the wallpaper draws, scaling up over 0.55s. The spinner
sits beneath it running clockwise at 210°/s with its arc length breathing between 34° and 292°
on a 1.5s cycle: a ring turning at a constant length is a clock, a breathing one is a loader.
A faster thin cyan arc (335°/s) sits inside it — one ring alone looks like a clock, two at
different speeds look like a machine — and an ember rides the arc's head, the only mark that
says how far around it has come. The status line changes at 0.3s, 1.0s and 1.6s, and says only
what is actually happening in this interval: the kernel finished booting back in act one, so
claiming "initialising kernel" here would be a lie.

**The ending matters.** In the final 0.5s the arc closes into a full circle while everything
scales up slightly, and the ember and inner arc withdraw — another arc still turning inside a
completed ring reads as unfinished. A loader that fizzles out reads as an interruption; one
that completes reads as a result, and the desktop that follows is that result.

One `AnimationTimer` drives all of it, advancing angles by *frame time* rather than elapsed
time so a stalled frame does not change the rotation speed. **Mind the sign convention:**
JavaFX `Arc` uses the mathematical one — 0° at three o'clock, positive counter-clockwise — so
drawing clockwise needs a negative length and turning clockwise needs a *decreasing* start
angle, while `Node.setRotate` is in screen coordinates where positive *is* clockwise. Same
direction, opposite signs; get it wrong once and the halo and the spinner turn against each
other. The logo also lives in a fixed-size box: the blur and glow inside `ForgeMark` inflate
its bounds, so dropping it straight into a `VBox` would make the wordmark shift every time the
glow strength changed.

**The two scenes now overlap.** `1.1.1` had to start act two only after the console had fully
faded, because building a `MediaPlayer` belonged in that gap; that added the two durations
together and left 0.4s of plain black. With nothing to prepare, they can overlap — and since
both sit on black, the logo simply rises in the place the text is fading from.

Act one is unchanged from `1.1.1`. The two MP4s stay in `assets/`, unused, in case you want
the video back — restoring it means undoing the change in **both** `module-info.java` and
`build.gradle.kts`. Skipping with `ESC` / `Space` / click still works, and the kernel boot
itself still cannot be skipped.

The kernel and CLI stay at `1.1.1`: every change in this release is inside ForgeOS, and
tagging two repositories that did not change seemed worse than letting the numbers diverge.

</details>
