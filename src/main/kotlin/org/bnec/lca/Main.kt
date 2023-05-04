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

private val config = object {}.javaClass.classLoader
    .getResourceAsStream("config.json")?.use { inputStream -> 
        mapper.readValue(inputStream, Config::class.java)
    } 
    ?: throw Error("Config could not be read")

fun main() {
    val data = when (System.getenv("DATASOURCE")) {
        "mysql" -> setupDbConnection().getOrThrow().let { Database.connect(it) }.let { KtormData(memberNimSet, it) }
        else -> InMemoryData(memberNimSet)
    }
    Lca.init(config, data).block()
}