plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false

    // Scegli UNA modalità per il Secrets plugin: qui usiamo l'id esplicito
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false

    // KSP dichiarato UNA sola volta a livello root
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false
}
