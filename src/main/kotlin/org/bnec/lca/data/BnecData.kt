package org.bnec.lca.data

import arrow.core.Either
import discord4j.common.util.Snowflake
import reactor.core.publisher.Mono

interface BnecData {
  fun nimIsMember(nim: String): Mono<Boolean>
  fun insertMemberData(nim: String, discordUserId: Snowflake): Mono<Either<Throwable, Unit>>
  fun nimOfDiscordUserId(discordUserId: Snowflake): Mono<Either<Throwable, String>>
  fun classSelectionsOfNim(nim: String): Mono<Either<Throwable, List<Int>>>
  fun getAllVerifiedMembers(): Mono<Either<Throwable, List<Pair<String, Snowflake>>>>
}