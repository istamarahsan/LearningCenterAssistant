import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.8.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.bnec"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.discord4j:discord4j-core:3.2.4")
    implementation("io.arrow-kt:arrow-core:1.2.0-RC")
    implementation("com.mysql:mysql-connector-j:8.0.32")
    implementation("org.apache.commons:commons-dbcp2:2.9.0")
    implementation("org.ktorm:ktorm-core:3.6.0")
    implementation("org.slf4j:slf4j-simple:2.0.7")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "org.bnec.lca.MainKt"
    }
}

tasks.withType<ShadowJar> {
    archiveFileName.set("${project.name}.jar")
}