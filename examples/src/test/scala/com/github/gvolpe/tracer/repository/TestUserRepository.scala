/*
 * Copyright 2018-2019 ProfunKtor
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

package dev.profunktor.tracer.repository

import cats.effect.Sync
import cats.implicits._
import dev.profunktor.tracer.model.user.{User, Username}
import dev.profunktor.tracer.repository.algebra.UserRepository

class TestUserRepository[F[_]: Sync] extends UserRepository[F] {
  def find(username: Username): F[Option[User]] =
    (if (username.value == "xxx") none[User] else Some(User(username))).pure[F]

  def persist(user: User): F[Unit] = ().pure[F]
}
