plugins {
    java
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    jacoco
}

group = "com"
version = "0.0.1-SNAPSHOT"
description = "pt-final-251022-be"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    runtimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jacoco {
    toolVersion = "0.8.12" // 최신 안정 버전 고정(예: 0.8.12)
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport) // ✅ 테스트 끝나면 리포트 생성
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)   // CI/분석툴 연동용
        html.required.set(true)  // 사람이 보는 리포트
        csv.required.set(false)
    }
    // (선택) 생성/부트/DTO 등 제외
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/Q*",                   // ✅ QueryDSL Q-classes
                    "**/*Application*",        // 부트스트랩
                    "**/*Config*",             // 설정(원하면)
                    "**/*Dto*", "**/*Request*", "**/*Response*"
                )
            }
        })
    )
}

// (선택) 임계치 검증 — 기준 미만이면 빌드 실패
tasks.register<JacocoCoverageVerification>("jacocoVerify") {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit { minimum = "0.60".toBigDecimal() } // 60%
        }
    }
}
