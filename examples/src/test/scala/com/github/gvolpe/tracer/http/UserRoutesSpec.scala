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

package dev.profunktor.tracer.http

import cats.effect.{ContextShift, IO}
import dev.profunktor.tracer.Trace._
import dev.profunktor.tracer.http.client.TestUserRegistry
import dev.profunktor.tracer.instances.tracer._
import dev.profunktor.tracer.model.user.{User, Username}
import dev.profunktor.tracer.program.UserProgram
import dev.profunktor.tracer.repository.TestUserRepository
import org.http4s.Method._
import org.http4s.{Request, Status, Uri}
import org.scalatest.prop.TableFor3

import scala.concurrent.ExecutionContext

class UserRoutesSpec extends HttpRoutesSpec {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  private val repo     = new TestUserRepository[Trace[IO, ?]]
  private val registry = new TestUserRegistry[Trace[IO, ?]]
  private val program  = new UserProgram[Trace[IO, ?]](repo, registry)
  private val routes   = new UserRoutes[IO](program).routes

  val requests: TableFor3[String, IO[Request[IO]], Status] = Table(
    ("description", "request", "expectedStatus"),
    ("find user by username", GET(Uri(path = "/users/gvolpe")), Status.Ok),
    ("not find user by username", GET(Uri(path = "/users/xxx")), Status.NotFound),
    ("create user", POST(User(Username("xxx")), Uri(path = "/users")), Status.Created),
    ("fail to create existent user", POST(User(Username("gvolpe")), Uri(path = "/users")), Status.Conflict)
  )

  propertySpec(requests, routes)

}
