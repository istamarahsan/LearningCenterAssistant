package org.bnec.lca.data

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import discord4j.common.util.Snowflake
import org.ktorm.database.Database
import org.ktorm.dsl.insert
import reactor.core.publisher.Mono

class KtormBnecData(private val memberNims: Set<String>, private val db: Database) : BnecData {
  override fun nimIsMember(nim: String): Mono<Boolean> = Mono.just(memberNims.contains(nim))

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
}
      
