---
layout: docs
title:  "Guide"
number: 1
position: 1
---

# Usage Guide

The following example will follow a (recommended) tagless final encoding:

#### User Algebra

```tut:book:silent
final case class Username(value: String) extends AnyVal
final case class User(username: Username)

case class UserAlreadyExists(username: Username) extends Exception(username.value)
case class UserNotFound(username: Username)      extends Exception(username.value)
```

```tut:book:silent
trait UserAlgebra[F[_]] {
  def find(username: Username): F[User]
}

trait UserRepository[F[_]] {
  def find(username: Username): F[Option[User]]
}
```

#### User Program

And implemented in two parts: a `program` that has all the logic:

```tut:book:silent
import cats.MonadError
import cats.implicits._

class UserProgram[F[_]](repo: UserRepository[F])(implicit F: MonadError[F, Throwable]) extends UserAlgebra[F] {

  override def find(username: Username): F[User] = {
    val notFound = F.raiseError[User](UserNotFound(username))
    for {
      mu <- repo.find(username) // F[Option[User]]
      rs <- mu.fold(notFound)(F.pure)
    } yield rs
  }

}
```

#### User Tracer Interpreter

And an `interpreter` that just adds the tracing log part to it, by following a `tagless final` design:

```tut:book:silent
import com.github.gvolpe.tracer.Tracer.KFX
import com.github.gvolpe.tracer._

class UserTracerInterpreter[F[_]](repo: UserRepository[KFX[F, ?]])(implicit F: MonadError[F, Throwable], L: TracerLog[KFX[F, ?]]) extends UserProgram[KFX[F, ?]](repo) {

  override def find(username: Username): KFX[F, User] =
    for {
      _ <- L.info[UserAlgebra[F]](s"Find user by username: ${username.value}")
      u <- super.find(username)
    } yield u

}
```

#### Define your Http Routes

Use `Http4sTracerDsl[F]` and `TracedHttpRoute` instead of `Http4sDsl[F]` and `HttpService` respectively. Eg:

```tut:book:silent
import cats.effect._
import io.circe._
import io.circe.generic.auto._
import io.circe.generic.extras.encoding.UnwrappedEncoder
import org.http4s._
import org.http4s.circe._

class UserRoutes[F[_]: Sync](userService: UserAlgebra[KFX[F, ?]]) extends Http4sTracerDsl[F] {

  implicit def valueClassEncoder[A: UnwrappedEncoder]: Encoder[A] = implicitly
  implicit def jsonEncoder[F[_]: Sync, A <: Product: Encoder]: EntityEncoder[F, A] = jsonEncoderOf[F, A]

  val routes: HttpService[F] = TracedHttpRoute[F] {
    case GET -> Root / username using traceId =>
      userService.find(Username(username)).run(traceId)
        .flatMap(user => Ok(user))
        .handleErrorWith { case UserNotFound(_) => NotFound(username) }
  }

}
```

***For authenticated routes use `Http4sAuthTracerDsl[F]` and `AuthTracedHttpRoute[T, F]` instead.***

### Use the given `Tracer` middleware on your http routes:

```tut:book:invisible
val userService: UserAlgebra[KFX[IO, ?]] = null
```

```tut:book:silent
import com.github.gvolpe.tracer.instances.tracerlog._

val userRoutes: HttpService[IO] = new UserRoutes[IO](userService).routes
val routes: HttpService[IO] = Tracer(userRoutes, headerName = "MyAppId") // Customizable Header name, default "Trace-Id"
```

Notice that an implicit instance of `TracerLog[F]` is needed for `Tracer.apply`. You can either provide your own or just use the default one that uses a `org.slf4j.Logger` instance to log the trace of your application.

### Go and have a rest, your application won't lose track of all its activity :)

Although defining an interpreter in terms of `Kleisli` adds a bit of boilerplate, at the end it pays off. And it's also super easy to test! You can just test your `program` by just using `IO` for example.

Take a look at the [complete example](https://github.com/gvolpe/http4s-tracer/tree/master/examples/src) for more details.

