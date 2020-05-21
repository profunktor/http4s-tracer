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
package program

import cats.effect._
import cats.implicits._
import dev.profunktor.tracer.http.client.TestUserRegistry
import dev.profunktor.tracer.model.user.{User, Username}
import dev.profunktor.tracer.repository.TestUserRepository

class UserProgramSpec extends IOSuite {

  private val repo     = new TestUserRepository[IO]
  private val registry = new TestUserRegistry[IO]
  private val program  = new UserProgram[IO](repo, registry)

  test("find user by username") {
    val username = Username("gvolpe")
    program.find(username).map { user =>
      assert(user.username.value === username.value)
    }
  }

  test("NOT find user by username") {
    val username = Username("xxx")
    program.find(username).attempt.map { either =>
      assert(either.isLeft)
    }
  }

  test("persist user") {
    val user = User(Username("xxx"))
    program.persist(user).attempt.map { either =>
      assert(either.isRight)
    }
  }

  // This test doesn't really do what it means because of the flaky repository implementation
  test("conflict when persisting same user") {
    val user = User(Username("gvolpe"))
    val rs = for {
      _ <- program.persist(user)
      _ <- program.persist(user)
    } yield ()
    rs.attempt.map { either =>
      assert(either.isLeft)
    }
  }

}
