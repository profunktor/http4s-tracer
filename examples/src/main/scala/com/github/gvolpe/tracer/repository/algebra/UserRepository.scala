package com.github.gvolpe.tracer.repository.algebra

import com.github.gvolpe.tracer.model.user.{User, Username}

trait UserRepository[F[_]] {
  def find(username: Username): F[Option[User]]
  def persist(user: User): F[Unit]
}
