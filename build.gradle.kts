plugins {
    id("fabric-loom") version "1.17.20"
    java
}

version = "0.1.0"
group = "dev.arnagpal.netherorder"

base {
    archivesName.set("nether-order")
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
}

dependencies {
    minecraft("com.mojang:minecraft:1.16.1")
    mappings("net.fabricmc:yarn:1.16.1+build.21:v2")
    modImplementation("net.fabricmc:fabric-loader:0.16.9")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand(mutableMapOf("version" to project.version))
    }
}
