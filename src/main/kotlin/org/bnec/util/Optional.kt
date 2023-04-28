package org.bnec.util

import arrow.core.Option
import arrow.core.toOption
import java.util.*
import kotlin.jvm.optionals.getOrNull

fun <T : Any> Optional<T>.asOption(): Option<T> {
  return this.getOrNull().toOption()
}