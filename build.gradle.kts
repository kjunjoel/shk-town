plugins {
    id("java")
    id("com.gradleup.shadow") version "9.4.1"
}

group = "kr.shkworld"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("com.mysql:mysql-connector-j:9.6.0")
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

tasks {
    shadowJar {
        archiveFileName.set("SHKTown.jar")
        destinationDirectory.set(file("../test_server/plugins"))

        relocate("com.zaxxer.hikari", "kr.shkworld.shktown.libs.hikari")
        relocate("com.mysql", "kr.shkworld.shktown.libs.mysql")
    }

    build {
        dependsOn(shadowJar)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}