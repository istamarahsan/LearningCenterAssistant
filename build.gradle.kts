plugins {
    kotlin("jvm") version "1.8.20"
    application
}

group = "org.bnec"

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

application {
    mainClass.set("org.bnec.lca.MainKt")
}