package org.bnec.lca.commands

import arrow.core.getOrElse
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
        data.getAllVerifiedMembers().map { result -> result.getOrElse { throw it } },
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
        data.classSelectionsOfNim(nim)
          .map { result -> result.getOrElse { throw it } }
          .map { classIds -> Pair(memberData, classIds) }
          .doOnError { error -> println("Error while getting class selections of: { nim: $nim, discord user id: ${memberData.id} }. Error message: ${error.message}") }
          .onErrorComplete()
      }.map { (memberData, classIds) ->
        Pair(
          memberData,
          classIds.mapNotNull { classId -> classRoles[classId] }
        )
      }.flatMap { (memberData, roleIdsForClasses) ->
        Mono.`when`(
          memberData.roleIds
            .asSequence()
            .filter { existingMemberRoleId -> classRoles.containsValue(existingMemberRoleId) }
            .filter { existingClassRoleId -> !roleIdsForClasses.contains(existingClassRoleId) }
            .map { invalidClassRoleId -> memberData.removeRole(invalidClassRoleId) }
            .asIterable()
        ).thenEmpty(
          Mono.`when`(
            roleIdsForClasses.asSequence()
              .filter { classRoleId -> !memberData.roleIds.contains(classRoleId) }
              .map { classRoleIdToAdd -> memberData.addRole(classRoleIdToAdd) }
              .asIterable()
          )
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