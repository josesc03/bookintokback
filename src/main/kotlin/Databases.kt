package com.bookintok

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database

fun Application.configureDatabases() {
    val dbUser = System.getenv("DB_USER")
    val dbPassword = System.getenv("DB_PASSWORD")
    val dbUrl = System.getenv("DB_URL")
    val database = Database.connect(
        url = dbUrl,
        user = dbUser,
        driver = "com.mysql.cj.jdbc.Driver",
        password = dbPassword,
    )
    println("\u001B[38;2;152;246;160mBase de datos configurada correctamente\u001B[0m\n")
}