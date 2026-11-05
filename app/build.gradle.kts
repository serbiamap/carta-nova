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
        versionCode = 3
        versionName = "1.1"
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
    implementation("com.google.android.gms:play-services-location:21.4.0")
}
