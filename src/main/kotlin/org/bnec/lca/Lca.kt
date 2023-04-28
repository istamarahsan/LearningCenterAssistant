package org.bnec.lca

import arrow.core.*
import com.fasterxml.jackson.databind.ObjectMapper
import discord4j.common.util.Snowflake
import discord4j.core.DiscordClient
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.discordjson.json.ApplicationCommandOptionData
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.rest.RestClient
import org.apache.commons.dbcp2.BasicDataSource
import org.bnec.lca.commands.ReassignAll
import org.bnec.lca.commands.Verify
import org.bnec.util.asOption
import org.ktorm.database.Database
import org.ktorm.dsl.insert
import reactor.core.publisher.Mono
import kotlin.io.path.Path

object Lca {
  
  private val commands = arrayOf(Verify).associateBy { cmd -> cmd.signature().name() }
  
  fun init(config: Config): Mono<Void> =
    DiscordClient.create(config.botToken).withGateway { gateway ->
      registerCommands(gateway.restClient, commands.values.map { cmd -> cmd.signature() }).and(
        gateway.on(ChatInputInteractionEvent::class.java) { event ->
          (commands[event.commandName]?.handle(event) ?: Mono.empty()).then()
        }
      )
    }
  
  private fun registerCommands(client: RestClient, requests: List<ApplicationCommandRequest>): Mono<Void> =
    client.applicationId.flatMapMany { appId ->
      client.applicationService.bulkOverwriteGlobalApplicationCommand(appId, requests)
    }.then()
}
