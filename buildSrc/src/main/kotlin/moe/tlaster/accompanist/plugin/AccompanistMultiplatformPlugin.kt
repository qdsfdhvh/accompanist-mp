package moe.tlaster.accompanist.plugin

import com.android.build.gradle.LibraryExtension
import java.util.Properties
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class AccompanistMultiplatformPlugin : Plugin<Project> {
    private val Project.ext get() = extensions.findByType<ExtraPropertiesExtension>()!!
    private val Project.android get() = extensions.findByType<LibraryExtension>()!!
    private val Project.kmp get() = extensions.findByType<KotlinMultiplatformExtension>()!!
    private val Project.publish get() = extensions.findByType<PublishingExtension>()!!
    private val Project.sign get() = extensions.findByType<SigningExtension>()!!
    override fun apply(target: Project) {
        with(target) {
            group = moe.tlaster.accompanist.plugin.Package.group
            version = moe.tlaster.accompanist.plugin.Package.versionName
            extConfig()
            applyPlugins()
            androidConfig()
            kmpConfig()
            publishConfig()
        }
    }

    private fun Project.extConfig() {
        with(ext) {
            val publishPropFile = rootProject.file("publish.properties")
            if (publishPropFile.exists()) {
                Properties().apply {
                    load(publishPropFile.inputStream())
                }.forEach { name, value ->
                    if (name == "signing.secretKeyRingFile") {
                        set(name.toString(), rootProject.file(value.toString()).absolutePath)
                    } else {
                        set(name.toString(), value)
                    }
                }
            } else {
                set("signing.keyId", System.getenv("SIGNING_KEY_ID"))
                set("signing.password", System.getenv("SIGNING_PASSWORD"))
                set("signing.secretKeyRingFile", System.getenv("SIGNING_SECRET_KEY_RING_FILE"))
                set("ossrhUsername", System.getenv("OSSRH_USERNAME"))
                set("ossrhPassword", System.getenv("OSSRH_PASSWORD"))
            }
        }
    }

    private fun Project.publishConfig() {
        with(publish) {
            if (rootProject.file("publish.properties").exists()) {
                with(sign) {
                    sign(publications)
                }
                repositories {
                    maven {
                        val releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                        val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                        url = if (version.toString().endsWith("SNAPSHOT")) {
                            uri(snapshotsRepoUrl)
                        } else {
                            uri(releasesRepoUrl)
                        }
                        credentials {
                            username = project.ext.get("ossrhUsername").toString()
                            password = project.ext.get("ossrhPassword").toString()
                        }
                    }
                }
            }
            val javadocJar: TaskProvider<Jar> =
                    tasks.register("javadocJar", org.gradle.api.tasks.bundling.Jar::class.java) {
                        archiveClassifier.set("javadoc")
                    }
            publications.withType<MavenPublication>() {
                artifact(javadocJar)
                pom {
                    artifactId = findProperty("POM_ARTIFACT_ID") as String
                    name.set(findProperty("POM_NAME") as String)
                    description.set(findProperty("POM_DESCRIPTION") as String)
                    url.set("https://github.com/Tlaster/accompanist-mp")

                    licenses {
                        license {
                            name.set("Apache-2.0")
                            url.set("https://opensource.org/licenses/Apache-2.0")
                        }
                    }

                    scm {
                        url.set("https://github.com/Tlaster/accompanist-mp")
                        connection.set("scm:git:git://github.com/Tlaster/accompanist-mp.git")
                        developerConnection.set("scm:git:git://github.com/Tlaster/accompanist-mp.git")
                    }
                }
            }
        }
    }

    private fun Project.kmpConfig() {
        with(kmp) {
            macosX64()
            macosArm64()
            ios("uikit")
            android {
                publishLibraryVariants("release", "debug")
            }
            jvm {
                compilations.all {
                    kotlinOptions.jvmTarget = Versions.Java.jvmTarget
                }
                testRuns.getByName("test").executionTask.configure {
                    useJUnitPlatform()
                }
            }
        }
    }


    private fun Project.androidConfig() {
        with(android) {
            compileSdk = Versions.Android.compile
            buildToolsVersion = Versions.Android.buildTools
            namespace = findProperty("ANDROID_NAMESPACE") as String
            defaultConfig {
                minSdk = Versions.Android.min
                targetSdk = Versions.Android.target
            }
            compileOptions {
                sourceCompatibility = Versions.Java.java
                targetCompatibility = Versions.Java.java
            }
        }
    }

    private fun Project.applyPlugins() {
        with(plugins) {
            apply("kotlin-multiplatform")
            apply("org.jetbrains.compose")
            apply("com.android.library")
            apply("maven-publish")
            apply("signing")
        }
    }
}