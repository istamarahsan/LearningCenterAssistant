package org.bnec.lca.commands

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.discordjson.json.ImmutableApplicationCommandRequest
import reactor.core.publisher.Mono

interface SlashCommand {
  fun signature(): ImmutableApplicationCommandRequest
  fun handle(command: ChatInputInteractionEvent): Mono<Void>
}