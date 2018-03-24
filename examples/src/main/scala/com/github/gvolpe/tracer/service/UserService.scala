/*
 * Copyright 2018 com.github.gvolpe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.gvolpe.tracer.service

import cats.data.Kleisli
import cats.effect.Sync
import cats.syntax.all._
import com.github.gvolpe.tracer.Tracer.Traceable
import com.github.gvolpe.tracer.TracerLog
import com.github.gvolpe.tracer.model.user.{User, Username}
import com.github.gvolpe.tracer.repository.UserRepository

trait UserService[F[_]] {
  def find(username: Username): F[User]
  def persist(user: User): F[Unit]
}

case class UserAlreadyExists(username: Username) extends Exception(username.value)
case class UserNotFound(username: Username)      extends Exception(username.value)

class DefaultUserService[F[_]](repo: UserRepository[Traceable[F, ?]])(implicit F: Sync[F], L: TracerLog[Traceable[F, ?]])
    extends UserService[Traceable[F, ?]] {

  override def find(username: Username): Traceable[F, User] = Kleisli { id =>
    val notFound = F.raiseError[User](UserNotFound(username))
    for {
      _  <- L.info[UserService[F]](s"Find user by username: ${username.value}").run(id)
      mu <- repo.find(username).run(id)
      rs <- mu.fold(notFound)(F.pure)
    } yield rs
  }

  override def persist(user: User): Traceable[F, Unit] = Kleisli { id =>
    val alreadyExists = F.raiseError[Unit](UserAlreadyExists(user.username))
    for {
      _  <- L.info[UserService[F]](s"About to persist user: ${user.username.value}").run(id)
      mu <- repo.find(user.username).run(id)
      rs <- mu.fold(repo.persist(user).run(id))(_ => alreadyExists)
    } yield rs
  }
}
