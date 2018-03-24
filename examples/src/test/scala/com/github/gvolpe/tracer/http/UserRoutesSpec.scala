package com.github.gvolpe.tracer.http

import cats.effect.IO
import com.github.gvolpe.tracer.model.user.{User, Username}
import com.github.gvolpe.tracer.program.UserProgram
import com.github.gvolpe.tracer.repository.algebra.UserRepository
import org.scalatest.FunSuite
import org.scalatest.prop.PropertyChecks
import org.http4s.Method._
import org.http4s.{Status, Uri}
import org.http4s.client.dsl.io._

class UserRoutesSpec extends FunSuite with PropertyChecks {

  private val repo = new UserRepository[IO] {
    override def find(username: Username): IO[Option[User]] = IO {
      if (username.value == "xxx") None else Some(User(username))
    }
    override def persist(user: User): IO[Unit] = IO.unit
  }

  private val program = new UserProgram[IO](repo)

  program.find(Username("gvolpe"))

  // FIXME and now what???
  //private val routes = new UserRoutes[IO](program)

  val requests = Table(
    ("description", "request", "expectedStatus"),
    ("find user by username", GET(Uri(path = "/users/gvolpe")), Status.Ok),
    ("create user", POST(Uri(path = "/users")), Status.Created)
  )

}
