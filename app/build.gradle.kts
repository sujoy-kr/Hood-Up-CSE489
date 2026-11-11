plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

// Parse dotenv without logging credentials; quoted values may contain '='.
val envFile = rootProject.file(".env")
val envValues = if (envFile.isFile) envFile.readLines().mapNotNull { raw ->
    val line = raw.trim().removePrefix("export ")
    val separator = line.indexOf('=')
    if (line.startsWith("#") || separator <= 0) null else {
        val key = line.substring(0, separator).trim()
        var value = line.substring(separator + 1).trim()
        if (value.length >= 2 && ((value.first() == '\"' && value.last() == '\"') ||
                    (value.first() == '\'' && value.last() == '\''))) {
            value = value.substring(1, value.length - 1)
        } else {
            value = value.substringBefore(" #").trimEnd()
        }
        key to value
    }
}.toMap() else emptyMap()
fun buildConfigString(value: String): String = "\"" + value
    .replace("\\", "\\\\").replace("\"", "\\\"")
    .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\""

android {
    namespace = "com.example.carworkshopbookingapp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.carworkshopbookingapp"
        minSdk = 24
        targetSdk = 37
        buildConfigField("String", "GEMINI_API_KEY", buildConfigString(envValues["GEMINI_API_KEY"].orEmpty()))
        buildConfigField("String", "ADMIN_INVITE_CODE", buildConfigString(envValues["ADMIN_INVITE_CODE"].orEmpty()))
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures { buildConfig = true }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    testImplementation(libs.junit)
    // JVM request-contract tests need real org.json rather than Android mock stubs.
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    implementation(platform("com.google.firebase:firebase-bom:35.1.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
}
