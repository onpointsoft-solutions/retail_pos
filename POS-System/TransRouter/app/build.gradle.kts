plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.onpointinfo.transrouter"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.onpointinfo.transrouter"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Compatibility task for tools/IDE versions expecting older AGP task names
tasks.register("unitTestClasses") {
    dependsOn(tasks.matching { it.name.contains("UnitTest") && (it.name.endsWith("Sources") || it.name.contains("JavaWithJavac") || it.name.contains("Kotlin")) })
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}