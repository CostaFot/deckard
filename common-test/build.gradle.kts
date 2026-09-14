plugins {
    id("library.common")
    id("hilt.common")
}

android {
    namespace = "com.costafotiadis.common.test"
}

dependencies {
    implementation(project(":common"))
    implementation(project(":testing"))
}
