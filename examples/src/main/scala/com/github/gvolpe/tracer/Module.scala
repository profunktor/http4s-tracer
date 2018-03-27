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
import com.github.gvolpe.tracer.Tracer.KFX
import com.github.gvolpe.tracer.algebra.UserAlgebra
import com.github.gvolpe.tracer.http.UserRoutes
import com.github.gvolpe.tracer.interpreter.UserTracerInterpreter
import com.github.gvolpe.tracer.instances.tracerlog._
import com.github.gvolpe.tracer.repository.UserTracerRepository
import com.github.gvolpe.tracer.repository.algebra.UserRepository
import org.http4s.HttpService

class Module[F[_]: Sync] {

  private val repo: UserRepository[KFX[F, ?]] =
    new UserTracerRepository[F]

  private val service: UserAlgebra[KFX[F, ?]] =
    new UserTracerInterpreter[F](repo)

  private val httpRoutes: HttpService[F] =
    new UserRoutes[F](service).routes

  val routes: HttpService[F] =
    Tracer(httpRoutes)

}
