package org.bnec.lca.data

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import discord4j.common.util.Snowflake
import reactor.core.publisher.Mono

class InMemoryData(private val memberNimSet: Set<String>, private val classSelections: Map<String, List<Int>>, initialData: List<Pair<String, Snowflake>> = emptyList()) :
  BnecData {
  private val memberDiscordIds: MutableList<Pair<String, Snowflake>>

  init {
    memberDiscordIds = initialData.toMutableList()
  }

  override fun nimIsMember(nim: String): Mono<Boolean> =
    Mono.just(memberNimSet.contains(nim)) 

  override fun insertMemberData(nim: String, discordUserId: Snowflake): Mono<Either<Throwable, Unit>> {
    if (memberDiscordIds.any{ pair -> pair.first == nim })
      return Mono.just(Error("nim already registered").left()) 
    memberDiscordIds.add(Pair(nim, discordUserId))
    return Mono.just(Unit.right())
  }

  override fun nimOfDiscordUserId(discordUserId: Snowflake): Mono<Either<Throwable, String>> =
    Mono.just(
      memberDiscordIds
        .firstOrNull { pair -> pair.second == discordUserId }?.first?.right() 
        ?: Error("user with Discord user id \"$discordUserId\" was not found").left() 
    )
  

  override fun classSelectionsOfNim(nim: String): Mono<Either<Throwable, List<Int>>> =
    Mono.just(classSelections[nim]?.right() ?: Error("Class selections for NIM: \"$nim\" not found").left())

  override fun getAllVerifiedMembers(): Mono<Either<Throwable, List<Pair<String, Snowflake>>>> =
    Mono.just(memberDiscordIds.right())

}