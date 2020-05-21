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

package dev.profunktor.tracer.module

import cats.effect.Sync
import dev.profunktor.tracer.http.client.{LiveUserRegistry, UserRegistry}
import org.http4s.client.Client

private[module] trait HttpClients[F[_]] {
  def userRegistry: UserRegistry[F]
}

final case class LiveHttpClients[F[_]: Sync](client: Client[F]) extends HttpClients[F] {
  def userRegistry: UserRegistry[F] = LiveUserRegistry[F](client)
}
