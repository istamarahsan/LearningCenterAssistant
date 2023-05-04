package org.bnec.lca.data

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import discord4j.common.util.Snowflake
import reactor.core.publisher.Mono
import java.lang.Error

class InMemoryData(private val memberNimSet: Set<String>, initialData: List<Pair<String, Snowflake>> = emptyList()) : Data {
  private val memberDiscordIds: MutableMap<String, Snowflake>
  
  init {
    memberDiscordIds = initialData.toMap().toMutableMap()
  }

  override fun nimIsMember(nim: String): Mono<Boolean> = 
    Mono.just(memberNimSet.contains(nim))

  override fun insertMemberData(nim: String, discordUserId: Snowflake): Mono<Either<Throwable, Unit>> =
    Mono.just(if (memberDiscordIds.containsKey(nim)) Error("nim already registered").left() else Unit.right())
}