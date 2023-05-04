package org.bnec.lca

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.bnec.lca.data.InMemoryData
import org.bnec.lca.data.KtormBnecData
import org.ktorm.database.Database

@OptIn(ExperimentalSerializationApi::class)
private val memberNimSet = object {}.javaClass.classLoader
    .getResourceAsStream("members.json")?.use { inputStream ->
        runCatching { Json.decodeFromStream<Array<String>>(inputStream) }.getOrNull()
    }
    ?.toSet()
    ?: throw Error("member nim list could not be read")

@OptIn(ExperimentalSerializationApi::class)
private val configFile = object {}.javaClass.classLoader
    .getResourceAsStream("config.json")?.use { inputStream -> 
        runCatching { Json.decodeFromStream<ConfigFile>(inputStream) }.getOrNull()
    } ?: throw Error("Config could not be read")

fun main() {
    val data = when (System.getenv("DATASOURCE")?.lowercase()) {
        "mysql" -> setupDbConnection().getOrThrow().let { Database.connect(it) }.let { KtormBnecData(memberNimSet, it) }
        else -> InMemoryData(memberNimSet)
    }
    val config = LcaConfig(
        botToken = configFile.botToken ?: (System.getenv("TOKEN") ?: throw Error("Bot token could not be found")),
        memberRoleId = configFile.memberRoleId
    )
    Lca.init(config, data).block()
}