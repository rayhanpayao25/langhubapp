plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.IT4A.langhub"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.IT4A.langhub"
        minSdk = 21
        targetSdk = 34
        versionCode = 25
        versionName = "2.5"
        vectorDrawables.useSupportLibrary = true

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

dependencies {
    // Core dependencies
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Volley for network requests
    implementation("com.android.volley:volley:1.2.1")
    implementation ("com.belerweb:pinyin4j:2.5.0")

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation ("com.google.mlkit:text-recognition:16.0.1")
    implementation ("androidx.activity:activity-ktx:1.7.0")
    implementation ("com.google.mlkit:text-recognition-chinese:16.0.1")
    implementation ("com.google.mlkit:language-id:17.0.6")
    implementation ("androidx.core:core:1.9.0")

    // Gson Converter to convert JSON responses
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")

    // Gson for parsing the JSON responses
    implementation ("com.google.code.gson:gson:2.8.8")

    implementation("com.vanniktech:android-image-cropper:4.6.0")
    implementation ("com.github.bumptech.glide:glide:4.14.2")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.14.2")
    implementation ("com.caverock:androidsvg:1.4")
    implementation ("de.hdodenhof:circleimageview:3.1.0")
    implementation ("androidx.cardview:cardview:1.0.0")
    implementation ("com.google.android.material:material:1.9.0")

    // Firebase dependencies
    implementation ("com.google.firebase:firebase-firestore:24.4.1")
    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))
    implementation("com.google.firebase:firebase-analytics")

    // Retrofit and OkHttp dependencies
    implementation("com.github.kittinunf.fuel:fuel:3.0.0-alpha04")
    implementation("com.github.bumptech.glide:glide:4.14.2")
    annotationProcessor("com.github.bumptech.glide:compiler:4.14.2")
    implementation("com.squareup.picasso:picasso:2.71828")

}

