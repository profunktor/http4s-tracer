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

package com.github.gvolpe.tracer.program

import cats.effect.IO
import com.github.gvolpe.tracer.IOAssertion
import com.github.gvolpe.tracer.model.user.{User, Username}
import com.github.gvolpe.tracer.repository.TestUserRepository
import org.scalatest.FunSuite

class UserProgramSpec extends FunSuite {

  private val repo    = new TestUserRepository[IO]
  private val program = new UserProgram[IO](repo)

  test("find user by username") {
    IOAssertion {
      val username = Username("gvolpe")
      program.find(username).map { user =>
        assert(user.username == username)
      }
    }
  }

  test("NOT find user by username") {
    IOAssertion {
      val username = Username("xxx")
      program.find(username).attempt.map { either =>
        assert(either.isLeft)
      }
    }
  }

  test("persist user") {
    IOAssertion {
      val user = User(Username("xxx"))
      program.persist(user).attempt.map { either =>
        assert(either.isRight)
      }
    }
  }

  // This test doesn't really do what it means because of the flaky repository implementation
  test("conflict when persisting same user") {
    IOAssertion {
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

}
