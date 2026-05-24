plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
}

val publishVersion = providers.gradleProperty("VERSION_NAME")
    .orElse("1.0.1-SNAPSHOT")
    .get()

android {
    namespace = "com.localflow.sdk"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"

        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
        )
    }

    buildFeatures {
        compose = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.process)

    // OkHttp & Serialization
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Jetpack Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.github.tiofani03"
                artifactId = "localflow-sdk"
                version = publishVersion

                pom {
                    name.set("LocalFlow SDK")
                    description.set("Android SDK for LocalFlow localization management")
                    url.set("https://github.com/tiofani03/localflow-android")

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }

                    developers {
                        developer {
                            id.set("tiofani03")
                            name.set("Tio Fani")
                            url.set("https://github.com/tiofani03")
                        }
                    }

                    scm {
                        connection.set("scm:git:github.com/tiofani03/localflow-android.git")
                        developerConnection.set("scm:git:ssh://github.com/tiofani03/localflow-android.git")
                        url.set("https://github.com/tiofani03/localflow-android")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "GitHubPackages"

                val githubRepo = System.getenv("GITHUB_REPOSITORY")
                    ?: "tiofani03/localflow-android"

                url = uri("https://maven.pkg.github.com/$githubRepo")

                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                        ?: project.findProperty("gpr.user") as String?
                                ?: ""

                    password = System.getenv("GITHUB_TOKEN")
                        ?: project.findProperty("gpr.key") as String?
                                ?: ""
                }
            }
        }
    }
}