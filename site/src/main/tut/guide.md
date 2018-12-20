---
layout: docs
title:  "Guide"
number: 3
position: 3
---

# A guide to a complete application using Http4s Tracer

The following example will follow a (recommended) [tagless final](http://okmij.org/ftp/tagless-final/index.html) encoding:

<nav role="navigation" id="toc"></nav>

### Tagless final application

#### Domain model & Errors

```tut:book:silent
final case class Username(value: String) extends AnyVal
final case class User(username: Username)

sealed trait UserError extends Exception
case class UserAlreadyExists(username: Username) extends UserError
case class UserNotFound(username: Username)      extends UserError
```

#### Algebras

Also known as interfaces, they define the functionality we want to expose and we will only operate in terms of these definitions:

```tut:book:silent
trait UserAlgebra[F[_]] {
  def find(username: Username): F[User]
  def persist(user: User): F[Unit]
}

trait UserRepository[F[_]] {
  def find(username: Username): F[Option[User]]
  def persist(user: User): F[Unit]
}
```

#### Programs

Contains pure logic. It can possiby combine multiple algebras as well as other programs but without commiting to a specific implementation:

```tut:book:silent
import cats.MonadError
import cats.implicits._

class UserProgram[F[_]](repo: UserRepository[F])(implicit F: MonadError[F, Throwable]) extends UserAlgebra[F] {

  override def find(username: Username): F[User] =
    for {
      mu <- repo.find(username)
      rs <- mu.fold(F.raiseError[User](UserNotFound(username)))(F.pure)
    } yield rs

  override def persist(user: User): F[Unit] =
    for {
      mu <- repo.find(user.username)
      rs <- mu.fold(repo.persist(user))(_ => F.raiseError(UserAlreadyExists(user.username)))
    } yield rs

}
```

#### Interpreters

In this case we will only have a single interpreter for our `Repository`: an in-memory implementation based on `Ref`.

```tut:book:silent
import cats.effect._
import cats.effect.concurrent.Ref

class MemUserRepository[F[_]: Sync] (
    state: Ref[F, Map[Username, User]]
) extends UserRepository[F] {

  override def find(username: Username): F[Option[User]] =
    state.get.map(_.get(username))

  override def persist(user: User): F[Unit] =
    state.update(_.updated(user.username, user))

}

object MemUserRepository {
  def create[F[_]: Sync]: F[UserRepository[F]] =
    Ref.of[F, Map[Username, User]](Map.empty).map(new MemUserRepository[F](_))
}
```

### Distributed Tracing

Note that until here we have only defined `algebras`, `programs` and `interpreters` that can easily be tested in isolation. No tracing concepts so far. And as mentioned in the overview section, the `HttpRoutes` should only be aware of it but we'll also need some tracer interpreters and we will soon see what this means.

#### Http Routes

We need to defined some codecs first that can be used by all our `HttpRoutes`:

```tut:book:silent
import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.decoding.UnwrappedDecoder
import io.circe.generic.extras.encoding.UnwrappedEncoder
import org.http4s._
import org.http4s.circe._

implicit def valueClassEncoder[A: UnwrappedEncoder]: Encoder[A] = implicitly
implicit def valueClassDecoder[A: UnwrappedDecoder]: Decoder[A] = implicitly

implicit def jsonDecoder[F[_]: Sync, A <: Product: Decoder]: EntityDecoder[F, A] = jsonOf[F, A]
implicit def jsonEncoder[F[_]: Sync, A <: Product: Encoder]: EntityEncoder[F, A] = jsonEncoderOf[F, A]
```

Use `Http4sTracerDsl[F]` and `TracedHttpRoute` instead of `Http4sDsl[F]` and `HttpRoutes.of[F]` respectively.

*For authenticated routes use `Http4sAuthTracerDsl[F]` and `AuthTracedHttpRoute[T, F]` instead.*

```tut:book:silent
import com.github.gvolpe.tracer.Trace._
import com.github.gvolpe.tracer.{Http4sTracerDsl, TracedHttpRoute, Tracer}
import io.circe.generic.auto._
import org.http4s.server.Router

class UserRoutes[F[_]: Sync: Tracer](users: UserAlgebra[Trace[F, ?]]) extends Http4sTracerDsl[F] {

  private val PathPrefix = "/users"

  private val httpRoutes: HttpRoutes[F] = TracedHttpRoute[F] {
    case GET -> Root / username using traceId =>
      users
        .find(Username(username))
        .run(traceId)
        .flatMap(user => Ok(user))
        .handleErrorWith {
          case UserNotFound(u) => NotFound(u.value)
        }

    case tr @ POST -> Root using traceId =>
      tr.request.decode[User] { user =>
        users
          .persist(user)
          .run(traceId)
          .flatMap(_ => Created())
          .handleErrorWith {
             case UserAlreadyExists(u) => Conflict(u.value)
          }
      }
  }

  val routes: HttpRoutes[F] = Router(
    PathPrefix -> httpRoutes
  )

}
```

There are a couple of things going on here:

- We require an `UserAlgebra[Trace[F, ?]]` instead of a plain `UserAlgebra[F]`.
- We need an instance of `Tracer[F]` in scope to make sure we are getting the right header.

This is necessary to pass the "Trace-Id" along and we'll soon see how to do it.

### Modules

The recommended way to structure tagless final applications is to group things in different modules. For this simple application we will define three modules: `HttpApi`, `Programs` and `Repositories`. In a larger application we might have more modules such as `HttpClients`, etc. The idea will remain the same.

#### Programs module

This module is going to be the "master algebra" that groups all the single algebras.

```tut:book:silent
trait Programs[F[_]] {
  def users: UserAlgebra[F]
}
```

#### Repositories module

In the same way, this is the "master algebra of repositories". And in addition, we provide a way of creating the in-memory interpreter since its creation is effectful.

```tut:book:silent
trait Repositories[F[_]] {
  def users: UserRepository[F]
}

class LiveRepositories[F[_]](usersRepo: UserRepository[F]) extends Repositories[F] {
  val users: UserRepository[F] = usersRepo
}

object LiveRepositories {
  def apply[F[_]: Sync]: F[Repositories[F]] =
    MemUserRepository.create[F].map(new LiveRepositories[F](_))
}
```

#### HttpApi module

Finally, here we define our `HttpRoutes` and middlewares (including tracing).

```tut:book:silent
import com.github.gvolpe.tracer.Trace.Trace
import com.github.gvolpe.tracer.{Tracer, TracerLog}
import org.http4s.implicits._
import org.http4s.{HttpApp, HttpRoutes}

class HttpApi[F[_]: Sync: Tracer](programs: Programs[Trace[F, ?]])(implicit L: TracerLog[Trace[F, ?]]) {

  private val httpRoutes: HttpRoutes[F] =
    new UserRoutes[F](programs.users).routes

  val httpApp: HttpApp[F] =
    Tracer[F].middleware(httpRoutes.orNotFound)

}
```

Note that we again require a `Programs[Trace[F, ?]]` instead of just `Programs[F]` and also an instance of `TracerLog[Trace[F, ?]]` required by `Tracer[F].middleware`.

### Tracers

Now that we have defined our program, http routes and modules it's time to introduce the "tracers". These are just interpreters with tracing capabilities written on top of our "group modules". Let's see the code.

#### Traced Programs

We extend `Programs` with our effect type being `Trace[F, ?]` and define all the different algebras.

```tut:book:silent
import com.github.gvolpe.tracer.Trace
import com.github.gvolpe.tracer.Trace.Trace
import com.github.gvolpe.tracer.TracerLog

class UserTracer[F[_]: Sync](repo: UserRepository[Trace[F, ?]])(implicit L: TracerLog[Trace[F, ?]]) extends UserProgram[Trace[F, ?]](repo) {

  override def find(username: Username): Trace[F, User] =
    for {
      _ <- L.info[UserAlgebra[F]](s"Find user by username: ${username.value}")
      u <- super.find(username)
    } yield u

  override def persist(user: User): Trace[F, Unit] =
    for {
      _  <- L.info[UserAlgebra[F]](s"About to persist user: ${user.username.value}")
      rs <- super.persist(user)
    } yield rs

}

class TracedPrograms[F[_]: Sync](repos: Repositories[Trace[F, ?]])(implicit L: TracerLog[Trace[F, ?]]) extends Programs[Trace[F, ?]] {
  override val users: UserAlgebra[Trace[F, ?]] = new UserTracer[F](repos.users)
}
```

In this case we only have one, so we just extend `UserProgram` and write tracing logs on top of it. This is just an
interpreter that adds tracing capabilities on top of the original program, highly decoupled.

#### Traced Repositories

Again we extend `Repositories[Trace[F, ?]]` and provide the necessary tracing interpreters.

```tut:book:silent
import cats.FlatMap

class UserTracerRepository[F[_]: FlatMap](repo: UserRepository[F])(implicit L: TracerLog[Trace[F, ?]]) extends UserRepository[Trace[F, ?]] {

  override def find(username: Username): Trace[F, Option[User]] =
    for {
      _ <- L.info[UserRepository[F]](s"Find user by username: ${username.value}")
      u <- Trace(_ => repo.find(username))
    } yield u

  override def persist(user: User): Trace[F, Unit] =
    for {
      _ <- L.info[UserRepository[F]](s"Persisting user: ${user.username.value}")
      _ <- Trace(_ => repo.persist(user))
    } yield ()

}

class TracedRepositories[F[_]: FlatMap](repos: Repositories[F])(implicit L: TracerLog[Trace[F, ?]]) extends Repositories[Trace[F, ?]] {
  val users: UserRepository[Trace[F, ?]] = new UserTracerRepository[F](repos.users)
}
```

Writing these tracers is the most tedious part as we need to write quite some boilerplate but this is the trade-off for getting nice distributed tracing logs.

### Putting all the pieces together

#### Main entry point

This is where we instantiate our modules and create our `Tracer` instance. For a default instance with header name "Trace-Id" just use `import com.github.gvolpe.tracer.instances.tracer._`.

```tut:book:silent
import com.github.gvolpe.tracer.instances.tracerlog._
import org.http4s.server.blaze.BlazeServerBuilder

class Main[F[_]: ConcurrentEffect: Timer] {

  implicit val tracer: Tracer[F] = Tracer.create[F]("Flow-Id")

  val server: F[Unit] =
    LiveRepositories[F].flatMap { repositories =>
      val tracedRepos    = new TracedRepositories[F](repositories)
      val tracedPrograms = new TracedPrograms[F](tracedRepos)
      val httpApi        = new HttpApi[F](tracedPrograms)

      BlazeServerBuilder[F]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(httpApi.httpApp)
        .serve
        .compile
        .drain
    }

}
```

#### Logger

Note that we can get a default instance of `TracerLog` if our effect type has an instance of `Sync` by a single import.

If you are a [log4cats](https://christopherdavenport.github.io/log4cats/) user we can derive a `TracerLog` instance if you provide a `Logger` instance. All you have to do is to import `com.github.gvolpe.tracer.log4cats._` and add the extra dependency `http4s-tracer-log4cats`. See the [Log4CatsServer](https://github.com/gvolpe/http4s-tracer/blob/master/examples/src/main/scala/com/github/gvolpe/tracer/Log4CatsServer.scala) example for more.

#### Choose your effect type!

Here we are going to be using `cats.effect.IO` but you could use your own effect.

```tut:book:silent
object Server extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    new Main[IO].server.as(ExitCode.Success)

}
```

### Running the application

This is how the activity log might look like for a simple POST request:

```bash
4:13:48.985 [scala-execution-context-global-17] INFO com.github.gvolpe.tracer.Tracer$ - [TraceId] - [02594e59-4b21-4d0a-aad5-5866a632fbb5] - Request(method=POST, uri=/users, headers=Headers(HOST: localhost:8080, content-type: application/json, content-length: 30))
14:13:49.282 [scala-execution-context-global-17] INFO com.github.gvolpe.tracer.algebra$UserAlgebra - [TraceId] - [02594e59-4b21-4d0a-aad5-5866a632fbb5] - About to persist user: modersky
14:13:49.290 [scala-execution-context-global-17] INFO com.github.gvolpe.tracer.repository.algebra$UserRepository - [TraceId] - [02594e59-4b21-4d0a-aad5-5866a632fbb5] - Find user by username: modersky
14:13:49.298 [scala-execution-context-global-17] INFO com.github.gvolpe.tracer.repository.algebra$UserRepository - [TraceId] - [02594e59-4b21-4d0a-aad5-5866a632fbb5] - Persisting user: modersky
14:13:49.315 [scala-execution-context-global-17] INFO com.github.gvolpe.tracer.Tracer$ - [TraceId] - [02594e59-4b21-4d0a-aad5-5866a632fbb5] - Response(status=201, headers=Headers(Content-Length: 0, Trace-Id: 02594e59-4b21-4d0a-aad5-5866a632fbb5))
```

Quite useful to trace the flow of your application starting out at each request. In a normal application, you will have thousands of requests and tracing the call-chain in a failure scenario will be invaluable.

### Source code

This documentation is compiled with [tut](http://tpolecat.github.io/tut/) to guarantee it's always updated. However, it is always easier to have a project you can import and easily run so we've got you covered!

Find the source code in the [examples module](https://github.com/gvolpe/http4s-tracer/tree/master/examples/src).

