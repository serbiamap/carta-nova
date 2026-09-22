plugins {
    id("com.android.application")
}

android {
    namespace = "net.serbiamap.cartanova"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.serbiamap.cartanova"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation("androidx.webkit:webkit:1.17.0")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
