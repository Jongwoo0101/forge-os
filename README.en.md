<div align="center">

<img src="assets/forgeOS-banner.svg" alt="ForgeOS — JavaFX Desktop Environment for the ForgeFramework Kernel" width="860">

[한국어](README.md) · **🇺🇸 English**

</div>

---

# ForgeOS

ForgeOS is a JavaFX desktop environment that runs on top of the
[ForgeFramework](https://github.com/Jongwoo0101/forge-framework) kernel. Watch processes move
through the scheduler in a table, read memory pressure off donut gauges, and see deadlocked
processes waiting on each other **as a picture** rather than a list of PIDs.

This repository contains neither kernel code nor command-parser code. Both arrive as Maven
artifacts, and the dependency is **one-way**: if ForgeOS disappeared, the kernel would not
notice.

```text
                       ForgeFramework          ← the kernel (separate repository)
                              ▲
                              │  requires
                ┌─────────────┼─────────────┐
                │             │             │
          ★ ForgeOS ★     ForgeCLI      ForgeStudio
                │             ▲
                └─────────────┘
                   requires — the Terminal app reuses the CLI's command layer
```

The Terminal app does not write its own parser; it reuses ForgeCLI's. Maintaining two parsers
guarantees that sooner or later a command works in the CLI but not in the GUI.

---

## Contents

- [Quick start](#quick-start)
- [The cinematic boot](#the-cinematic-boot)
- [Using the desktop](#using-the-desktop)
  - [Menu bar](#menu-bar)
  - [Dock](#dock)
  - [Windows](#windows)
  - [Keyboard shortcuts](#keyboard-shortcuts)
- [Built-in apps](#built-in-apps)
  - [Terminal](#terminal)
  - [Activity Monitor](#activity-monitor)
  - [Finder](#finder)
  - [Deadlock Resolver](#deadlock-resolver)
- [Theme and wallpaper](#theme-and-wallpaper)
- [Learn by scenario](#learn-by-scenario)
- [Project layout](#project-layout)
- [License](#license)

---

## Quick start

### Requirements

- **JDK 21** or newer
- ForgeFramework kernel `1.0` installed in your local Maven repository (`~/.m2`)
- ForgeCLI `1.0.1` or newer installed in your local Maven repository

You do not install JavaFX separately. The Gradle plugin (`org.openjfx.javafxplugin`) fetches
the runtime for your platform (mac-aarch64 · win · linux) automatically.

### 1. Install the kernel first

```bash
git clone https://github.com/Jongwoo0101/forge-framework.git
cd forge-framework
./gradlew publishToMavenLocal      # or ./scripts/publish.sh
```

### 2. Install the command layer (ForgeCLI)

```bash
git clone https://github.com/Jongwoo0101/forge-cli.git
cd forge-cli
./gradlew publishToMavenLocal
```

This installs `io.github.jongwoo0101:forgeframework:1.0` and
`io.github.jongwoo0101:forgecli:1.0.1` into `~/.m2/repository`.

### 3. Run ForgeOS

```bash
git clone https://github.com/Jongwoo0101/forge-os.git
cd forge-os

./scripts/run.sh                   # or ./gradlew run
```

> If dependency resolution fails, you almost certainly skipped a `publishToMavenLocal` in
> step 1 or 2. ForgeCLI must be `1.0.1` or newer — that is the release that first exported
> its command layer for other clients to use.

### Artifacts

| Command | Description |
|---|---|
| `./gradlew run` | Launch during development. Fastest path. |
| `./gradlew build` | Compile and verify. Zero warnings (`-Xlint:all -Werror`) is the baseline. |

A JavaFX application needs a platform-specific runtime on the module path, so unlike ForgeCLI
this project does not ship a single `java -jar` runnable jar.

---

## The cinematic boot

Launching does not drop you straight onto a desktop. Three stages run first.

### Stage 1 — terminal boot log

Kernel log lines are typed out one character at a time on a black screen. **This is not
staged text — it is the real kernel log.** The kernel boots on a background thread and every
line `EventLogger` emits is piped straight to the screen.

```text
=================================================
 ForgeFramework v1.0
 Operating System Kernel Architecture Engine
=================================================
[10:32:11.153] [INFO] Hardware check...
[10:32:11.306] [INFO] Initialising event logger...
[10:32:11.457] [INFO] Initialising kernel...
[10:32:11.607] [INFO] Initialising subsystems...
[10:32:11.612] [INFO] ProcessManager initialized [Scheduler: Round Robin (RR), timeQuantum: 3]
...
```

### Stage 2 — MP4 boot animation

The text fades out and `src/main/resources/assets/forgeOS-Booting-Animation2.mp4` plays
full-screen through a `MediaView`.

### Stage 3 — desktop

When playback ends (`setOnEndOfMedia`) the screen cross-fades into the desktop: wallpaper,
top menu bar, bottom Dock.

> **Stages are joined by events, not by time.** Stitching them together with fixed durations
> is guaranteed to drift — kernel boot time varies by machine and typing time varies with log
> length. Stage 1 → 2 fires when *the kernel has booted **and** the typing queue has drained*;
> stage 2 → 3 fires on the *end-of-media event*.

`ESC`, `Space` or a click skips the presentation. Kernel boot itself cannot be skipped, so
pressing early simply jumps to the desktop the moment boot completes.

---

## Using the desktop

### Menu bar

The translucent bar across the top. The left side answers "what am I using", the right side
answers "what is the kernel doing".

| Side | Contents |
|---|---|
| Left | Forge mark · active window title · `창` (Window) menu |
| Right | Process count · memory usage · uptime · theme toggle · clock |

The three chips on the right refresh every second via `PS`, `MEMINFO` and `UPTIME` system
calls. Keeping kernel state visible in one line no matter which app you are in is the point
of this simulator, so it gets permanent real estate.

### Dock

A glass bar at the bottom centre. Hovering an icon shows the **app name directly above it,
centred**. Icon sizes never change.

- Click an icon to launch. If the app is already open, its window comes forward.
- Click the active window's icon again to minimise it.
- Running apps get a cyan border and a dot underneath.

This started out as macOS-style magnification, but no falloff curve removed the sense that
**neighbouring icons rise along with the one you are aiming at**. In a dock of four
widely-spaced icons, magnification does not help you aim — it blurs the answer to "what am I
pointing at". So nothing resizes; only the name appears. It answers the actual question and
leaves the screen still.

The name is a label inside the Dock, not a `Tooltip`. JavaFX tooltips follow the pointer and
appear below and to the right, which — at the very bottom of the screen — either covers the
icon or falls off the display.

### Windows

Instead of spawning multiple JavaFX `Stage`s, windows are **nodes** on the desktop (a custom
MDI). That is what lets them stay inside the desktop area, get sucked into the Dock, and
layer over translucent material.

| Action | How |
|---|---|
| Move | Drag the title bar. The grab point is preserved; dragging past the edge rubber-bands and springs back. |
| Resize | Drag any of the eight window edges |
| Full screen | Green button, or double-click the title bar |
| Minimise | Yellow button → flies into the Dock. Click the Dock icon to restore. |
| Close | Red button |
| Bring to front | Click anywhere in the window |

The traffic lights follow macOS placement and behaviour but use the Forge palette —
🔴 Forge Ember · 🟡 Molten Gold · 🟢 Electric Cyan. The glyphs appear on all three at once
when the pointer enters any of them.

### Keyboard shortcuts

| Key | Action |
|---|---|
| `Cmd/Ctrl + W` | Close the active window |
| `Cmd/Ctrl + M` | Minimise the active window |
| `Cmd/Ctrl + Shift + L` | Toggle light / dark theme |
| `ESC` · `Space` · click | Skip the boot presentation |

---

## Built-in apps

All four bind the kernel's **immutable record DTOs directly** to tables and shapes rather
than turning them into strings first. If the GUI re-parsed text that the CLI had formatted,
every formatting change would break the screen.

### Terminal

Runs all **33 ForgeCLI commands** unchanged. The same input produces exactly the same result
as the CLI, because it is the same `CommandRegistry`.

| Feature | How |
|---|---|
| Run a command | Type and press `Enter` |
| Previous command | `↑` · `↓` |
| Command completion | `Tab` (prints candidates when ambiguous) |
| Clear the screen | `clear` or `Cmd/Ctrl + L` |

```text
forgeframework:/> exec worker 20
forgeframework:/> ps
PID   | STATE      | CPU_TIME | BURST      | NAME
-------------------------------------------------------
1     | RUNNING    | 3        | 20         | worker
```

Failed commands render in Ember, prompts in Cyan. Typing `shutdown` brings the kernel down
and covers the whole desktop with a shutdown screen.

> The Terminal stays dark even in the light theme. A white shell makes fixed-width output
> read like a document, and it severs the sense that this is the same thing as the boot
> console.

### Activity Monitor

A process table and memory gauges, refreshed once per second.

| Area | Contents | System calls |
|---|---|---|
| Table | PID · name · state badge · CPU usage · progress bar | `PS` |
| Toolbar | Spawn a process · force-kill the selection · current scheduler | `EXEC` `KILL` `SCHEDULER` |
| Donuts | Physical frame usage · heap usage · TLB hit ratio | `MEMINFO` |

State badges are colour-coded by meaning — `RUNNING`/`READY` in Cyan, `WAITING` in Gold,
`TERMINATED` in Ember. You see the state before you read the word.

The donut accents differ by what they measure: physical frames are trouble when exhausted
(Ember), heap is a caution signal (Gold), and TLB hit ratio is a value you want high (Cyan).

> The table is replaced wholesale every second but **the selection survives**. A table that
> drops your selection while you reach for the kill button is unusable.

### Finder

Browses the file system in a macOS multi-column view. Columns beat a tree view here because
this file system is inode-based: a column view shows both "which directory am I in" and
"what else lives beside this entry" at the same time.

| Action | Result | System calls |
|---|---|---|
| Select a directory | A new column appears to the right | `LS` |
| Select a file | A content preview appears to the right | `CAT` |
| New folder · new file | Created inside the current column | `MKDIR` `TOUCH` |

### Deadlock Resolver

The left half is **numbers** (Allocation · Max · Need); the right half is **relationships**
(who waits on whom). Banker's is a matrix algorithm and deadlock detection is a graph
algorithm, so putting them side by side shows the same situation two ways at once.

| Area | Contents | System calls |
|---|---|---|
| Toolbar | Banker's toggle · victim policy · detect · recover | `BANKER` `DETECT` `RECOVER` |
| Left table | Available/total vectors plus per-process Allocation · Max · Need · pending request | `RES_INFO` |
| Right | Wait-for graph — processes on a circle, wait edges as arrows | `DETECT` |
| Bottom | Enter a PID and a vector to declare max, request, or release | `RES_MAX` `RES_REQ` `RES_FREE` |

Only the deadlocked nodes turn Ember, with a halo that slowly pulses. Every other node stays
neutral, so your eye goes straight there — no hunting for the cycle in a table.

> The toggle switch only **reflects the kernel's value**. Type `banker off` in the Terminal
> and the switch follows. A switch that owns its own state drifts out of sync with the kernel.

---

## Theme and wallpaper

Toggle light ↔ dark with the sun/moon icon on the right of the menu bar, or
`Cmd/Ctrl + Shift + L`. The icon shows **where pressing it takes you**, not the current state.

Switching replaces the **token stylesheet wholesale** rather than adding a class to the root.
Context menus, tooltips and combo popups each own their own `Scene`, so a root class never
reaches them — but they do inherit the owning scene's stylesheets.

```text
tokens-dark.css / tokens-light.css   colours only; the token name sets must match exactly
theme.css / apps.css                 structure and dimensions; no literal colours allowed
```

Three places deliberately ignore the theme: the **boot screen** (a machine's console has no
light mode), the **Terminal**, and the **logo mark** (a brand is the same object in both
themes).

The wallpaper is not an image file but a vector rebuilt from the paths in
`assets/forgeOS-logo.svg` (`ui/ForgeMark`). JavaFX cannot read SVG files, and baking a PNG
would smear at wallpaper size and could not follow the theme. It scales with the screen's
shorter side and sits **dead centre**. Being covered by windows is a wallpaper's normal
condition; what matters more is that the screen has an axis when nothing is open.

---

## Learn by scenario

### Scenario 1 — watch the scheduler turn processes over

1. Open **Activity Monitor** from the Dock.
2. Enter a name and burst time in the toolbar and press `실행` a few times to spawn 3–4
   processes.
3. Sit still. A timer interrupt fires once per second; state badges flip between `READY` and
   `RUNNING` and the progress bars fill.
4. Open the Terminal, type `scheduler fcfs`, and look at the table again. Preemption is gone —
   a process now holds the CPU until it finishes.

### Scenario 2 — build a deadlock and see the cycle

Open **Deadlock Resolver** and switch Banker's algorithm **off** (a deadlock cannot form
while avoidance is on). In the Terminal, spawn two processes and request resources in this
order:

```text
exec p1 30
exec p2 30
res_req 1 6 0 0
res_req 2 4 4 0
res_req 1 4 0 0        ← blocks
res_req 2 0 2 0        ← blocks → circular wait complete
```

Back in Deadlock Resolver both nodes are Ember and their arrows form a loop. Press `복구`
(Recover) and a victim is terminated, breaking the cycle.

### Scenario 3 — the moment avoidance blocks a request

Same app, this time with the toggle **on**:

```text
exec p1 30
exec p2 30
res_max 1 7 5 3
res_max 2 3 2 2
res_req 1 7 4 3        ← GRANTED
res_req 2 3 2 2        ← BLOCKED_UNSAFE: resources remain, but granting them is unsafe
```

The Available vector still shows headroom, yet the request is refused. That is avoidance
earning its keep. Turn the toggle off, repeat the request, and it succeeds — after which the
deadlock from scenario 2 becomes possible.

### Scenario 4 — watch the TLB hit ratio climb

Leave **Activity Monitor** open and run this in the Terminal:

```text
exec app 20
malloc 1 8
translate 1 0          ← MISS
translate 1 0          ← HIT
translate 1 0
```

The TLB donut fills. Since the TLB holds only 4 entries, sweeping five or more distinct pages
drops the ratio again — replacement, visible.

---

## Project layout

```text
forge-os/
├── assets/                              # logo and banner SVGs (README and releases)
├── build.gradle.kts                     # kernel + CLI as Maven artifacts, JavaFX plugin
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
    │       ├── Launcher.java            # entry point — one layer so a missing JavaFX runtime is diagnosable
    │       ├── ForgeOsApp.java          # makes one window and runs the boot sequence. That is all
    │       ├── core/
    │       │   └── KernelService.java   # the only channel between kernel and UI (thread boundary, refresh pulse)
    │       ├── boot/
    │       │   ├── BootSequence.java    # three-stage orchestration
    │       │   ├── BootConsole.java     # typewriter (arrival and output separated by a queue)
    │       │   └── BootVideo.java       # MediaView (handles media-inside-jar)
    │       ├── desktop/
    │       │   ├── DesktopPane.java     # layer order and work-area computation
    │       │   ├── Wallpaper.java       # logo wallpaper
    │       │   ├── MenuBarView.java     # top menu bar
    │       │   └── DockView.java        # bottom Dock (magnification)
    │       ├── wm/
    │       │   ├── WindowManager.java   # window layer — open, focus, minimise, close
    │       │   ├── ForgeWindow.java     # a single window (drag, resize, zoom)
    │       │   └── TrafficLights.java   # window control buttons
    │       ├── app/
    │       │   ├── ForgeApp.java        # id() · title() · iconPath() · launch()
    │       │   ├── AppInstance.java     # (view, cleanup) pair
    │       │   ├── AppCatalog.java      # Dock order = list order
    │       │   ├── terminal/            # reuses the ForgeCLI command layer
    │       │   ├── monitor/             # TableView + donut gauges
    │       │   ├── finder/              # multi-column view
    │       │   └── deadlock/            # Banker's toggle + wait-for graph
    │       └── ui/
    │           ├── SpringValue.java     # spring animation
    │           ├── Motion.java          # global motion constants
    │           ├── ForgeMark.java       # the logo SVG rebuilt as vectors
    │           ├── ThemeManager.java    # light/dark switching
    │           ├── Glyphs.java          # icons (SVG paths)
    │           ├── Styles.java          # CSS pseudo-classes
    │           └── ToggleSwitch.java
    └── resources/
        ├── assets/                      # runtime resources such as the boot animation MP4
        └── forgeos/css/
            ├── tokens-dark.css          # colours only
            ├── tokens-light.css         # colours only
            ├── theme.css                # shell structure and dimensions
            └── apps.css                 # app structure and dimensions
```

### Design principles

- **The kernel composes no sentences.** It returns immutable record DTOs; ForgeCLI renders
  them as tables and ForgeOS renders them as shapes. Same DTOs, different presentation.
- **The command layer is never rewritten.** The Terminal app uses ForgeCLI's
  `CommandRegistry` directly, so a command added to the CLI appears in the GUI for free.
- **There is exactly one background → FX thread boundary: `KernelService`.** Kernel logs
  arrive from the boot thread and the timer device thread, and every one of them is handed
  over with `Platform.runLater` there. System calls, by contrast, run on the FX thread
  directly — they are in-memory operations, and pushing them to a background thread only adds
  a round trip that makes the UI respond a beat late.
- **Colours and dimensions live only in CSS.** Java toggles `styleClass` and pseudo-classes;
  it never calls `setStyle()`. A design change must not require a compile.
- **Animation is springs, not `Transition`.** A fixed-duration script discards its velocity
  when the target changes mid-flight, producing a visible hitch the moment you grab a moving
  window. A spring carries position and velocity continuously, so the path stays smooth
  through any reversal.
- **Adding an app is one interface plus one line.** Implement `ForgeApp` and add it to
  `AppCatalog.defaults()`; the Dock and the window manager pick it up automatically.

---

## Related repositories

<table>
  <tr>
    <td width="64" align="center"><img src="assets/forge-framework-logo.svg" alt="" width="48"></td>
    <td>
      <b><a href="https://github.com/Jongwoo0101/forge-framework">forge-framework</a></b><br>
      <sub>The kernel engine this project depends on ·
      <a href="https://github.com/Jongwoo0101/forge-framework/blob/master/docs/api/README.en.md">API docs</a></sub>
    </td>
  </tr>
  <tr>
    <td width="64" align="center"><img src="assets/forge-cli-logo.svg" alt="" width="48"></td>
    <td>
      <b><a href="https://github.com/Jongwoo0101/forge-cli">forge-cli</a></b><br>
      <sub>The kernel's command-line client — the Terminal app reuses its command layer</sub>
    </td>
  </tr>
  <tr>
    <td width="64" align="center"><img src="assets/forgeOS-logo.svg" alt="" width="48"></td>
    <td><b>forge-os</b><br><sub>This repository — the JavaFX GUI operating-system simulator</sub></td>
  </tr>
  <tr>
    <td width="64" align="center"></td>
    <td><b>ForgeStudio</b><br><sub>Operating-system teaching and visualisation platform (planned)</sub></td>
  </tr>
</table>

---

## License

MIT License
