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

package com.github.gvolpe.tracer.http

import cats.effect.Sync
import cats.syntax.all._
import com.github.gvolpe.tracer.Tracer
import com.github.gvolpe.tracer.Tracer.KFX
import com.github.gvolpe.tracer.algebra.UserAlgebra
import com.github.gvolpe.tracer.model.user.{User, Username}
import com.github.gvolpe.tracer.program.{UserAlreadyExists, UserNotFound}
import io.circe.generic.auto._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

class UserRoutes[F[_]: Sync](userService: UserAlgebra[KFX[F, ?]]) extends Http4sDsl[F] {

  private[http] val PathPrefix = "/users"

  private val httpRoutes: HttpService[F] = HttpService[F] {
    case req @ GET -> Root / username =>
      userService
        .find(Username(username))
        .run(Tracer.getTraceId[F](req))
        .flatMap(user => Ok(user))
        .handleErrorWith {
          case UserNotFound(_) => NotFound(username)
        }

    case req @ POST -> Root =>
      req.decode[User] { user =>
        userService
          .persist(user)
          .run(Tracer.getTraceId[F](req))
          .flatMap(_ => Created())
          .handleErrorWith {
            case UserAlreadyExists(_) => Conflict(user.username.value)
          }
      }
  }

  val routes: HttpService[F] = Router(
    PathPrefix -> httpRoutes
  )

}
