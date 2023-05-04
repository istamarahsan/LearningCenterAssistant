package org.bnec.lca

import com.mysql.cj.jdbc.Driver
import org.apache.commons.dbcp2.BasicDataSource
import javax.sql.DataSource

data class MySqlConnectionDetails(
  val host: String, val port: Int, val user: String, val password: String, val database: String
)

fun setupDbConnection(): Result<DataSource> =
  runCatching {
    Driver()
    val conn = MySqlConnectionDetails(
      host = System.getenv("MYSQLHOST"),
      port = System.getenv("MYSQLPORT").toInt(),
      user = System.getenv("MYSQLUSER"),
      password = System.getenv("MYSQLPASSWORD"),
      database = System.getenv("MYSQLDATABASE")
    )
    val connString = "jdbc:mysql://${conn.host}:${conn.port}/${conn.database}"
    BasicDataSource().apply {
      driverClassName = com.mysql.cj.jdbc.Driver::class.java.name
      url = connString
      username = conn.user
      password = conn.password
    }
  }
