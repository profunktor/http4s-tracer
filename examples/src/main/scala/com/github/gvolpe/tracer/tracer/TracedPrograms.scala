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

package com.github.gvolpe.tracer.tracer

import cats.effect.Sync
import com.github.gvolpe.tracer.Trace.Trace
import com.github.gvolpe.tracer.TracerLog
import com.github.gvolpe.tracer.algebra.UserAlgebra
import com.github.gvolpe.tracer.model.user.{User, Username}
import com.github.gvolpe.tracer.module.{Programs, Repositories}
import com.github.gvolpe.tracer.program.UserProgram
import com.github.gvolpe.tracer.repository.algebra.UserRepository

class TracedPrograms[F[_]: Sync](repos: Repositories[Trace[F, ?]])(implicit L: TracerLog[Trace[F, ?]])
    extends Programs[Trace[F, ?]] {
  override val users: UserAlgebra[Trace[F, ?]] = new UserTracer[F](repos.users)
}

class UserTracer[F[_]: Sync](
    repo: UserRepository[Trace[F, ?]]
)(implicit L: TracerLog[Trace[F, ?]])
    extends UserProgram[Trace[F, ?]](repo) {

  override def find(username: Username): Trace[F, User] =
    for {
      _ <- L.info[UserAlgebra[F]](s"Find user by username: ${username.value}")
      u <- super.find(username)
    } yield u

  override def persist(user: User): Trace[F, Unit] =
    for {
      _  <- L.info[UserAlgebra[F]](s"About to persist user: ${user.username.value}")
      rs <- super.persist(user)
    } yield rs

}
