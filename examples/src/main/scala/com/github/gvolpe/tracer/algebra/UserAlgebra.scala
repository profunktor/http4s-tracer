package com.github.gvolpe.tracer.algebra

import com.github.gvolpe.tracer.model.user.{User, Username}

trait UserAlgebra[F[_]] {
  def find(username: Username): F[User]
  def persist(user: User): F[Unit]
}
