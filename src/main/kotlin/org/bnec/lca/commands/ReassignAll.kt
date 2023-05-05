package org.bnec.lca.commands

import arrow.core.Either
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.entity.Member
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.discordjson.json.ImmutableApplicationCommandRequest
import org.bnec.lca.data.BnecData
import org.bnec.util.flatMapEither
import reactor.core.publisher.Mono

class ReassignAll(
  private val classRoles: Map<Int, Snowflake>,
  private val data: BnecData
) : SlashCommand {
  override fun signature(): ImmutableApplicationCommandRequest =
    ApplicationCommandRequest.builder()
      .name("reassignall")
      .description("assign class roles to verified members (overwrites)")
      .defaultPermission(false)
      .build()

  override fun handle(command: ChatInputInteractionEvent): Mono<Void> =
    command.deferReply().withEphemeral(true).then (
      command.interaction
        .guild
        .flatMapMany { guild -> guild.members }
        .filterWhen(this::guildMemberHasClassAccess)
        .flatMap(this::processRoleReassignment)
        .then()
    ).thenEmpty(
      command.createFollowup()
        .withContent("Done")
        .then()
        .onErrorComplete()
    ).onErrorResume { error ->
      command.createFollowup()
        .withContent("Something went wrong: ${error.message}")
        .then()
    }


  private fun guildMemberHasClassAccess(member: Member): Mono<Boolean> =
    data.nimOfDiscordUserId(member.id)
      .flatMap { result ->
        when (result) {
          is Either.Left -> Mono.just(false)
          is Either.Right -> data.nimIsMember(result.value)
        }
      }
      .defaultIfEmpty(false)

  private fun processRoleReassignment(member: Member): Mono<Void> =
    removeAllClassRoles(member).thenEmpty(addClassRoles(member))

  private fun removeAllClassRoles(member: Member): Mono<Void> =
    Mono.whenDelayError(
      classRoles.values
        .asSequence()
        .map { role -> member.removeRole(role).then() }
        .asIterable()
    )

  private fun addClassRoles(member: Member): Mono<Void> =
    data.nimOfDiscordUserId(member.id)
      .flatMapEither { nim ->
        data.classSelectionsOfNim(nim)
      }.map { result ->
        when (result) {
          is Either.Left -> throw result.value
          is Either.Right -> result.value
        }
      }.map { classIds ->
        classIds.mapNotNull { id ->
          classRoles[id]
        }
      }.flatMap { roleIds ->
        Mono.whenDelayError(
          roleIds.asSequence()
            .map { id ->
              member.addRole(id).then()
            }
            .asIterable()
        )
      }.log()
}