package org.bnec.lca

import discord4j.common.util.Snowflake
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.bnec.lca.data.InMemoryData
import org.bnec.lca.data.KtormDataPartial
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
    val inMemoryData = InMemoryData(
        memberNimSet, 
        configFile.classSelections.mapValues { it.value.toList() }
    )
    val data = when (System.getenv("DATASOURCE")?.lowercase()) {
        "mysql" -> setupDbConnection().getOrThrow()
            .let { connectionSource -> Database.connect(connectionSource) }
            .let { db -> KtormDataPartial(inMemoryData, db) }
        else -> inMemoryData
    }
    val config = LcaConfig(
        botToken = configFile.botToken ?: (System.getenv("TOKEN") ?: throw Error("Bot token could not be found")),
        memberRoleId = Snowflake.of(configFile.memberRoleId),
        classRoles = configFile.classRoles
            .mapKeys { entry -> entry.key.toInt() }
            .mapValues { entry -> Snowflake.of(entry.value) }
    )
    Lca.init(config, data).block()
}