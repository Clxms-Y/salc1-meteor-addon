plugins {
    id("fabric-loom") version "1.10-SNAPSHOT"
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

val archivesBaseName = project.extra["archives_base_name"]
val version = project.extra["mod_version"]
val group = project.extra["maven_group"]

repositories {
    maven {
        name = "Meteor Dev Releases"
        url = uri("https://maven.meteordev.org/releases")
    }
    maven {
        name = "Meteor Dev Snapshots"
        url = uri("https://maven.meteordev.org/snapshots")
    }
}

dependencies {
    // Fabric
    "minecraft"("com.mojang:minecraft:${project.extra["minecraft_version"]}")
    "mappings"("net.fabricmc:yarn:${project.extra["yarn_mappings"]}:v2")
    "modImplementation"("net.fabricmc:fabric-loader:${project.extra["loader_version"]}")
    // Meteor
    "modImplementation"("meteordevelopment:meteor-client:${project.extra["minecraft_version"]}-SNAPSHOT")
}

tasks {
    processResources {
        filteringCharset = "UTF-8"
        inputs.properties(project.properties)
        from(sourceSets.main.get().resources) {
            filesMatching("fabric.mod.json") {
                expand(mutableMapOf(
                    "version" to project.version,
                    "mc_version" to project.extra["minecraft_version"]
                ))
            }
        }
    }

    withType<JavaCompile> {
        options.apply {
            encoding = "UTF-8"
            release.set(21)
            compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
        }
    }
}
