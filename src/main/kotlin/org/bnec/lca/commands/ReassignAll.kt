package org.bnec.lca.commands

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.discordjson.json.ImmutableApplicationCommandRequest
import org.bnec.lca.data.BnecData
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class ReassignAll(
  private val memberRoleId: Snowflake,
  private val classRoles: Map<Int, Snowflake>,
  private val data: BnecData
) : SlashCommand {
  override fun signature(): ImmutableApplicationCommandRequest =
    ApplicationCommandRequest.builder()
      .name("reassignall")
      .description("assign class roles to verified members (overwrites)")
      .defaultPermission(false)
      .dmPermission(false)
      .build()

  override fun handle(command: ChatInputInteractionEvent): Mono<Void> =
    command.deferReply().withEphemeral(true).then(
      Mono.zip(
        command.interaction.guild.switchIfEmpty(Mono.error(Error("This command only works on servers"))),
        data.getAllVerifiedMembers().map { result -> result.getOrNull() ?: throw Error("Could not retrieve members") },
        ::Pair
      ).flatMapMany { (guild, verificationDataSet) ->
        Flux.merge(
          verificationDataSet.asSequence()
            .map { (nim, discordUserId) -> 
              guild.getMemberById(discordUserId).map { serverMemberData -> Pair(nim, serverMemberData) }
          }
            .asIterable()
        )
      }.filter { (_, memberData) ->
        memberData.roleIds.contains(memberRoleId)
      }.flatMap { (nim, memberData) ->
        Mono.whenDelayError(
          classRoles.values
            .map { roleId -> memberData.removeRole(roleId).then() }
            .asIterable()
        ).onErrorComplete()
          .then(
            data.classSelectionsOfNim(nim)
              .map { result -> result.getOrNull() ?: throw Error("Error in retrieving class selections") }
              .map { classIds -> Pair(memberData, classIds) }
        )
      }.map { (memberData, classIds) ->
        Pair(
          memberData,
          classIds.mapNotNull { classId -> classRoles[classId] }
        )
      }.flatMap { (memberData, roleIds) ->
        Flux.merge(
          roleIds.map { roleId -> memberData.addRole(roleId).then() }
            .asIterable()
        )
      }.then()
    ).thenEmpty(
      command.createFollowup()
        .withContent("Done!")
        .then()
    ).onErrorResume { error ->
      command.createFollowup()
        .withContent("An error occurred: ${error.message}")
        .then()
    }
}