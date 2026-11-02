plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.template.framework"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    // 公开 API 暴露的类型使用 api，使用者无需重复声明同一依赖。
    api(libs.androidx.appcompat)
    api(libs.androidx.recyclerview)
    // LoadingDialog 的 XML 引用 Material 控件，资源编译与宿主使用均需要此依赖。
    api(libs.androidx.material)

    api(libs.androidx.lifecycle.runtime.ktx)

    api(libs.androidx.datastore.preferences)

    api(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    api(libs.okhttp)
    api(libs.gson)

    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.timber)

    testImplementation(libs.junit)
}
