plugins {
    id("java")
    id("application")
    //id("org.beryx.jlink") version "3.0.1"
    //id("org.beryx.runtime") version "1.13.1"
    id("com.dua3.gradle.runtime") version "1.13.1-patch-1"
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "co.raring"
version = "1.0.9"
description = "TelnetQueue"

application {
    //mainModule.set("co.raring.telnetqueue")
    applicationName = "TelnetQueue"
    mainClass = "co.raring.telnetqueue.Launcher"
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
    implementation("org.apache.logging.log4j:log4j-api:2.24.1")
    implementation("org.apache.logging.log4j:log4j-core:2.24.1")
    //annotationProcessor("org.apache.logging.log4j:log4j-core:2.23.1")

    // JNA for Windows Registry
    implementation("net.java.dev.jna:jna:5.15.0")
    implementation("net.java.dev.jna:jna-platform:5.15.0")

    // JavaFX
    implementation("org.openjfx:javafx-controls:${javafx.version}")
    implementation("org.openjfx:javafx-fxml:${javafx.version}")

    // JUnit Jupiter
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
}

tasks.test {
    useJUnitPlatform()
}

// Build native image with :jpackage
runtime {
    options.addAll("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages")

    launcher {
        noConsole = true
    }

    jpackage {
        val currentOs = org.gradle.internal.os.OperatingSystem.current()
        val imgType = when {
            currentOs.isWindows -> "ico"
            currentOs.isMacOsX -> "icns"
            else -> "png"
        }
        imageOptions.addAll(listOf("--icon", "src/main/resources/co/raring/telnetqueue/zenner.$imgType"))
        installerOptions.addAll(listOf("--resource-dir", "src/main/resources", "--vendor", "TelnetQueue Group"))

        when {
            currentOs.isWindows -> installerOptions.addAll(listOf("--win-per-user-install", "--win-dir-chooser", "--win-menu", "--win-shortcut"))
            currentOs.isLinux -> installerOptions.addAll(listOf("--linux-package-name", "TelnetQueue", "--linux-shortcut"))
            currentOs.isMacOsX -> installerOptions.addAll(listOf("--mac-package-name", "TelnetQueue"))
        }
    }
}