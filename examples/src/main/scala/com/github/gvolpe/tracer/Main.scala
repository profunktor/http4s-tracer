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

package dev.profunktor.tracer

import cats.Parallel
import cats.effect._
import cats.implicits._
import dev.profunktor.tracer.Trace.Trace
import dev.profunktor.tracer.module._
import dev.profunktor.tracer.module.tracer._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

class Main[F[_]: ConcurrentEffect: Parallel: Timer: Tracer: λ[T[_] => TracerLog[Trace[T, ?]]]] {

  val server: F[Unit] =
    BlazeClientBuilder[F](ExecutionContext.global).resource.use { client =>
      for {
        repos          <- LiveRepositories[F]
        tracedRepos    = TracedRepositories[F](repos)
        tracedClients  = TracedHttpClients[F](client)
        tracedPrograms = TracedPrograms[F](tracedRepos, tracedClients)
        httpApi        = HttpApi[F](tracedPrograms)
        _ <- BlazeServerBuilder[F]
              .bindHttp(8080, "0.0.0.0")
              .withHttpApp(httpApi.httpApp)
              .serve
              .compile
              .drain
      } yield ()
    }

}
