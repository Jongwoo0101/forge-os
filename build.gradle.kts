// ForgeOS — ForgeFramework 커널 위에 올라가는 JavaFX 데스크탑 환경.
//
// 이 프로젝트는 커널 소스도, 명령어 파서 소스도 포함하지 않는다. 둘 다 Maven
// 아티팩트로 가져오며, 로컬 개발에서는 두 저장소에서 먼저 아래를 실행해 둔다.
//
//     forge-framework $ ./gradlew publishToMavenLocal
//     forge-cli       $ ./gradlew publishToMavenLocal
//
// 그러면 ~/.m2/repository 에 io.github.jongwoo0101:forgeframework:1.0 과
// io.github.jongwoo0101:forgecli:1.0.1 이 설치되고, 아래 mavenLocal()이 집어간다.

plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
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
// 붙이고, 모듈 경로 구성까지 처리한다. media 는 부팅 애니메이션(MP4) 재생에
// 반드시 필요하다 — 빼면 MediaView 가 통째로 사라진다.
javafx {
    version = property("javafxVersion") as String
    modules = listOf("javafx.controls", "javafx.graphics", "javafx.media")
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
