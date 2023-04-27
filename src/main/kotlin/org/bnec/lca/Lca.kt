package org.bnec.lca

import arrow.core.*
import com.fasterxml.jackson.databind.ObjectMapper
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.discordjson.json.ApplicationCommandOptionData
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.rest.RestClient
import org.apache.commons.dbcp2.BasicDataSource
import org.bnec.util.asOption
import org.ktorm.database.Database
import org.ktorm.dsl.insert
import reactor.core.publisher.Mono
import kotlin.io.path.Path

sealed class VerifyNimError {
    object DiscordCommandError : VerifyNimError()
    object DataAccessError : VerifyNimError()
    class NimNotFound(val nim: String) : VerifyNimError()
    object AddRoleError : VerifyNimError()
    object AlreadyVerified : VerifyNimError()
}

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

val memberNimSet = java.nio.file.Files.readAllBytes(Path("src/main/resources/members.json")).let {
    String(it)
}.let {
    ObjectMapper().readValue(it, Array<String>::class.java).toSet()
}

val config = java.nio.file.Files.readAllBytes(Path("src/main/resources/config.json")).let {
    String(it)
}.let {
    ObjectMapper().readValue(it, Config::class.java)
}

fun handleVerify(command: ChatInputInteractionEvent): Mono<Void> =
        command.deferReply().withEphemeral(true).then(Mono.fromSupplier {
            command.getOption("nim").asOption().flatMap {
                it.value.asOption()
            }.toEither { VerifyNimError.DiscordCommandError }.map { it.asString() }.flatMap {
                if (memberNimSet.contains(it)) it.right()
                else VerifyNimError.NimNotFound(it).left()
            }.fold(ifRight = { nim ->
                db.runCatching {
                    insert(MemberDiscordIds) {
                        set(it.discordUserId, command.interaction.user.id.asString())
                        set(it.nim, nim)
                    }
                }.fold(onSuccess = { if (it > 0) none() else VerifyNimError.AlreadyVerified.toOption() },
                        onFailure = { _ -> VerifyNimError.DataAccessError.toOption() })
            }, ifLeft = { it.toOption() })
        }.flatMap { err ->
            err.fold(ifEmpty = {
                command.interaction.guildId.asOption().toEither { VerifyNimError.AddRoleError }
            }, ifSome = { it.left() }).fold(ifRight = { guildId ->
                command.interaction.user.asMember(guildId).map { it.right() }
            }, ifLeft = { Mono.just(it.left()) })
        }.map { result ->
            result.fold(ifRight = { member ->
                // this stream is negative: only emits on error
                member.addRole(Snowflake.of(config.memberRoleId)).subscribe()
                return@map none<VerifyNimError>()
            }, ifLeft = { it.toOption() })
        }.flatMap { err ->
            err.fold(ifEmpty = {
                command.createFollowup().withContent("Welcome, Extraordinary!")
            }, ifSome = {
                command.createFollowup().withContent(viewErrorMessageForVerifyNimError(it))
            })
        }.then())

fun viewErrorMessageForVerifyNimError(err: VerifyNimError): String = when (err) {
    is VerifyNimError.AddRoleError -> "Hmm, we weren't able to give you the right access. Please inform the staff of this technical issue."
    is VerifyNimError.DataAccessError -> "Sorry, we had issues accessing our data. Please notify staff of this issue."
    is VerifyNimError.DiscordCommandError -> "Something went wrong with discord's slash commands. Please try again later."
    is VerifyNimError.NimNotFound -> "We couldn't find NIM: ${err.nim}. If this is a mistake, please contact staff."
    is VerifyNimError.AlreadyVerified -> "It seems that you're already verified."
}

fun registerCommands(client: RestClient) {
    val appId = client.applicationId.block() ?: throw Exception("Could not connect to register commands")
    val verifyRequest =
            ApplicationCommandRequest.builder().name("verify").description("verify with your NIM").addOption(
                    ApplicationCommandOptionData.builder().name("nim").description("Your NIM")
                            .type(ApplicationCommandOption.Type.STRING.value).required(true).build()
            ).build()
    client.applicationService.bulkOverwriteGlobalApplicationCommand(appId, listOf(verifyRequest)).subscribe()
}