package org.bnec.lca

import kotlinx.serialization.Serializable

@Serializable
class ConfigFile(
  val botToken: String? = null,
  val memberRoleId: String,
  val classes: Array<ClassRoleAssociation>)

@Serializable
data class ClassRoleAssociation(
  val id: String,
  val roleId: String
)