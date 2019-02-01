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

package com.github.gvolpe.tracer.auth

import cats.Monad
import cats.data.{Kleisli, OptionT}
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.github.gvolpe.tracer.Tracer
import com.github.gvolpe.tracer.Tracer.TraceId
import org.http4s.{AuthedRequest, AuthedService, Response}

object AuthTracedHttpRoute {
  case class AuthTracedRequest[F[_], T](traceId: TraceId, request: AuthedRequest[F, T])

  def apply[T, F[_]: Monad: Tracer](
      pf: PartialFunction[AuthTracedRequest[F, T], F[Response[F]]]
  ): AuthedService[T, F] =
    Kleisli[OptionT[F, ?], AuthedRequest[F, T], Response[F]] { req =>
      OptionT {
        Tracer[F]
          .getTraceId(req.req)
          .map(x => AuthTracedRequest[F, T](x.getOrElse(TraceId("-")), req))
          .flatMap { tr =>
            val rs: OptionT[F, Response[F]] = pf.andThen(OptionT.liftF(_)).applyOrElse(tr, Function.const(OptionT.none))
            rs.value
          }
      }
    }
}
