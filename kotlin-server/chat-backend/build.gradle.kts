plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20"
    id("io.ktor.plugin") version "2.3.5"
}

group = "com.avrahamChat"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":shared"))

    // Ktor Server Core & Plugins
    implementation("io.ktor:ktor-server-core-jvm:2.3.5")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.5")
    implementation("io.ktor:ktor-server-websockets-jvm:2.3.5")
    implementation("io.ktor:ktor-server-cors-jvm:2.3.5")
    implementation("io.ktor:ktor-server-call-logging-jvm:2.3.5")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.5")

    // Ktor Client
    implementation("io.ktor:ktor-client-core-jvm:2.3.13")
    implementation("io.ktor:ktor-client-cio-jvm:2.3.5")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:2.3.5")

    // Serialization
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.5")

    // Database (MongoDB)
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:5.0.0")
    implementation("org.litote.kmongo:kmongo-coroutine:4.11.0")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // RabbitMQ
    implementation("com.rabbitmq:amqp-client:5.19.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
}

application {
    mainClass.set("com.avrahamChat.ApplicationKt")
}