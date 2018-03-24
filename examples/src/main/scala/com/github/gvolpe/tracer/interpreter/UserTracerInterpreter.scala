package com.github.gvolpe.tracer.interpreter

import cats.effect.Sync
import com.github.gvolpe.tracer.Tracer.KFX
import com.github.gvolpe.tracer.TracerLog
import com.github.gvolpe.tracer.algebra.UserAlgebra
import com.github.gvolpe.tracer.model.user.{User, Username}
import com.github.gvolpe.tracer.program.UserProgram
import com.github.gvolpe.tracer.repository.algebra.UserRepository

class UserTracerInterpreter[F[_]](repo: UserRepository[KFX[F, ?]])
                                 (implicit F: Sync[F], L: TracerLog[KFX[F, ?]])
  extends UserProgram[KFX[F, ?]](repo) {

  override def find(username: Username): KFX[F, User] =
    for {
      _ <- L.info[UserAlgebra[F]](s"Find user by username: ${username.value}")
      u <- super.find(username)
    } yield u

  override def persist(user: User): KFX[F, Unit] =
    for {
      _  <- L.info[UserAlgebra[F]](s"About to persist user: ${user.username.value}")
      rs <- super.persist(user)
    } yield rs
}
