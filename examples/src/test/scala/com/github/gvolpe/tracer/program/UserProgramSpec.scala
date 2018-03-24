package com.github.gvolpe.tracer.program

import cats.effect.IO
import com.github.gvolpe.tracer.IOAssertion
import com.github.gvolpe.tracer.model.user.{User, Username}
import com.github.gvolpe.tracer.repository.algebra.UserRepository
import org.scalatest.FunSuite

class UserProgramSpec extends FunSuite {

  private val repo = new UserRepository[IO] {
    override def find(username: Username): IO[Option[User]] = IO {
      if (username.value == "xxx") None else Some(User(username))
    }
    override def persist(user: User): IO[Unit] = IO.unit
  }

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
