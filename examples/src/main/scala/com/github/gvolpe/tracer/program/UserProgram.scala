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
