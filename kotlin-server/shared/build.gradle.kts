plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20"
}

group = "com.avrahamChat"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.5")

    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:5.0.0")
    implementation("org.litote.kmongo:kmongo-coroutine:4.11.0")

    // RabbitMQ
    dependencies {
        api("com.rabbitmq:amqp-client:5.21.0")
    }
}