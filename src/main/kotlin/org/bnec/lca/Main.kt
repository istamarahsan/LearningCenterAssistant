package org.bnec.lca

import com.mysql.cj.jdbc.Driver
import discord4j.core.DiscordClient
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import reactor.core.publisher.Mono

fun main() {
    Driver()

    val token = System.getenv("TOKEN") ?: throw Exception("Bot token not found")

    DiscordClient.create(token).withGateway { gateway ->
        val handle = gateway.on(ChatInputInteractionEvent::class.java) { command ->
            return@on (if (command.commandName == "verify") handleVerify(command)
            else Mono.empty())
        }.then()

        registerCommands(gateway.restClient)

        return@withGateway handle
    }.block()
}