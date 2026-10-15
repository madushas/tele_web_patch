group = "app.teleweb.morphe-patches"

patches {
    about {
        name = "Telegram Web 10.2.0 Patches"
        description = "Patches for Telegram Web (org.telegram.messenger.web) 10.2.0."
        source = "https://github.com/tele-web/tele_web_patch"
        author = "tele-web"
        contact = "https://github.com/tele-web/tele_web_patch"
        website = "https://morphe.software/add-source?github=tele-web/tele_web_patch"
        license = "GPLv3"
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

val generatedSecretsDir = layout.buildDirectory.dir("generated/secrets/kotlin")

sourceSets {
    main {
        kotlin.srcDir(generatedSecretsDir)
    }
}

fun String.kotlinStringLiteral() = replace("\\", "\\\\").replace("\"", "\\\"")

val generateSecrets by tasks.registering {
    val sharedMapsApiKey = providers.environmentVariable("SHARED_MAPS_API_KEY")
    inputs.property("SHARED_MAPS_API_KEY", sharedMapsApiKey.orElse(""))
    outputs.dir(generatedSecretsDir)

    doLast {
        val outputDir = generatedSecretsDir.get().asFile.resolve("app/template/patches/shared")
        outputDir.mkdirs()
        outputDir.resolve("BuildSecrets.kt").writeText(
            """
            package app.template.patches.shared

            internal object BuildSecrets {
                const val SHARED_MAPS_API_KEY = "${sharedMapsApiKey.orNull.orEmpty().kotlinStringLiteral()}"
            }
            """.trimIndent(),
        )
    }
}

tasks.named("compileKotlin") {
    dependsOn(generateSecrets)
}

tasks.named("sourcesJar") {
    dependsOn(generateSecrets)
}

// Separate configuration so gson is available at runtime for the
// generatePatchesList task but never bundled into the APK.
val patchListGeneratorClasspath: Configuration by configurations.creating

dependencies {
    compileOnly(libs.gson)
    patchListGeneratorClasspath(libs.gson)
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("app.morphe.util.PatchListGeneratorKt")
    }

    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}
