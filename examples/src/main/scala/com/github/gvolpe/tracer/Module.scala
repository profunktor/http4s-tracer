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

package com.github.gvolpe.tracer

import cats.effect.Sync
import cats.syntax.applicative._
import com.github.gvolpe.tracer.Tracer.{KFX, TraceIdHeaderName}
import com.github.gvolpe.tracer.algebra.UserAlgebra
import com.github.gvolpe.tracer.http.UserRoutes
import com.github.gvolpe.tracer.instances.tracerlog._
import com.github.gvolpe.tracer.interpreter.UserTracerInterpreter
import com.github.gvolpe.tracer.repository.UserTracerRepository
import com.github.gvolpe.tracer.repository.algebra.UserRepository
import com.github.gvolpe.tracer.typeclasses.Ask
import org.http4s.HttpRoutes

class Module[F[_]: Sync] {

  // Header name can be customized if an instance of Ask[F, TraceIdHeaderName] is provided. Default name is "Trace-Id".
  private implicit val ask: Ask[F, TraceIdHeaderName] =
    new Ask[F, TraceIdHeaderName] {
      override def ask: F[TraceIdHeaderName] = TraceIdHeaderName("Flow-Id").pure[F]
    }

  private val repo: UserRepository[KFX[F, ?]] =
    new UserTracerRepository[F]

  private val service: UserAlgebra[KFX[F, ?]] =
    new UserTracerInterpreter[F](repo)

  private val httpRoutes: HttpRoutes[F] =
    new UserRoutes[F](service).routes

  val routes: HttpRoutes[F] =
    Tracer[F](httpRoutes)

}
