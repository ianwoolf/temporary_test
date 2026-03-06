pluginManagement {
    repositories {
        // 1. 优先走阿里插件镜像
        maven { url = uri("https://maven.aliyun.com") }
        maven { url = uri("https://maven.aliyun.com") }
        // 2. 官方源放最后保底
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "testflow-generator"

