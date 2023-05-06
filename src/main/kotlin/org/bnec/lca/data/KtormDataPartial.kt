package org.bnec.lca.data

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import discord4j.common.util.Snowflake
import org.ktorm.database.Database
import org.ktorm.dsl.*
import reactor.core.publisher.Mono

class KtormDataPartial(private val inMemory: InMemoryData, private val db: Database) : BnecData by inMemory {

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

  override fun getAllVerifiedMembers(): Mono<Either<Throwable, List<Pair<String, Snowflake>>>> =
    Mono.fromSupplier {
      db.runCatching {
        from(MemberDiscordIds)
          .select()
          .map { row ->
            Pair(
              row[MemberDiscordIds.nim] ?: throw Error("NIM column not found"),
              row[MemberDiscordIds.discordUserId] ?: throw Error("Discord User ID column not found")
            )
          }.map { pair ->
            Pair(pair.first, Snowflake.of(pair.second))
          }
      }.fold(
        onSuccess = { it.right() },
        onFailure = { it.left() }
      )
    }
}
      
