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
  versionCode = 110
  versionName = "1.0.10-safe"
 }
 // Stable debug signing for CI. Never commit a keystore or passwords.
 val keystorePath = System.getenv("JOHNA_KEYSTORE_PATH")
 val keystorePassword = System.getenv("JOHNA_SIGNING_PASSWORD")
 if (!keystorePath.isNullOrBlank() && !keystorePassword.isNullOrBlank()) {
  signingConfigs {
   create("johnaCi") {
    storeFile = file(keystorePath)
    storePassword = keystorePassword
    keyAlias = "johna"
    keyPassword = keystorePassword
    storeType = "pkcs12"
   }
  }
  buildTypes {
   getByName("debug") { signingConfig = signingConfigs.getByName("johnaCi") }
  }
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
 debugImplementation("androidx.compose.ui:ui-tooling")
}
