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

package dev.profunktor.tracer.http
package client

import cats.effect.Sync
import cats.syntax.functor._
import dev.profunktor.tracer.model.user.User
import io.circe.syntax._
import org.http4s.Method._
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl

trait UserRegistry[F[_]] {
  def register(user: User): F[Unit]
}

final case class LiveUserRegistry[F[_]: Sync](
    client: Client[F]
) extends UserRegistry[F]
    with Http4sClientDsl[F] {

  private val uri = Uri.uri("https://jsonplaceholder.typicode.com/posts")

  def register(user: User): F[Unit] =
    client.successful(POST(user.asJson, uri)).void
}
