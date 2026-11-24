plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val gdxVersion: String by rootProject.extra
val kotlinVersion: String by rootProject.extra

val natives: Configuration by configurations.creating

android {
    namespace = "com.ironbow.stickmanarcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ironbow.stickmanarcher"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        multiDexEnabled = true
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("libs")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
}

tasks.register("copyAndroidNatives") {
    doFirst {
        val abis = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        abis.forEach { abi ->
            file("libs/$abi").mkdirs()
        }
        natives.files.forEach { jar ->
            val targetAbi = abis.firstOrNull { jar.name.contains("natives-$it") }
            if (targetAbi != null) {
                copy {
                    from(zipTree(jar))
                    into(file("libs/$targetAbi"))
                    include("*.so")
                }
            }
        }
    }
}

tasks.matching { it.name.contains("package") || (it.name.contains("merge") && it.name.contains("JniLib")) }
    .configureEach {
        dependsOn("copyAndroidNatives")
    }
