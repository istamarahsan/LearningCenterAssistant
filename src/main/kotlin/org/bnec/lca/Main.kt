package org.bnec.lca

import com.fasterxml.jackson.databind.ObjectMapper
import org.bnec.lca.data.InMemoryData
import org.bnec.lca.data.KtormData
import org.ktorm.database.Database

val mapper = ObjectMapper()

private val memberNimSet = object {}.javaClass.classLoader
    .getResourceAsStream("members.json")?.use { inputStream ->
        mapper.readValue(inputStream, Array<String>::class.java)
    }
    ?.toSet()
    ?: throw Error("member nim list could not be read")

private val configFile = object {}.javaClass.classLoader
    .getResourceAsStream("config.json")?.use { inputStream -> 
        mapper.readValue(inputStream, ConfigFile::class.java)
    } 
    ?: throw Error("Config could not be read")

fun main() {
    val data = when (System.getenv("DATASOURCE")?.lowercase()) {
        "mysql" -> setupDbConnection().getOrThrow().let { Database.connect(it) }.let { KtormData(memberNimSet, it) }
        else -> InMemoryData(memberNimSet)
    }
    val config = LcaConfig(
        botToken = if (configFile.botToken != "") configFile.botToken else System.getenv("TOKEN") ?: throw Error("Bot token could not be found"),
        memberRoleId = configFile.memberRoleId
    )
    Lca.init(config, data).block()
}