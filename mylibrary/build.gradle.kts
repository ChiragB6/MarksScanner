plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.tencent.ncnn"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    // 👇 VERY IMPORTANT for JNI module
    externalNativeBuild {
        cmake {
            path = file("src/main/jni/CMakeLists.txt")
        }
    }

    sourceSets {
        getByName("main") {
            // jniLibs folder where .so files exist
            jniLibs.srcDirs("src/main/jnilibs")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // Only core — NO UI libraries!
    implementation("androidx.core:core-ktx:1.12.0")
}
