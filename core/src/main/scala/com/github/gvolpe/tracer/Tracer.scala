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

import cats.Applicative
import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import cats.syntax.all._
import com.gilt.timeuuid.TimeUuid
import org.http4s.syntax.StringSyntax
import org.http4s.{Header, HttpRoutes, Request, Response}

/**
  * `org.http4s.server.HttpMiddleware` that either tries to get a Trace-Id from the headers or otherwise
  * creates one with a unique Time-based UUID value, adds it to the headers and logs the http request and
  * http response with it.
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

  private val DefaultTraceIdHeader = "Trace-Id"
  private var TraceIdHeader        = DefaultTraceIdHeader

  final case class TraceId(value: String) extends AnyVal

  type KFX[F[_], A] = Kleisli[F, TraceId, A]

  // format: off
  def apply[F[_]](service: HttpRoutes[F], headerName: String = DefaultTraceIdHeader)
                 (implicit F: Sync[F], L: TracerLog[KFX[F, ?]]): HttpRoutes[F] =
    Kleisli[OptionT[F, ?], Request[F], Response[F]] { req =>
      val createId: F[(Request[F], TraceId)] =
        for {
          id <- F.delay(TraceId(TimeUuid().toString))
          tr <- F.delay(req.putHeaders(Header(TraceIdHeader, id.value)))
        } yield (tr, id)

      for {
        _        <- OptionT.liftF(F.delay(TraceIdHeader = headerName))
        mi       <- OptionT.liftF(getTraceId(req))
        (tr, id) <- mi.fold(OptionT.liftF(createId)){ id => OptionT.liftF((req, id).pure[F]) }
        _        <- OptionT.liftF(L.info[Tracer.type](s"$req").run(id))
        rs       <- service(tr).map(_.putHeaders(Header(TraceIdHeader, id.value)))
        _        <- OptionT.liftF(L.info[Tracer.type](s"$rs").run(id))
      } yield rs
    }

  def getTraceId[F[_]: Applicative](request: Request[F]): F[Option[TraceId]] =
    request.headers.get(TraceIdHeader.ci).map(h => TraceId(h.value)).pure[F]

}
