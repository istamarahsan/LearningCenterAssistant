package org.bnec.lca.commands

import arrow.core.*
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.discordjson.json.ApplicationCommandOptionData
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.discordjson.json.ImmutableApplicationCommandRequest
import org.bnec.lca.data.BnecData
import org.bnec.util.asOption
import org.bnec.util.flatMapEither
import org.bnec.util.mapEither
import reactor.core.publisher.Mono

class Verify(private val memberRoleId: Snowflake, private val bnecData: BnecData): SlashCommand {
  private sealed interface VerifyNimError {
    object DiscordCommandError : VerifyNimError
    object DataAccessError : VerifyNimError
    class NimNotFound(val nim: String) : VerifyNimError
    object AddRoleError : VerifyNimError
    object AlreadyVerified : VerifyNimError
    class UnhandledError(val error: Throwable) : VerifyNimError
  }
  override fun signature(): ImmutableApplicationCommandRequest =
    ApplicationCommandRequest.builder().name("verify").description("verify with your NIM").addOption(
      ApplicationCommandOptionData.builder().name("nim").description("Your NIM")
        .type(ApplicationCommandOption.Type.STRING.value).required(true).build()
    ).build()
  

  override fun handle(command: ChatInputInteractionEvent): Mono<Void> =
    command.deferReply().withEphemeral(true).then(
      Mono.fromSupplier { extractNimOption(command) }
        .flatMapEither { nim -> verifyNimIsMember(nim) }
        .flatMapEither { nim -> insertMemberData(nim, command.interaction.user.id) }
        .mapEither { command.interaction.guildId.asOption().toEither { VerifyNimError.AddRoleError } }
        .flatMapEither { guildId -> command.interaction.user.asMember(guildId).map { it.right() } }
        .flatMapEither { member -> member.addRole(memberRoleId).thenReturn(Unit.right()) }
        .onErrorResume { unhandledError -> Mono.just(VerifyNimError.UnhandledError(unhandledError).left()) } 
        .flatMap { result -> 
          println(result)
          when (result) {
            is Either.Left -> command.createFollowup().withContent(viewErrorMessageForVerifyNimError(result.value))
            is Either.Right -> command.createFollowup().withContent("Welcome, Extraordinary!")
          }
      }.then()
    )
  
  private fun extractNimOption(command: ChatInputInteractionEvent): Either<VerifyNimError.DiscordCommandError, String> =
    command.getOption("nim")
      .asOption()
      .flatMap { it.value.asOption() }
      .toEither { VerifyNimError.DiscordCommandError }
      .map { it.asString() }
  
  private fun verifyNimIsMember(nim: String): Mono<Either<VerifyNimError.NimNotFound, String>> =
    bnecData.nimIsMember(nim).map { nimIsMember -> if (nimIsMember) nim.right() else VerifyNimError.NimNotFound(nim).left()}
  
  private fun insertMemberData(nim: String, discordUserId: Snowflake): Mono<Either<VerifyNimError.DataAccessError, Unit>> =
    bnecData.insertMemberData(nim, discordUserId).map { result -> result.mapLeft { VerifyNimError.DataAccessError } }
  
  private fun viewErrorMessageForVerifyNimError(err: VerifyNimError): String = when (err) {
    VerifyNimError.AddRoleError -> "Hmm, we weren't able to give you the right access. Please inform the staff of this technical issue."
    VerifyNimError.DataAccessError -> "Sorry, we had issues accessing our data. Please notify staff of this issue."
    VerifyNimError.DiscordCommandError -> "Something went wrong with discord's slash commands. Please try again later."
    is VerifyNimError.NimNotFound -> "We couldn't find NIM: ${err.nim}. If this is a mistake, please contact staff."
    VerifyNimError.AlreadyVerified -> "It seems that you're already verified."
    is VerifyNimError.UnhandledError -> "Sorry, something unexpected happened. Please try again or notify staff."
  }
}