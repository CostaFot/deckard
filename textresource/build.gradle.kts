plugins {
    id("library.common")
    id("library.compose.common")
}

android {
    namespace = "com.costafotiadis.textresource"
}

dependencies {
    api(libs.androidx.compose.runtime)
    // Also api-exposes androidx.annotation, which is where @StringRes comes from.
    api(libs.androidx.ui)

    testImplementation(libs.mockk)
}
