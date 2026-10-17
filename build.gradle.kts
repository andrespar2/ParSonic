// Build raiz de ParSonic.
// Los plugins se declaran aqui con `apply false` y se aplican por modulo.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
