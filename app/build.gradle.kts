plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
 id("org.jetbrains.kotlin.plugin.compose")
}
android {
 namespace = "com.johna.assistant"
 compileSdk = 35
 defaultConfig {
  applicationId = "com.johna.assistant"
  minSdk = 26
  targetSdk = 35
  versionCode = 109
  versionName = "1.0.9"
 }
 buildFeatures { compose = true }
 compileOptions {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
 }
 kotlinOptions { jvmTarget = "17" }
 packaging {
  resources { excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE", "META-INF/NOTICE") }
 }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2024.09.03"))
 implementation("androidx.activity:activity-compose:1.9.3")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.foundation:foundation")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.ui:ui-tooling-preview")
 // Google Sign-In + OAuth2 token API (for Gmail / Calendar)
 implementation("com.google.android.gms:play-services-auth:21.2.0")
 debugImplementation("androidx.compose.ui:ui-tooling")
}
