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

package com.github.gvolpe.tracer.module

import cats.effect.Sync
import cats.temp.par._
import com.github.gvolpe.tracer.algebra.UserAlgebra
import com.github.gvolpe.tracer.program.UserProgram

private[module] trait Programs[F[_]] {
  def users: UserAlgebra[F]
}

final case class LivePrograms[F[_]: Par: Sync](
    repos: Repositories[F],
    clients: HttpClients[F]
) extends Programs[F] {
  def users: UserAlgebra[F] = new UserProgram[F](repos.users, clients.userRegistry)
}
