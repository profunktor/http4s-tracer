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

import cats.effect.IO
import com.github.gvolpe.tracer.Trace._
import com.github.gvolpe.tracer.instances.tracer._
import com.github.gvolpe.tracer.model.user.{User, Username}
import com.github.gvolpe.tracer.program.UserProgram
import com.github.gvolpe.tracer.repository.TestUserRepository
import io.circe.generic.auto._
import org.http4s.Method._
import org.http4s.{Request, Status, Uri}
import org.scalatest.prop.TableFor3

class UserRoutesSpec extends HttpRoutesSpec {

  private val repo    = new TestUserRepository[Trace[IO, ?]]
  private val program = new UserProgram[Trace[IO, ?]](repo)
  private val routes  = new UserRoutes[IO](program).routes

  val requests: TableFor3[String, IO[Request[IO]], Status] = Table(
    ("description", "request", "expectedStatus"),
    ("find user by username", GET(Uri(path = "/users/gvolpe")), Status.Ok),
    ("not find user by username", GET(Uri(path = "/users/xxx")), Status.NotFound),
    ("create user", POST(User(Username("xxx")), Uri(path = "/users")), Status.Created),
    ("fail to create existent user", POST(User(Username("gvolpe")), Uri(path = "/users")), Status.Conflict)
  )

  propertySpec(requests, routes)

}
