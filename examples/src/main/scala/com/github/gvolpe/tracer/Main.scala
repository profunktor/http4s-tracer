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

package com.github.gvolpe.tracer

import cats.effect._
import cats.syntax.all._
import com.github.gvolpe.tracer.module._
import com.github.gvolpe.tracer.Trace.Trace
import com.github.gvolpe.tracer.tracer.{TracedPrograms, TracedRepositories}
import org.http4s.server.blaze.BlazeServerBuilder

class Main[F[_]: ConcurrentEffect: Timer: Tracer](implicit L: TracerLog[Trace[F, ?]]) {

  val server: F[Unit] =
    LiveRepositories[F].flatMap { repositories =>
      val tracedRepos    = new TracedRepositories[F](repositories)
      val tracedPrograms = new TracedPrograms[F](tracedRepos)
      val httpApi        = new HttpApi[F](tracedPrograms)

      BlazeServerBuilder[F]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(httpApi.httpApp)
        .serve
        .compile
        .drain
    }

}
