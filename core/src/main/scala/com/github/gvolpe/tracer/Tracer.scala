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

import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import com.gilt.timeuuid.TimeUuid
import org.http4s.syntax.StringSyntax
import org.http4s.{Header, HttpService, Request, Response}

/**
  * An `org.http4s.server.HttpMiddleware` that adds a Trace-Id header with a unique Time-based UUID
  * value and logs the http request and http response with it.
  *
  * Quite useful to trace the flow of each request. For example:
  *
  * TraceId(72b079c8-fc92-4c4f-aa5a-c0cd91ea221c) >> Request(method=GET, uri=/users, ...)
  * TraceId(72b079c8-fc92-4c4f-aa5a-c0cd91ea221c) >> UserAlgebra requesting users
  * TraceId(72b079c8-fc92-4c4f-aa5a-c0cd91ea221c) >> UserRepository fetching users from DB
  * TraceId(72b079c8-fc92-4c4f-aa5a-c0cd91ea221c) >> MetricsService saving users metrics
  * TraceId(72b079c8-fc92-4c4f-aa5a-c0cd91ea221c) >> Response(status=200, ...)
  *
  * In a normal application, you will have thousands of requests and tracing the call chain in
  * a failure scenario will be invaluable.
  * */
object Tracer extends StringSyntax {

  private val TraceIdHeader = "Trace-Id"

  final case class TraceId(value: String) extends AnyVal

  type KFX[F[_], A] = Kleisli[F, TraceId, A]

  def apply[F[_]](service: HttpService[F])(implicit F: Sync[F], L: TracerLog[KFX[F, ?]]): HttpService[F] =
    Kleisli[OptionT[F, ?], Request[F], Response[F]] { req =>
      for {
        id <- OptionT.liftF(F.delay(TraceId(TimeUuid().toString)))
        tr <- OptionT.liftF(F.delay(req.putHeaders(Header(TraceIdHeader, id.value))))
        _  <- OptionT.liftF(L.info[Tracer.type](s"$req").run(id))
        rs <- service(tr)
        _  <- OptionT.liftF(L.info[Tracer.type](s"$rs").run(id))
      } yield rs
    }

  def getTraceId[F[_]](request: Request[F]): TraceId =
    request.headers.get(TraceIdHeader.ci).map(h => TraceId(h.value)).getOrElse(TraceId("-"))

}
