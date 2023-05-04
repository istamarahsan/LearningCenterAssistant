package org.bnec.lca.commands

import arrow.core.*
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.discordjson.json.ApplicationCommandOptionData
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.discordjson.json.ImmutableApplicationCommandRequest
import org.bnec.lca.*
import org.bnec.lca.data.MemberDiscordIds
import org.bnec.util.asOption
import org.ktorm.dsl.insert
import reactor.core.publisher.Mono

object Verify: SlashCommand {
  private sealed interface VerifyNimError {
    object DiscordCommandError : VerifyNimError
    object DataAccessError : VerifyNimError
    class NimNotFound(val nim: String) : VerifyNimError
    object AddRoleError : VerifyNimError
    object AlreadyVerified : VerifyNimError
  }
  override fun signature(): ImmutableApplicationCommandRequest =
    ApplicationCommandRequest.builder().name("verify").description("verify with your NIM").addOption(
      ApplicationCommandOptionData.builder().name("nim").description("Your NIM")
        .type(ApplicationCommandOption.Type.STRING.value).required(true).build()
    ).build()
  

  override fun handle(command: ChatInputInteractionEvent): Mono<Void> =
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
  
  private fun viewErrorMessageForVerifyNimError(err: VerifyNimError): String = when (err) {
    is VerifyNimError.AddRoleError -> "Hmm, we weren't able to give you the right access. Please inform the staff of this technical issue."
    is VerifyNimError.DataAccessError -> "Sorry, we had issues accessing our data. Please notify staff of this issue."
    is VerifyNimError.DiscordCommandError -> "Something went wrong with discord's slash commands. Please try again later."
    is VerifyNimError.NimNotFound -> "We couldn't find NIM: ${err.nim}. If this is a mistake, please contact staff."
    is VerifyNimError.AlreadyVerified -> "It seems that you're already verified."
  }
}