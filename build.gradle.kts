plugins {
    java
}

group = "com.camping"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

// Versions
val cucumberVersion = "7.14.0"
val restAssuredVersion = "5.3.2"
val jacksonVersion = "2.17.2"

dependencies {
    // Cucumber
    testImplementation("io.cucumber:cucumber-java:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-picocontainer:$cucumberVersion")

    // RestAssured
    testImplementation("io.rest-assured:rest-assured:${restAssuredVersion}")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:${jacksonVersion}")

    // JUnit Jupiter
    testImplementation("org.junit.platform:junit-platform-suite:1.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-suite-engine:1.10.0")

    // Assertions
    testImplementation("org.assertj:assertj-core:3.25.1")

    // JDBC driver for test hooks
    testImplementation("com.mysql:mysql-connector-j:8.3.0")
}

tasks.test {
    useJUnitPlatform()
    // Gradle의 -D 플래그는 Gradle JVM에만 적용되므로, 테스트 JVM으로 명시적으로 전달한다.
    val tagFilter = System.getProperty("cucumber.filter.tags")
    if (tagFilter != null) {
        systemProperty("cucumber.filter.tags", tagFilter)
    }
}

// ──────────────────────────────────────────
// Smoke test lifecycle tasks
// ──────────────────────────────────────────
val smokeComposeFile = "infra/docker/docker-compose.yml"

tasks.register<Exec>("smokeUp") {
    group = "smoke"
    description = "Docker Compose로 키오스크 컨테이너 기동"
    commandLine("docker", "compose", "-f", smokeComposeFile, "up", "-d", "--build")
}

tasks.register<Exec>("smokeDown") {
    group = "smoke"
    description = "키오스크 컨테이너 종료"
    commandLine("docker", "compose", "-f", smokeComposeFile, "down")
}

tasks.register<Test>("smokeTest") {
    group = "smoke"
    description = "@smoke 테스트 실행 (컨테이너 기동/종료는 SmokeHooks가 자동 처리)"
    useJUnitPlatform()
    systemProperty("cucumber.filter.tags", "@smoke")
}
