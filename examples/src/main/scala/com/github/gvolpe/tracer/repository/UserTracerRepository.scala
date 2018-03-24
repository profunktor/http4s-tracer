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

package com.github.gvolpe.tracer.repository

import cats.data.Kleisli
import cats.effect.Sync
import cats.syntax.all._
import com.github.gvolpe.tracer.Tracer.KFX
import com.github.gvolpe.tracer.TracerLog
import com.github.gvolpe.tracer.model.user.{User, Username}
import com.github.gvolpe.tracer.repository.algebra.UserRepository

class UserTracerRepository[F[_]](implicit F: Sync[F], L: TracerLog[KFX[F, ?]]) extends UserRepository[KFX[F, ?]] {
  private val users = scala.collection.mutable.Map.empty[Username, User]

  override def find(username: Username): KFX[F, Option[User]] = Kleisli { id =>
    for {
      _ <- L.info[UserRepository[F]](s"Find user by username: ${username.value}").run(id)
      u <- F.delay(users.get(username))
    } yield u
   }

  override def persist(user: User): KFX[F, Unit] = Kleisli { id =>
    for {
      _ <- L.info[UserRepository[F]](s"Persisting user: ${user.username.value}").run(id)
      _ <- F.delay(users.update(user.username, user))
    } yield ()
  }
}
