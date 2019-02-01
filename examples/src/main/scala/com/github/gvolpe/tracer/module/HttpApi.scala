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
import com.github.gvolpe.tracer.Trace.Trace
import com.github.gvolpe.tracer.http.UserRoutes
import com.github.gvolpe.tracer.{Tracer, TracerLog}
import org.http4s.implicits._
import org.http4s.{HttpApp, HttpRoutes}

class HttpApi[F[_]: Sync: Tracer](
    programs: Programs[Trace[F, ?]]
)(implicit L: TracerLog[Trace[F, ?]]) {

  private val httpRoutes: HttpRoutes[F] =
    new UserRoutes[F](programs.users).routes

  val httpApp: HttpApp[F] =
    Tracer[F].loggingMiddleware(httpRoutes.orNotFound)

}
