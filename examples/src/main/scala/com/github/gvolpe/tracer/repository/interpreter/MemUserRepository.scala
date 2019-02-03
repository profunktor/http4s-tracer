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

package com.github.gvolpe.tracer.repository.interpreter

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.all._
import com.github.gvolpe.tracer.model.user.{User, Username}
import com.github.gvolpe.tracer.repository.algebra.UserRepository

object MemUserRepository {
  def create[F[_]: Sync]: F[UserRepository[F]] =
    Ref.of[F, Map[Username, User]](Map.empty).map(new MemUserRepository[F](_))
}

class MemUserRepository[F[_]: Sync] private (
    state: Ref[F, Map[Username, User]]
) extends UserRepository[F] {

  def find(username: Username): F[Option[User]] =
    state.get.map(_.get(username))

  def persist(user: User): F[Unit] =
    state.update(_.updated(user.username, user))

}
