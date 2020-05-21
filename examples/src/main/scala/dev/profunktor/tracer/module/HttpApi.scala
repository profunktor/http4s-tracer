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
import dev.profunktor.tracer.Trace.Trace
import dev.profunktor.tracer.http.UserRoutes
import dev.profunktor.tracer.module.tracer.TracedPrograms
import dev.profunktor.tracer.{Tracer, TracerLog}
import org.http4s.implicits._
import org.http4s.{HttpApp, HttpRoutes}

final case class HttpApi[F[_]: Sync: Tracer: λ[T[_] => TracerLog[Trace[T, ?]]]](
    programs: TracedPrograms[F]
) {

  private val httpRoutes: HttpRoutes[F] =
    new UserRoutes[F](programs.users).routes

  val httpApp: HttpApp[F] =
    Tracer[F].loggingMiddleware(httpRoutes.orNotFound)

}
