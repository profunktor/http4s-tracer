package com.github.gvolpe.tracer.program

import cats.effect.Sync
import cats.syntax.all._
import com.github.gvolpe.tracer.algebra.UserAlgebra
import com.github.gvolpe.tracer.model.user.{User, Username}
import com.github.gvolpe.tracer.repository.algebra.UserRepository

case class UserAlreadyExists(username: Username) extends Exception(username.value)
case class UserNotFound(username: Username)      extends Exception(username.value)

class UserProgram[F[_]](repo: UserRepository[F])(implicit F: Sync[F]) extends UserAlgebra[F] {

  override def find(username: Username): F[User] = {
    val notFound = F.raiseError[User](UserNotFound(username))
    for {
      mu <- repo.find(username)
      rs <- mu.fold(notFound)(F.pure)
    } yield rs
  }

  override def persist(user: User): F[Unit] = {
    val alreadyExists = F.raiseError[Unit](UserAlreadyExists(user.username))
    for {
      mu <- repo.find(user.username)
      rs <- mu.fold(repo.persist(user))(_ => alreadyExists)
    } yield rs
  }

}
