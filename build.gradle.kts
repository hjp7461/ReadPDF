// 플러그인 버전 (plugins 블록에서 접근 가능하도록 최상위 변수로 선언)
val runtimePluginVersion = "1.13.0"

// 의존성 버전 관리
object Versions {
    // 플러그인
    const val runtimePlugin = "1.13.0"

    // PDF 처리 라이브러리
    const val pdfbox = "2.0.27"

    // 데이터베이스
    const val sqliteJdbc = "3.45.1.0"

    // 로깅
    const val slf4j = "2.0.9"
    const val logback = "1.4.11"

    // 유틸리티
    const val commonsLang3 = "3.14.0"
    const val commonsIo = "2.15.1"
    const val guava = "32.1.3-jre"

    // 테스트 라이브러리
    const val junitJupiter = "5.10.1"
    const val mockito = "5.8.0"
}

plugins {
    id("java")
    id("application")
    id("org.beryx.runtime") version "1.13.0" // 커스텀 JRE 생성
}

group = "com.pdfviewer"
version = "1.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21
java.targetCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
}

dependencies {
    // PDF 처리 라이브러리
    implementation("org.apache.pdfbox:pdfbox:${Versions.pdfbox}")
    implementation("org.apache.pdfbox:pdfbox-tools:${Versions.pdfbox}")
    implementation("org.apache.pdfbox:preflight:${Versions.pdfbox}")

    // 데이터베이스
    implementation("org.xerial:sqlite-jdbc:${Versions.sqliteJdbc}")

    // 로깅
    implementation("org.slf4j:slf4j-api:${Versions.slf4j}")
    implementation("ch.qos.logback:logback-classic:${Versions.logback}")

    // 유틸리티
    implementation("org.apache.commons:commons-lang3:${Versions.commonsLang3}")
    implementation("commons-io:commons-io:${Versions.commonsIo}")
    implementation("com.google.guava:guava:${Versions.guava}")

    // 테스트 라이브러리
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.junitJupiter}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.junitJupiter}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${Versions.junitJupiter}")
    testImplementation("org.mockito:mockito-core:${Versions.mockito}")
    testImplementation("org.mockito:mockito-junit-jupiter:${Versions.mockito}")
}

application {
    mainClass.set("com.pdfviewer.application.Main")
}

// macOS 배포를 위한 설정
runtime {
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
}

tasks.test {
    useJUnitPlatform()
}

// 단독 실행 가능한 JAR 파일 생성 설정
tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.pdfviewer.application.Main"
    }

    // 모든 의존성을 포함한 fat JAR 생성
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
