package com.avrahamChat.config

object AppConfig {
    val HTTP_PORT = System.getenv("PORT")?.toInt() ?: 8081

    val SIGNON_PORT = System.getenv("SIGNON_PORT")?.toInt() ?: 8082

    val TCP_PORT = HTTP_PORT + 1000

    val SIGNON_URL = System.getenv("SIGNON_URL") ?: "http://localhost:$SIGNON_PORT"

    val MONGO_URI = System.getenv("MONGO_URI") ?: "mongodb://localhost:27017"
    val RABBIT_HOST = System.getenv("RABBIT_HOST") ?: "localhost"

    val MY_HOST = System.getenv("MY_HOST") ?: "localhost"
    val MY_FULL_ADDRESS = "http://$MY_HOST:$HTTP_PORT"
}