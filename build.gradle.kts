import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.binary.compatibility.validator)
    `maven-publish`
    signing
    jacoco
}

group = "io.github.eduardo3677-ai"

tasks {
    processResources {
        expand("projectVersion" to project.version)
    }

    withType<Jar> {
        from(rootProject.file("LICENSE"))
        from(rootProject.file("NOTICE"))
    }

    test {
        useJUnitPlatform()
        testLogging {
            events("PASSED", "SKIPPED", "FAILED")
        }
        finalizedBy(jacocoTestReport)
        testLogging { exceptionFormat = TestExceptionFormat.FULL }
    }

    jacocoTestReport {
        dependsOn(test)
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
    // Obtain baksmali/smali from source builds - https://github.com/iBotPeaches/smali
    // Remove when official smali releases come out again.
    maven {
        url = uri("https://jitpack.io")
        content {
            includeGroup("com.github.MorpheApp.smali")
            includeGroup("com.github.REAndroid")
        }
    }
    maven {
        url = uri("https://maven.pkg.github.com/eduardo3677-ai/registry")
        credentials {
            username = System.getenv("GITHUB_ACTOR") ?: ""
            password = System.getenv("GITHUB_TOKEN") ?: ""
        }
    }
}

dependencies {
    compileOnly(libs.android) {
        // Exclude, otherwise the org.w3c.dom API breaks.
        exclude(group = "xerces", module = "xmlParserAPIs")
    }

    implementation(libs.bcpkix.jdk18on)
    implementation(libs.apksig)
    implementation(libs.apkzlib)
    implementation(libs.arsclib)
    implementation(libs.guava)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.smali)

    testImplementation(libs.mockk)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter.params)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)

        freeCompilerArgs = listOf("-Xcontext-receivers")
    }
}

tasks.withType<Test> {
    // Allow running the test suite against a specific JDK (e.g. -PtestJavaVersion=11)
    // while Gradle itself keeps running on JDK 17+. Used by CI to verify the library
    // works across the JDK versions it supports.
    providers.gradleProperty("testJavaVersion").orNull?.let { testJavaVersion ->
        javaLauncher.set(
            javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(testJavaVersion.toInt()))
            },
        )
    }

    testLogging {
        // Uncomment to show println and exception stack traces in unit tests.
        // showStandardStreams = true
    }
}


java {
    targetCompatibility = JavaVersion.VERSION_11

    withSourcesJar()
}

publishing {
    repositories {
        maven {
            name = "MavenCentral"
            url = uri(
                if (project.version.toString().endsWith("-SNAPSHOT")) {
                    "https://central.sonatype.com/repository/maven-snapshots/"
                } else {
                    "https://central.sonatype.com/api/v1/publisher/deploy/"
                }
            )
            credentials {
                username = System.getenv("MAVEN_CENTRAL_USERNAME") ?: ""
                password = System.getenv("MAVEN_CENTRAL_PASSWORD") ?: ""
            }
        }
    }

    publications {
        create<MavenPublication>("morphe-patcher-publication") {
            from(components["java"])

            version = project.version.toString()
            groupId = "io.github.eduardo3677-ai"
            artifactId = "morphe-patcher"

            pom {
                name = "Morphe Patcher"
                description = "Patcher used by Morphe."
                url = "https://github.com/eduardo3677-ai/morphe-patcher"

                licenses {
                    license {
                        name = "GNU General Public License v3.0"
                        url = "https://www.gnu.org/licenses/gpl-3.0.en.html"
                    }
                }
                developers {
                    developer {
                        id = "eduardo3677-ai"
                        name = "eduardo3677-ai"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/eduardo3677-ai/morphe-patcher.git"
                    developerConnection = "scm:git:git@github.com:eduardo3677-ai/morphe-patcher.git"
                    url = "https://github.com/eduardo3677-ai/morphe-patcher"
                }
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["morphe-patcher-publication"])
}
