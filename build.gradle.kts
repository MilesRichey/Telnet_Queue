plugins {
    id("java")
    id("application")
    //id("org.beryx.jlink") version "3.0.1"
    id("org.beryx.runtime") version "1.13.1"
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "co.raring"
version = "1.0.9b"
description = "TelnetQueue"

application {
    mainClass.set("co.raring.telnetqueue.App")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

javafx {
    version = "21.0.4"
    modules = listOf("javafx.controls", "javafx.fxml")
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    // Log4J
    // implementation("log4j:log4j:1.2.17")
    // Log4J 2
    implementation("org.apache.logging.log4j:log4j-api:2.23.1")
    implementation("org.apache.logging.log4j:log4j-core:2.23.1")
    //annotationProcessor("org.apache.logging.log4j:log4j-core:2.23.1")

    // JNA for Windows Registry
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")

    // JavaFX
    implementation("org.openjfx:javafx-controls:${javafx.version}")
    implementation("org.openjfx:javafx-fxml:${javafx.version}")

    // JUnit Jupiter
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "co.raring.telnetqueue.App"
        )
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
// Still need to test below on Windows :jpackage
/*jlink {
    // Directory for the custom JRE
    imageZip.set(project.file("${layout.buildDirectory}/distributions/${project.name}-${project.version}-runtime.zip"))

    launcher {
        name = "TelnetQueue"
    }

    jpackage {
        imageOptions.addAll(
            listOf(
                "--icon", "src/main/resources/co/raring/telnetqueue/zenner.ico", // Update path to your icon
                "--win-console"
            )
        )
        installerOptions.addAll(
            listOf(
                "--win-dir-chooser",
                "--win-shortcut",
                "--win-menu",
                "--win-menu-group", "TelnetQueue Group"
            )
        )
        installerType = "exe"
        appVersion = project.version.toString()
    }
}*/
