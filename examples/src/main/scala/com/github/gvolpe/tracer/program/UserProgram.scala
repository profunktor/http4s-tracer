/*
 * Copyright 2018-2019 Gabriel Volpe
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

import cats.MonadError
import cats.syntax.all._
import cats.temp.par._
import com.github.gvolpe.tracer.algebra.UserAlgebra
import com.github.gvolpe.tracer.http.client.UserRegistry
import com.github.gvolpe.tracer.model.errors.UserError._
import com.github.gvolpe.tracer.model.user.{User, Username}
import com.github.gvolpe.tracer.repository.algebra.UserRepository

class UserProgram[F[_]: Par](
    repo: UserRepository[F],
    userRegistry: UserRegistry[F]
)(implicit F: MonadError[F, Throwable])
    extends UserAlgebra[F] {

  def find(username: Username): F[User] =
    repo.find(username).flatMap {
      case Some(u) => F.pure(u)
      case None    => F.raiseError(UserNotFound(username))
    }

  def persist(user: User): F[Unit] =
    repo.find(user.username).flatMap {
      case Some(_) => F.raiseError(UserAlreadyExists(user.username))
      case None    => (userRegistry.register(user), repo.persist(user)).parTupled.void
    }

}
