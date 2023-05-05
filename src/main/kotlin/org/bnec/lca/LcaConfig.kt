package org.bnec.lca

import discord4j.common.util.Snowflake

data class LcaConfig(
  val botToken: String,
  val memberRoleId: Snowflake,
  val sudo: List<Snowflake>,
  val classRoles: Map<Int, Snowflake>
)