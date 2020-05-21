/*
 * Copyright 2018-2020 ProfunKtor
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
package http

import cats.effect._
import cats.implicits._
import dev.profunktor.tracer.Trace._
import dev.profunktor.tracer.http.client.TestUserRegistry
import dev.profunktor.tracer.instances.tracer._
import dev.profunktor.tracer.model.user.{User, Username}
import dev.profunktor.tracer.program.UserProgram
import dev.profunktor.tracer.repository.TestUserRepository
import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.dsl.io._

class UserRoutesSpec extends IOSuite {

  private val repo     = new TestUserRepository[Trace[IO, ?]]
  private val registry = new TestUserRegistry[Trace[IO, ?]]
  private val program  = new UserProgram[Trace[IO, ?]](repo, registry)
  private val routes   = new UserRoutes[IO](program).routes

  def testBody(mkReq: IO[Request[IO]], expectedStatus: Status): IO[Unit] =
    mkReq.flatMap(routes(_).value).map {
      case None    => fail("empty response")
      case Some(r) => assert(r.status === expectedStatus)
    }

  test("find user by username") {
    testBody(
      GET(Uri(path = "/users/gvolpe")),
      Status.Ok
    )
  }

  test("not find user by username") {
    testBody(
      GET(Uri(path = "/users/xxx")),
      Status.NotFound
    )
  }

  test("create user") {
    testBody(
      POST(User(Username("xxx")), Uri(path = "/users")),
      Status.Created
    )
  }

  test("fail to create user existent user") {
    testBody(
      POST(User(Username("gvolpe")), Uri(path = "/users")),
      Status.Conflict
    )
  }

}
