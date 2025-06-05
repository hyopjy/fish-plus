plugins {
    val kotlinVersion = "1.7.20"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

//    id("net.mamoe.mirai-console") version "2.13.2"
    id("net.mamoe.mirai-console") version "2.16.0"
}

group = "fish.plus"
version = "0.1.0"

repositories {
    if (System.getenv("CI")?.toBoolean() != true) {
        maven("https://maven.aliyun.com/repository/public") // 阿里云国内代理仓库
    }
    maven("https://central.sonatype.com/repository/maven-snapshots/")
    mavenCentral()
}

mirai {
    noTestCore = true
    setupConsoleTestRuntime {
        // 移除 mirai-core 依赖
        classpath = classpath.filter {
            !it.nameWithoutExtension.startsWith("mirai-core-jvm")
        }
    }
    jvmTarget = JavaVersion.VERSION_17
}
dependencies {
    // 若需要使用 Overflow 的接口，请取消注释下面这行
    // compileOnly("top.mrxiaom:overflow-core-api:$VERSION")
// https://mvnrepository.com/artifact/org.eclipse.paho/org.eclipse.paho.client.mqttv3
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    implementation( "org.projectlombok:lombok:1.18.24")
//    annotationProcessor group: 'org.projectlombok', name: 'lombok', version: '1.18.24'
    annotationProcessor("org.projectlombok","lombok","1.18.24")
    implementation("cn.hutool:hutool-all:5.8.10")
    implementation("org.apache.commons:commons-lang3:3.8.1")
    implementation("cn.chahuyun:hibernate-plus:1.0.16")
    testConsoleRuntime("top.mrxiaom.mirai:overflow-core:1.0.5")



}