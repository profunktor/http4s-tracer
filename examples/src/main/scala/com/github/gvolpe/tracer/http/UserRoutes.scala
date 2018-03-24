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

package com.github.gvolpe.tracer.http

import cats.effect.Sync
import cats.syntax.all._
import com.github.gvolpe.tracer.{TraceableHttpRoutes, Tracer}
import com.github.gvolpe.tracer.Tracer.Traceable
import com.github.gvolpe.tracer.model.user.{User, Username}
import com.github.gvolpe.tracer.service._
import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._
import io.circe.generic.extras.decoding.UnwrappedDecoder
import io.circe.generic.extras.encoding.UnwrappedEncoder
import org.http4s._
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl

class UserRoutes[F[_]](userService: UserService[Traceable[F, ?]])(implicit F: Sync[F]) extends Http4sDsl[F] {

  implicit def valueClassEncoder[A: UnwrappedEncoder]: Encoder[A] = implicitly
  implicit def valueClassDecoder[A: UnwrappedDecoder]: Decoder[A] = implicitly

  implicit def jsonDecoder[A <: Product: Decoder]: EntityDecoder[F, A] = jsonOf[F, A]
  implicit def jsonEncoder[A <: Product: Encoder]: EntityEncoder[F, A] = jsonEncoderOf[F, A]

  val routes: HttpService[F] = HttpService[F] {
    case req @ GET -> Root / username =>
      userService
        .find(Username(username))
        .run(Tracer.getTraceId[F](req))
        .flatMap(user => Ok(user))
        .handleErrorWith {
          case UserNotFound(_) => NotFound(username)
        }

    case req @ POST -> Root =>
      req.decode[User] { user =>
        userService
          .persist(user)
          .run(Tracer.getTraceId[F](req))
          .flatMap(_ => Ok())
          .handleErrorWith {
            case UserAlreadyExists(_) => NotFound(user.username.value)
          }
      }
  }

}
