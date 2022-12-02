import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    kotlin("jvm") version "1.6.20"
    `maven-publish`
}

group = "dev.virefire.viira"
version = "1.1.0"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.rikonardo.com/releases")
    }
}

dependencies {
    implementation("io.ktor:ktor-server-cio:2.1.3")
    implementation("dev.virefire.kson:KSON:1.3.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.register("createProperties") {
    doLast {
        val f = File("$buildDir/resources/main/dev/virefire/viira/version.properties")
        File(f.parent).mkdirs()
        val p = Properties()
        p["version"] = project.version.toString()
        p.store(f.outputStream(), null)
    }
}

tasks.getByName("jar") {
    dependsOn("createProperties")
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            pom {
                name.set("Viira")
                description.set("Kotlin HTTP framework")
            }
        }
    }
    repositories {
        if (rootProject.file("publish.properties").exists()) {
            maven {
                val properties = Properties()
                properties.load(rootProject.file("publish.properties").inputStream())
                url = uri(properties["deployRepoUrl"].toString())
                credentials {
                    username = properties["deployRepoUsername"].toString()
                    password = properties["deployRepoPassword"].toString()
                }
            }
        }
    }
}
