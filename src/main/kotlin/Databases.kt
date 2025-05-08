package com.bookintok

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database

fun Application.configureDatabases() {
    val database = Database.connect(
        url = "jdbc:mysql://localhost:3306/Bookintok",
        user = "josaca",
        driver = "com.mysql.cj.jdbc.Driver",
        password = "85211",
    )
    println("\u001B[38;2;152;246;160mBase de datos configurada correctamente\u001B[0m\n")
}