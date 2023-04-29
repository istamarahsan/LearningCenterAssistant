package org.bnec.util

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import reactor.core.publisher.Mono

inline fun <Left, LeftA : Left, LeftB : Left, A, B> Mono<Either<LeftA, A>>.mapEither(crossinline f: (A) -> Either<LeftB, B>): Mono<Either<Left, B>> =
  this.map { it.flatMap(f) }

inline fun <Left, LeftA : Left, LeftB : Left, A, B> Mono<Either<LeftA, A>>.flatMapEither(crossinline f: (A) -> Mono<Either<LeftB, B>>): Mono<Either<Left, B>> =
  this.flatMap { it.fold(ifLeft = { a -> Mono.just(a.left()) }, ifRight = f) }