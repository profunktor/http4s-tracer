/*
 * Copyright 2018-2020 ProfunKtor
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

package dev.profunktor.tracer.module.tracer

import cats.effect.Sync
import cats.syntax.apply._
import dev.profunktor.tracer.Trace.Trace
import dev.profunktor.tracer.http.client.UserRegistry
import dev.profunktor.tracer.model.user.User
import dev.profunktor.tracer.module.{HttpClients, LiveHttpClients}
import dev.profunktor.tracer.{Trace, TracerLog}
import org.http4s.client.Client

case class TracedHttpClients[F[_]: Sync: λ[T[_] => TracerLog[Trace[T, ?]]]] private (
    client: Client[F]
) extends HttpClients[Trace[F, ?]] {
  private val clients = LiveHttpClients[F](client)

  override val userRegistry: UserRegistry[Trace[F, ?]] = new TracedUserRegistry[F](clients.userRegistry)
}

private[tracer] final class TracedUserRegistry[F[_]: Sync](
    registry: UserRegistry[F]
)(implicit L: TracerLog[Trace[F, ?]])
    extends UserRegistry[Trace[F, ?]] {

  override def register(user: User): Trace[F, Unit] =
    L.info[UserRegistry[F]](s"Registering user: ${user.username.value}") *>
      Trace(_ => registry.register(user))

}
