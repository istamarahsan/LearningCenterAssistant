package org.bnec.lca.data

import arrow.core.Either
import discord4j.common.util.Snowflake
import reactor.core.publisher.Mono

interface BnecData {
  fun nimIsMember(nim: String): Mono<Boolean>
  fun insertMemberData(nim: String, discordUserId: Snowflake): Mono<Either<Throwable, Unit>>
}