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

package dev.profunktor.tracer.http

import cats.effect.Sync
import cats.syntax.flatMap._
import dev.profunktor.tracer.Trace._
import dev.profunktor.tracer.Tracer
import dev.profunktor.tracer.algebra.UserAlgebra
import dev.profunktor.tracer.auth.{AuthTracedHttpRoute, Http4sAuthTracerDsl}
import dev.profunktor.tracer.model.user.Username
import io.circe.generic.auto._
import org.http4s._
import org.http4s.server.{AuthMiddleware, Router}

class AuthRoutes[F[_]: Sync: Tracer](users: UserAlgebra[Trace[F, ?]]) extends Http4sAuthTracerDsl[F] {

  private[http] val PathPrefix = "/auth"

  private val httpRoutes: AuthedRoutes[String, F] = AuthTracedHttpRoute[String, F] {
    case GET -> Root as user using traceId =>
      users.find(Username(user)).run(traceId) >> Ok(user -> traceId)

    case POST -> Root as user using traceId =>
      Created(user -> traceId)
  }

  lazy val authMiddleware: AuthMiddleware[F, String] = null

  lazy val routes: HttpRoutes[F] = Router(
    PathPrefix -> authMiddleware(httpRoutes)
  )

}
