package org.bnec.lca

import com.fasterxml.jackson.databind.ObjectMapper
import com.mysql.cj.jdbc.Driver
import org.apache.commons.dbcp2.BasicDataSource
import org.ktorm.database.Database

data class MySqlCredentials(
    val host: String, val port: Int, val user: String, val password: String, val database: String
)

val conn = MySqlCredentials(
    host = System.getenv("MYSQLHOST"),
    port = System.getenv("MYSQLPORT").toInt(),
    user = System.getenv("MYSQLUSER"),
    password = System.getenv("MYSQLPASSWORD"),
    database = System.getenv("MYSQLDATABASE")
)

val connString = "jdbc:mysql://${conn.host}:${conn.port}/${conn.database}"

val db = BasicDataSource().apply {
    driverClassName = com.mysql.cj.jdbc.Driver::class.java.name
    url = connString
    username = conn.user
    password = conn.password
}.let {
    Database.connect(it)
}

val memberNimsJsonString = object {}.javaClass
    .getResource("/members.json")
    ?.readText()

val memberNimSet: Set<String> = ObjectMapper().readValue(memberNimsJsonString, Array<String>::class.java).toSet()

val config = object {}.javaClass
    .getResource("/config.json")
    ?.readText()
    ?.let { jsonString -> ObjectMapper().readValue(jsonString, Config::class.java) } 
    ?: throw Error("Config could not be read")

fun main() {
    Driver()
    val token = System.getenv("TOKEN") ?: throw Exception("Bot token not found")
    Lca.init(Config(token, "")).block()
}