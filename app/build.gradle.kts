plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    signingConfigs {
        getByName("debug") {
            storeFile = file("../keystore.jks")
            storePassword = "qazwsx"
            keyPassword = "111111"
            keyAlias = "music"
        }

    }
    namespace = "com.ztftrue.music"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ztftrue.music"
        minSdk = 30
        targetSdk = 35
        versionCode = 49
        versionName = "0.1.49"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

    }
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = true
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            applicationVariants.all {
                outputs.all {
                    val variantOutput = this
                    if (variantOutput is com.android.build.gradle.internal.api.ApkVariantOutputImpl) {
                        tasks.named("mergeReleaseArtProfile").configure {
                            doLast {
                                copy {
                                    from("${layout.buildDirectory}/outputs/mapping/release/mapping.txt")
                                    into(variantOutput.outputFile.parent)
                                    rename { "mapping.txt" }
                                }
                            }
                        }
                    }
                }
            }
            signingConfig = signingConfigs.getByName("debug")

        }
        getByName("debug") {
            applicationIdSuffix = ".debug" // Appends ".debug" to the
//            isMinifyEnabled = true
//            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}
composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("stability_config.conf"))
}
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
dependencies {

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.navigation:navigation-compose:2.9.1")
    implementation("androidx.media3:media3-exoplayer:1.7.1")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.media:media:1.7.0")

    implementation("androidx.appcompat:appcompat:1.7.1")


    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.material3:material3:1.4.0-alpha17")
    implementation("androidx.compose.material3:material3-window-size-class:1.4.0-alpha17")

    // splash
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.1")

    // load image
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.browser:browser:1.8.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.06.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")


    implementation("androidx.collection:collection-ktx:1.5.0")
    implementation("androidx.fragment:fragment-ktx:1.8.8")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")
    implementation("androidx.palette:palette-ktx:1.0.0")

    // https://mvnrepository.com/artifact/org.apache.commons/commons-math3
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("com.google.code.gson:gson:2.13.1")

    val roomVersion = "2.7.2"
    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")
    // To use Kotlin Symbol Processing (KSP)
    ksp("androidx.room:room-compiler:$roomVersion")
    // optional - Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:$roomVersion")
    // optional - RxJava3 support for Room
    implementation("androidx.room:room-rxjava3:$roomVersion")
    // optional - Test helpers
    implementation("androidx.room:room-testing:$roomVersion")
    // optional - Paging 3 Integration
    implementation("androidx.room:room-paging:$roomVersion")


//    debugImplementation ("com.squareup.leakcanary:leakcanary-android:2.14")
//    implementation("uk.me.berndporr:iirj:1.7")
    // https://mvnrepository.com/artifact/com.github.wendykierp/JTransforms
//    implementation("com.github.wendykierp:JTransforms:3.1")
//    implementation("be.tarsos.dsp:core:2.5")
// https://mvnrepository.com/artifact/org.bitbucket.ijabz/jaudiotagger
//    implementation("net.jthink:jaudiotagger:3.0.1")

}
