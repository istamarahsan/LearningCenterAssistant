package org.bnec.lca

import discord4j.core.DiscordClient
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.rest.RestClient
import org.bnec.lca.commands.Verify
import org.bnec.lca.data.MemberData
import reactor.core.publisher.Mono

object Lca {
  
  fun init(config: Config, memberData: MemberData): Mono<Void> =
    arrayOf(Verify(config.memberRoleId, memberData)).associateBy { cmd -> cmd.signature().name() }.let { commands ->
      DiscordClient.create(config.botToken).withGateway { gateway ->
        registerCommands(gateway.restClient, commands.values.map { cmd -> cmd.signature() }).and(
          gateway.on(ChatInputInteractionEvent::class.java) { event ->
            (commands[event.commandName]?.handle(event) ?: Mono.empty()).then()
          }
        )
      }
    }
    
  
  private fun registerCommands(client: RestClient, requests: List<ApplicationCommandRequest>): Mono<Void> =
    client.applicationId.flatMapMany { appId ->
      client.applicationService.bulkOverwriteGlobalApplicationCommand(appId, requests)
    }.then()
}
