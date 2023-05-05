package org.bnec.lca.data

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import discord4j.common.util.Snowflake
import org.ktorm.database.Database
import org.ktorm.dsl.*
import reactor.core.publisher.Mono

class KtormDataPartial(private val inMemory: InMemoryData, private val db: Database) : BnecData {
  override fun nimIsMember(nim: String): Mono<Boolean> = inMemory.nimIsMember(nim)

  override fun insertMemberData(nim: String, discordUserId: Snowflake): Mono<Either<Throwable, Unit>> =
    Mono.fromSupplier {
      db.runCatching {
        insert(MemberDiscordIds) {
          set(it.discordUserId, discordUserId.asString())
          set(it.nim, nim)
        }
      }.fold(
        onSuccess = { Unit.right() },
        onFailure = { it.left() }
      )
    }

  override fun nimOfDiscordUserId(discordUserId: Snowflake): Mono<Either<Throwable, String>> =
    Mono.fromSupplier {
      db.runCatching {
        from(MemberDiscordIds)
          .select(MemberDiscordIds.nim)
          .where { MemberDiscordIds.discordUserId.eq(discordUserId.asString()) }
          .limit(1)
          .mapNotNull { it[MemberDiscordIds.nim] }
          .first()
      }.fold(
        onSuccess = { it.right() },
        onFailure = { it.left() }
      )
    }
  
  override fun classSelectionsOfNim(nim: String): Mono<Either<Throwable, List<Int>>> = inMemory.classSelectionsOfNim(nim)
}
      
