import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    kotlin("jvm") version "1.6.20"
    `maven-publish`
}

group = "dev.virefire.viira"
version = "1.0.1"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.rikonardo.com/releases")
    }
}

dependencies {
    implementation("io.ktor:ktor-server-cio:2.0.0")
    implementation("dev.virefire.kson:KSON:1.2.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.register("createProperties") {
    doLast {
        val f = File("$buildDir/resources/main/version.properties")
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