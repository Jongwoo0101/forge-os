// ForgeOS — ForgeFramework 커널 위에 올라가는 JavaFX 데스크탑 환경.
//
// 이 프로젝트는 커널 소스도, 명령어 파서 소스도 포함하지 않는다. 둘 다 Maven
// 아티팩트로 가져오며, 로컬 개발에서는 두 저장소에서 먼저 아래를 실행해 둔다.
//
//     forge-framework $ ./gradlew publishToMavenLocal
//     forge-cli       $ ./gradlew publishToMavenLocal
//
// 그러면 ~/.m2/repository 에 io.github.jongwoo0101:forgeframework:1.1.0 과
// io.github.jongwoo0101:forgecli:1.1.0 이 설치되고, 아래 mavenLocal()이 집어간다.

plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"

    // 배포용 실행 이미지. JavaFX 애플리케이션은 모듈 경로에 플랫폼별 런타임이
    // 필요해서 `java -jar` 로 도는 단일 jar 를 만들 수 없다. jlink 로 JRE 를 품은
    // 런타임 이미지를 만들고, jpackage 로 그것을 설치본(.dmg/.msi/.deb)으로 굽는다.
    // OpenJFX 가 공식 문서에서 권하는 조합이 이 플러그인이다.
    id("org.beryx.jlink") version "3.1.1"
}

group = "io.github.jongwoo0101"
description = "ForgeOS — JavaFX desktop environment for the ForgeFramework kernel"

repositories {
    // Forge 구성요소를 로컬에서 직접 빌드해 쓰는 것이 기본 경로이므로 mavenLocal()이 먼저다.
    mavenLocal()
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    // 커널. forgecli 가 requires transitive 로 다시 내보내지만, ForgeOS 는 커널을
    // 직접 쓰는 쪽이 주(主)이므로 의존을 명시한다 — CLI 를 떼어내도 컴파일이 깨지지 않도록.
    implementation("io.github.jongwoo0101:forgeframework:${property("forgeFrameworkVersion")}")

    // 명령어 계층. Terminal 앱이 CLI 와 완전히 같은 명령어 세트를 쓰기 위한 의존이다.
    // GUI 가 자체 파서를 갖는 순간 "CLI 에서는 되는데 GUI 에서는 안 되는" 명령이 생긴다.
    implementation("io.github.jongwoo0101:forgecli:${property("forgeCliVersion")}")
}

// ── JavaFX ─────────────────────────────────────────────────────────
//
// 플러그인이 현재 플랫폼(mac-aarch64 / win / linux)에 맞는 classifier 를 알아서
// 붙이고, 모듈 경로 구성까지 처리한다.
//
// 1.1.1 에서 media 가 빠졌다. 부팅 2단계가 MP4 재생에서 벡터 스플래시로 바뀌면서
// MediaView 를 쓰는 곳이 하나도 남지 않았다. 영상을 되살리려면 여기와
// module-info.java 양쪽에 되돌려야 한다.
// web 은 ForgeWeb 앱(WebView)이 쓴다. 이 모듈만 유일하게 무게가 다르다 —
// WebKit 네이티브 라이브러리가 통째로 딸려 와서 배포 크기가 100MB 가까이 늘어난다.
// 그럼에도 넣은 이유는, 브라우저 없이 "데스크탑 환경"이라고 부르기 어렵기 때문이다.
javafx {
    version = property("javafxVersion") as String
    modules = listOf("javafx.controls", "javafx.graphics", "javafx.web")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // 커널·CLI 와 같은 기준선: 경고 0건.
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "ForgeFramework",
            "Created-By" to "Gradle ${gradle.gradleVersion}",
            "Build-Jdk-Spec" to "21"
        )
    }
}

// ── 실행 ───────────────────────────────────────────────────────────
//
// mainClass 는 Application 서브클래스(ForgeOsApp)가 아니라 Launcher 다.
// JavaFX 애플리케이션 클래스를 직접 main 으로 지정하면 JavaFX 런타임이
// 모듈 경로에 없을 때 "JavaFX runtime components are missing" 로만 죽어서
// 원인을 알기 어렵다. Launcher 를 한 겹 두면 클래스패스 실행도 가능해진다.
application {
    mainModule = "forgeos"
    mainClass = "forgeos.Launcher"
    applicationDefaultJvmArgs = listOf(
        "-Dfile.encoding=UTF-8",
        // 창 드래그·스프링 애니메이션이 60fps 아래로 떨어지지 않도록 강제 렌더 파이프라인 확인용.
        // 문제가 생기면 -Dprism.order=sw 로 소프트웨어 렌더링 폴백을 확인할 수 있다.
        "-Dprism.lcdtext=false"
    )
}

// ── 배포 ───────────────────────────────────────────────────────────
//
// jlink 가 만드는 런타임 이미지에는 실행에 필요한 JDK 모듈만 들어간다. 그래서
// 받는 사람은 자바를 따로 깔지 않아도 되고, 대신 이미지가 플랫폼마다 하나씩
// 필요하다 — 릴리즈에 macOS·Windows·Linux 산출물이 따로 올라가는 이유다.
//
// javafx.web(WebKit)이 들어 있어 산출물이 300MB 안팎으로 크다. 브라우저 앱을
// 넣기로 한 순간 감수하기로 한 비용이다.

// 저작권 표기는 beryx 의 jpackage 블록에 속성이 없다. jpackage 자체의 --copyright
// 옵션으로 넘긴다 — 플러그인이 모르는 값은 전부 이 경로로 지나간다.
// (const val 은 .kts 스크립트에서 허용되지 않으므로 일반 val 이다.)
val copyrightNotice = "Copyright (c) 2026 Jongwoo Won"

val hostOs: String = System.getProperty("os.name").lowercase()
val isMacHost = hostOs.contains("mac")
val isWindowsHost = hostOs.contains("win")

jlink {
    // --strip-debug 와 압축으로 이미지가 절반 가까이 줄어든다. 디버그 심볼은
    // 배포본에서 쓸 일이 없고, 스택 트레이스의 줄 번호는 그대로 남는다.
    options.set(listOf("--strip-debug", "--compress", "zip-6", "--no-header-files", "--no-man-pages"))

    launcher {
        name = "ForgeOS"
        // application 블록의 applicationDefaultJvmArgs 는 gradle run 전용이다.
        // 배포본 실행기에는 여기 적은 것만 들어간다 — 한글이 깨지지 않도록 인코딩은 필수.
        jvmArgs = listOf("-Dfile.encoding=UTF-8", "-Dprism.lcdtext=false")
    }

    jpackage {
        imageName = "ForgeOS"
        installerName = "ForgeOS"
        appVersion = project.version.toString()
        vendor = "ForgeFramework"

        when {
            isMacHost -> {
                icon = "assets/icons/forgeos.icns"
                installerType = "dmg"
                // 서명하지 않는다. Apple Developer 계정이 없으면 공증도 불가능하고,
                // 서명 없는 앱은 처음 열 때 사용자가 Gatekeeper 를 한 번 넘겨야 한다.
                // 그 방법은 README 와 릴리즈 노트에 적어 둔다.
                imageOptions = listOf(
                    "--copyright", copyrightNotice,
                    "--mac-package-name", "ForgeOS",
                    "--mac-package-identifier", "io.github.jongwoo0101.forgeos"
                )
            }

            isWindowsHost -> {
                icon = "assets/icons/forgeos.ico"
                installerType = "msi"
                imageOptions = listOf("--copyright", copyrightNotice)
                installerOptions = listOf(
                    "--win-dir-chooser",
                    "--win-menu",
                    "--win-menu-group", "ForgeOS",
                    "--win-shortcut",
                    // 업그레이드 UUID 는 절대 바꾸면 안 된다. 바꾸는 순간 새 버전이
                    // 이전 버전을 덮어쓰지 않고 나란히 설치된다.
                    "--win-upgrade-uuid", "5ec6558c-2470-5ac6-b0ff-aa7bb07a51fa"
                )
            }

            else -> {
                icon = "assets/icons/forgeos.png"
                installerType = "deb"
                imageOptions = listOf("--copyright", copyrightNotice)
                installerOptions = listOf(
                    "--linux-shortcut",
                    "--linux-menu-group", "Development",
                    "--linux-deb-maintainer", "wonjongwoo01@gmail.com"
                )
            }
        }
    }
}
