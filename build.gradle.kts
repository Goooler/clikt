import org.jetbrains.dokka.gradle.DokkaTask
import java.io.ByteArrayOutputStream


plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.publish) apply false
}

fun getPublishVersion(): String {
    val versionName = project.property("VERSION_NAME") as String
    // Call gradle with -PinferVersion to set the dynamic version name.
    // Otherwise, we skip it to save time.
    if (!project.hasProperty("inferVersion")) return versionName

    val stdout = ByteArrayOutputStream()
    project.exec {
        commandLine = listOf("git", "tag", "--points-at", "HEAD")
        standardOutput = stdout
    }
    val tag = String(stdout.toByteArray()).trim()
    if (tag.isNotEmpty()) return tag

    val buildNumber = System.getenv("GITHUB_RUN_NUMBER") ?: "0"
    return "$versionName.$buildNumber-SNAPSHOT"
}



subprojects {
    project.setProperty("VERSION_NAME", getPublishVersion())

    plugins.withType<JavaBasePlugin>().configureEach {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion = JavaLanguageVersion.of(8)
        }
    }

    pluginManager.withPlugin("com.vanniktech.maven.publish") {
        apply(plugin = "org.jetbrains.dokka")

        tasks.named<DokkaTask>("dokkaHtml") {
            outputDirectory = rootProject.rootDir.resolve("docs/api")
            val rootPath = rootProject.rootDir.toPath()
            val logoCss = rootPath.resolve("docs/css/logo-styles.css").toString().replace('\\', '/')
            val paletteSvg = rootPath.resolve("docs/img/wordmark_small_dark.svg").toString()
                .replace('\\', '/')
            pluginsMapConfiguration = mapOf(
                    "org.jetbrains.dokka.base.DokkaBase" to """{
                "customStyleSheets": ["$logoCss"],
                "customAssets": ["$paletteSvg"],
                "footerMessage": "Copyright &copy; 2021 AJ Alt"
            }"""
                    )
            dokkaSourceSets.configureEach {
                reportUndocumented = false
                skipDeprecated = true
            }
        }
    }
}
