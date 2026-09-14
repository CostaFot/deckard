plugins {
    id("library.common")
}

android {
    namespace = "com.costafotiadis.logging"
}

dependencies {
    api(libs.timber)
}
