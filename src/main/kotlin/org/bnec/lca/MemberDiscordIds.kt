package org.bnec.lca

import org.ktorm.schema.Table
import org.ktorm.schema.varchar

object MemberDiscordIds : Table<Nothing>("member_discord_ids") {
    val discordUserId = varchar("discord_user_id")
    val nim = varchar("nim")
}