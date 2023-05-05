package org.bnec.lca

import kotlinx.serialization.Serializable

@Serializable
class ConfigFile(
  val botToken: String? = null,
  val memberRoleId: String,
  val classRoles: Map<String, String>,
  val classSelections: Map<String, Array<Int>>)