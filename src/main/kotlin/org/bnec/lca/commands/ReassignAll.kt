package org.bnec.lca.commands

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.entity.Member
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.discordjson.json.ImmutableApplicationCommandRequest
import reactor.core.publisher.Mono

object ReassignAll: SlashCommand {
  override fun signature(): ImmutableApplicationCommandRequest =
    ApplicationCommandRequest.builder()
      .name("reassignAll")
      .description("assign class roles to verified members (overwrites)")
      .build()
  override fun handle(command: ChatInputInteractionEvent): Mono<Void> =
    command.deferReply().withEphemeral(false).and {
      command.interaction
        .guild
        .flatMapMany { guild -> guild.members }
        .filter(this::guildMemberHasClassAccess)
        .flatMap(this::processRoleReassignment)
    }

  // TODO
  private fun guildMemberHasClassAccess(member: Member): Boolean =
    false 

  private fun processRoleReassignment(member: Member): Mono<Void> =
    removeAllClassRoles(member).then(addClassRoles(member))

  // TODO
  private fun removeAllClassRoles(member: Member): Mono<Void> =
    Mono.empty<Void>().then()

  // TODO
  private fun addClassRoles(member: Member): Mono<Void> =
    Mono.empty<Void>().then()
}