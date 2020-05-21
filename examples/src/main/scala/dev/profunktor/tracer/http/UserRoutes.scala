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

import cats.effect.Sync
import cats.syntax.all._
import dev.profunktor.tracer.Trace._
import dev.profunktor.tracer.algebra.UserAlgebra
import dev.profunktor.tracer.model.user.{User, Username}
import dev.profunktor.tracer.model.errors.UserError._
import dev.profunktor.tracer.{Http4sTracerDsl, TracedHttpRoute, Tracer}
import org.http4s._
import org.http4s.server.Router

class UserRoutes[F[_]: Sync: Tracer](users: UserAlgebra[Trace[F, ?]]) extends Http4sTracerDsl[F] {

  private[http] val PathPrefix = "/users"

  private val httpRoutes: HttpRoutes[F] = TracedHttpRoute[F] {
    case GET -> Root / username using traceId =>
      users
        .find(Username(username))
        .run(traceId)
        .flatMap(user => Ok(user))
        .handleErrorWith {
          case UserNotFound(u) => NotFound(u.value)
        }

    case tr @ POST -> Root using traceId =>
      tr.request.decode[User] { user =>
        users
          .persist(user)
          .run(traceId)
          .flatMap(_ => Created())
          .handleErrorWith {
            case UserAlreadyExists(u) => Conflict(u.value)
          }
      }
  }

  val routes: HttpRoutes[F] = Router(
    PathPrefix -> httpRoutes
  )

}
