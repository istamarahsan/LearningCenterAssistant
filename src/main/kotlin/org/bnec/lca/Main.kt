package org.bnec.lca

import com.fasterxml.jackson.databind.ObjectMapper
import com.mysql.cj.jdbc.Driver
import org.apache.commons.dbcp2.BasicDataSource
import org.bnec.lca.data.DataImpl
import org.ktorm.database.Database

data class MySqlCredentials(
    val host: String, val port: Int, val user: String, val password: String, val database: String
)

private val conn = MySqlCredentials(
    host = System.getenv("MYSQLHOST"),
    port = System.getenv("MYSQLPORT").toInt(),
    user = System.getenv("MYSQLUSER"),
    password = System.getenv("MYSQLPASSWORD"),
    database = System.getenv("MYSQLDATABASE")
)

private val connString = "jdbc:mysql://${conn.host}:${conn.port}/${conn.database}"

private val db = BasicDataSource().apply {
    driverClassName = com.mysql.cj.jdbc.Driver::class.java.name
    url = connString
    username = conn.user
    password = conn.password
}.let {
    Database.connect(it)
}

private val memberNimSet = object {}.javaClass
    .getResource("/members.json")
    ?.readText()?.let{ jsonString -> ObjectMapper().readValue(jsonString, Array<String>::class.java) }
    ?.toSet()
    ?: throw Error("member nim list could not be read")

private val config = object {}.javaClass
    .getResource("/config.json")
    ?.readText()
    ?.let { jsonString -> ObjectMapper().readValue(jsonString, Config::class.java) } 
    ?: throw Error("Config could not be read")

fun main() {
    Driver()
    Lca.init(config, DataImpl(memberNimSet, db)).block()
}