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

package com.github.gvolpe.tracer.module.tracer

import cats.FlatMap
import cats.syntax.apply._
import com.github.gvolpe.tracer.Trace.Trace
import com.github.gvolpe.tracer.model.user.{User, Username}
import com.github.gvolpe.tracer.module.Repositories
import com.github.gvolpe.tracer.repository.algebra.UserRepository
import com.github.gvolpe.tracer.{Trace, TracerLog}

case class TracedRepositories[F[_]: FlatMap](
    repos: Repositories[F]
)(implicit L: TracerLog[Trace[F, ?]])
    extends Repositories[Trace[F, ?]] {
  val users: UserRepository[Trace[F, ?]] = new UserTracerRepository[F](repos.users)
}

private[tracer] final class UserTracerRepository[F[_]: FlatMap](
    repo: UserRepository[F]
)(implicit L: TracerLog[Trace[F, ?]])
    extends UserRepository[Trace[F, ?]] {

  override def find(username: Username): Trace[F, Option[User]] =
    L.info[UserRepository[F]](s"Find user by username: ${username.value}") *>
      Trace(_ => repo.find(username))

  override def persist(user: User): Trace[F, Unit] =
    L.info[UserRepository[F]](s"Persisting user: ${user.username.value}") *>
      Trace(_ => repo.persist(user))

}
