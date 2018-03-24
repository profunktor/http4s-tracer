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

import java.util.UUID

import cats.Monad
import cats.data.{Kleisli, OptionT}
import org.http4s.syntax.StringSyntax
import org.http4s.{Header, HttpService, Request, Response}

/**
  * [[org.http4s.server.HttpMiddleware]] that adds a Trace-Id header with a unique UUID value
  * and logs the start of the request and the response with the given UUID.
  *
  * Quite useful to trace the flow of each request. For example:
  *
  * TraceId(72b079c8-fc92-4c4f-aa5a-c0cd91ea221c) >> Http Request /users
  * TraceId(72b079c8-fc92-4c4f-aa5a-c0cd91ea221c) >> UserService fetching users
  * TraceId(72b079c8-fc92-4c4f-aa5a-c0cd91ea221c) >> UserRepository fetching users from DB
  * TraceId(72b079c8-fc92-4c4f-aa5a-c0cd91ea221c) >> MetricsService saving users metrics
  * TraceId(72b079c8-fc92-4c4f-aa5a-c0cd91ea221c) >> HttpResponse users
  *
  * In a normal application, you will have thousands of requests and tracing the call chain in
  * a failure scenario will be invaluable.
  * */
object Tracer extends StringSyntax {

  private val TraceIdHeader = "Trace-Id"

  final case class TraceId(value: String) extends AnyVal

  // TODO: Define a Context maybe instead of just TraceId? Context could carry the Logger instance
  type KFX[F[_], A] = Kleisli[F, TraceId, A]

  def apply[F[_]: Monad](service: HttpService[F])(implicit L: TracerLog[KFX[F, ?]]): HttpService[F] =
    Kleisli[OptionT[F, ?], Request[F], Response[F]] { req =>
      // TODO: Use a more efficient UUID generator
      val traceId = TraceId(UUID.randomUUID().toString)
      val tracedReq = req.putHeaders(Header(TraceIdHeader, traceId.value))

      for {
        _ <- OptionT.liftF(L.info[Tracer.type](s"Performing HTTP Request: ${req.pathInfo}").run(traceId))
        s <- service(tracedReq)
      } yield s
    }

  def getTraceId[F[_]](request: Request[F]): TraceId =
    request.headers.get(TraceIdHeader.ci).map(h => TraceId(h.value)).getOrElse(TraceId("-"))

}
