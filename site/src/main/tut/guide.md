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

trait UserRegistry[F[_]] {
  def register(user: User): F[Unit]
}
```

#### Programs

Contains pure logic. It can possiby combine multiple algebras as well as other programs but without commiting to a specific implementation:

```tut:book:silent
import cats.{MonadError, Parallel}
import cats.implicits._

class UserProgram[F[_]: Parallel](repo: UserRepository[F], registry: UserRegistry[F])(implicit F: MonadError[F, Throwable]) extends UserAlgebra[F] {

  def find(username: Username): F[User] =
    repo.find(username).flatMap {
      case Some(u) => F.pure(u)
      case None    => F.raiseError(UserNotFound(username))
    }

  def persist(user: User): F[Unit] =
    repo.find(user.username).flatMap {
      case Some(_) => F.raiseError(UserAlreadyExists(user.username))
      case None    => (registry.register(user), repo.persist(user)).parTupled.void
    }

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

And an interpreter for our `UserRegistry` which calls an external http service. But first we need to define some Json codecs that will also be used by all our `HttpRoutes`:

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

Here's our interpreter for `UserRegistry`:

```tut:book:silent
import io.circe.syntax._
import org.http4s.Method._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl

final case class LiveUserRegistry[F[_]: Sync](client: Client[F]) extends UserRegistry[F]  with Http4sClientDsl[F] {

  private val uri = Uri.uri("https://jsonplaceholder.typicode.com/posts")

  def register(user: User): F[Unit] =
    client.successful(POST(user.asJson, uri)).void
}

```

### Distributed Tracing

Note that until here we have only defined `algebras`, `programs` and `interpreters` that can easily be tested in isolation. Neither tracing nor logging concepts so far. And as mentioned in the overview section, the `HttpRoutes` should only be aware of it but we'll also need some tracer interpreters and we will soon see what this means.

#### Http Routes

Use `Http4sTracerDsl[F]` and `TracedHttpRoute` instead of `Http4sDsl[F]` and `HttpRoutes.of[F]` respectively.

*For authenticated routes use `Http4sAuthTracerDsl[F]` and `AuthTracedHttpRoute[T, F]` instead.*

```tut:book:silent
import dev.profunktor.tracer.Trace._
import dev.profunktor.tracer.{Http4sTracerDsl, TracedHttpRoute, Tracer}
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

The recommended way to structure tagless final applications is to group things in different modules. For this simple application we will define four modules: `HttpApi`, `HttpClients`, `Programs` and `Repositories`. In a larger application we might have more modules but the idea remains the same.

#### Repositories module

This is the "master algebra of repositories". And in addition, we provide a way of creating the in-memory interpreter since its creation is effectful.

```tut:book:silent
trait Repositories[F[_]] {
  def users: UserRepository[F]
}

final class LiveRepositories[F[_]](usersRepo: UserRepository[F]) extends Repositories[F] {
  val users: UserRepository[F] = usersRepo
}

object LiveRepositories {
  def apply[F[_]: Sync]: F[Repositories[F]] =
    MemUserRepository.create[F].map(new LiveRepositories[F](_))
}
```

#### Http Clients module

The master algebra of the http clients.

```tut:book:silent
trait HttpClients[F[_]] {
  def userRegistry: UserRegistry[F]
}

final case class LiveHttpClients[F[_]: Sync](client: Client[F]) extends HttpClients[F] {
  def userRegistry: UserRegistry[F] = LiveUserRegistry[F](client)
}
```

#### Programs module

This module is going to be the "master algebra" that groups all the single algebras.

```tut:book:silent
trait Programs[F[_]] {
  def users: UserAlgebra[F]
}

final case class LivePrograms[F[_]: Parallel: Sync](repos: Repositories[F], clients: HttpClients[F]) extends Programs[F] {
  def users: UserAlgebra[F] = new UserProgram[F](repos.users, clients.userRegistry)
}
```

#### HttpApi module

Before we look into the `HttpApi` module let's look into the `middleware` signature:

```scala
def middleware(
  http: HttpApp[F],
  logRequest: Boolean = false,
  logResponse: Boolean = false
)(implicit F: Sync[F], L: TracerLog[Trace[F, ?]]): HttpApp[F] = ???
```

You can change the default values of the boolean flags if you want to have the request and/or response logged. If you want both activated there's a another constructor provided by the library:

```scala
def loggingMiddleware(
    http: HttpApp[F]
)(implicit F: Sync[F], L: TracerLog[Trace[F, ?]]): HttpApp[F] =
  middleware(http, logRequest = true, logResponse = true)
```

Finally, here we define our `HttpRoutes` and tracing middleware.

```tut:book:silent
import dev.profunktor.tracer.Trace.Trace
import dev.profunktor.tracer.{Tracer, TracerLog}
import org.http4s.implicits._
import org.http4s.{HttpApp, HttpRoutes}

final case class HttpApi[F[_]: Sync: Tracer](programs: Programs[Trace[F, ?]])(implicit L: TracerLog[Trace[F, ?]]) {

  private val httpRoutes: HttpRoutes[F] =
    new UserRoutes[F](programs.users).routes

  val httpApp: HttpApp[F] =
    Tracer[F].middleware(httpRoutes.orNotFound)

}
```

Note that we again require a `Programs[Trace[F, ?]]` instead of just `Programs[F]` and also an instance of `TracerLog[Trace[F, ?]]` required by `Tracer[F].middleware`.

### Tracers

Now that we have defined our program, http routes and modules it's time to introduce the "tracers". These are just interpreters with tracing and logging capabilities written on top of our "group modules". Let's see the code.

#### Traced Repositories

We extend `Repositories[Trace[F, ?]]` (notice the change in the effect type), receive `Repositories[F]` as a parameter and provide the necessary tracing interpreters.

```tut:book:silent
import cats.FlatMap
import dev.profunktor.tracer.Trace
import dev.profunktor.tracer.Trace.Trace
import dev.profunktor.tracer.TracerLog

final class UserTracerRepository[F[_]: FlatMap](repo: UserRepository[F])(implicit L: TracerLog[Trace[F, ?]]) extends UserRepository[Trace[F, ?]] {

  override def find(username: Username): Trace[F, Option[User]] =
    L.info[UserRepository[F]](s"Find user by username: ${username.value}") *>
      Trace(_ => repo.find(username))

  override def persist(user: User): Trace[F, Unit] =
    L.info[UserRepository[F]](s"Persisting user: ${user.username.value}") *>
      Trace(_ => repo.persist(user))

}

case class TracedRepositories[F[_]: FlatMap](repos: Repositories[F])(implicit L: TracerLog[Trace[F, ?]]) extends Repositories[Trace[F, ?]] {
  val users: UserRepository[Trace[F, ?]] = new UserTracerRepository[F](repos.users)
}
```

#### Traced Http Clients

Again we extend `HttpClients[Trace[F, ?]]` and receive `Client[F]` as a parameter:

```tut:book:silent
final class TracedUserRegistry[F[_]: Sync](registry: UserRegistry[F])(implicit L: TracerLog[Trace[F, ?]]) extends UserRegistry[Trace[F, ?]] {

  override def register(user: User): Trace[F, Unit] =
    L.info[UserRegistry[F]](s"Registering user: ${user.username.value}") *>
      Trace(_ => registry.register(user))

}

case class TracedHttpClients[F[_]: Sync] (client: Client[F])(implicit L: TracerLog[Trace[F, ?]]) extends HttpClients[Trace[F, ?]] {
  private val clients = LiveHttpClients[F](client)

  override val userRegistry: UserRegistry[Trace[F, ?]] = new TracedUserRegistry[F](clients.userRegistry)
}
```

#### Traced Programs

We again extend `Programs[Trace[F, ?]]` but in this case we receive other tracer interpreters as parameters:

```tut:book:silent
final class UserTracer[F[_]: Sync](users: UserAlgebra[Trace[F, ?]])(implicit L: TracerLog[Trace[F, ?]]) extends UserAlgebra[Trace[F, ?]] {

  override def find(username: Username): Trace[F, User] =
    L.info[UserAlgebra[F]](s"Find user by username: ${username.value}") *> users.find(username)

  override def persist(user: User): Trace[F, Unit] =
    L.info[UserAlgebra[F]](s"About to persist user: ${user.username.value}") *> users.persist(user)

}

case class TracedPrograms[F[_]: Parallel: Sync](repos: TracedRepositories[F], clients: TracedHttpClients[F])(implicit L: TracerLog[Trace[F, ?]]) extends Programs[Trace[F, ?]] {
  private val programs = LivePrograms[Trace[F, ?]](repos, clients)

  override val users: UserAlgebra[Trace[F, ?]] = new UserTracer[F](programs.users)
}
```

You might have noticed that the approach used in `TracedPrograms` is different from the one in `TracedHttpClients` and `TracedRepositories`. The reason is that the last two are at the bottom of the graph so they can be created based on a simple effectful interpreter `F` whereas `TracedPrograms` is one level up and needs to have the tracer instances of such components.

Writing these tracers is the most tedious part as we need to write quite some boilerplate but this is the trade-off for getting nice distributed tracing logs.

### Putting all the pieces together

#### Main entry point

This is where we instantiate our modules and create our `Tracer` instance. For a default instance with header name "Trace-Id" just use `import dev.profunktor.tracer.instances.tracer._`.

```tut:book:silent
import dev.profunktor.tracer.instances.tracer._
import dev.profunktor.tracer.instances.tracerlog._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder
import scala.concurrent.ExecutionContext

class Main[F[_]: ConcurrentEffect: Parallel: Timer] {

  val server: F[Unit] =
    BlazeClientBuilder[F](ExecutionContext.global).resource.use { client =>
      for {
        repos          <- LiveRepositories[F]
        tracedRepos    = TracedRepositories[F](repos)
        tracedClients  = TracedHttpClients[F](client)
        tracedPrograms = TracedPrograms[F](tracedRepos, tracedClients)
        httpApi        = HttpApi[F](tracedPrograms)
        _ <- BlazeServerBuilder[F]
              .bindHttp(8080, "0.0.0.0")
              .withHttpApp(httpApi.httpApp)
              .serve
              .compile
              .drain
      } yield ()
    }

}
```

#### Logger

Note that we can get a default instance of `TracerLog` if our effect type has an instance of `Sync` by a single import.

If you are a [log4cats](https://christopherdavenport.github.io/log4cats/) user we can derive a `TracerLog` instance if you provide a `Logger` instance. All you have to do is to import `dev.profunktor.tracer.log4cats._` and add the extra dependency `http4s-tracer-log4cats`. See the [Log4CatsServer](https://github.com/gvolpe/http4s-tracer/blob/master/examples/src/main/scala/dev.profunktor/tracer/Log4CatsServer.scala) example for more.

#### Choose your effect type!

Here we are going to be using `cats.effect.IO` but you could use your own effect.

```tut:book:silent
object Server extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    new Main[IO].server.as(ExitCode.Success)

}
```

### Running the application

This is how the activity log might look like for a simple POST request (logging activated):

```bash
18:02:25.366 [blaze-selector-0-2] INFO  o.h.b.c.nio1.NIO1SocketServerGroup - Accepted connection from /0:0:0:0:0:0:0:1:58284
18:02:25.375 [ec-1] INFO  dev.profunktor.tracer.Tracer - [Trace-Id] - [6cb069c0-2792-11e9-9038-b9bcfc32f88f] - Request(method=POST, uri=/users, headers=Headers(HOST: localhost:8080, content-type: application/json, content-length: 8))
18:02:25.527 [ec-1] INFO  d.p.tracer.algebra.UserAlgebra - [Trace-Id] - [6cb069c0-2792-11e9-9038-b9bcfc32f88f] - About to persist user: gvolpe
18:02:25.527 [ec-1] INFO  d.p.t.r.algebra$UserRepository - [Trace-Id] - [6cb069c0-2792-11e9-9038-b9bcfc32f88f] - Find user by username: gvolpe
18:02:25.540 [ec-1] INFO  d.p.t.http.client.UserRegistry - [Trace-Id] - [6cb069c0-2792-11e9-9038-b9bcfc32f88f] - Registering user: gvolpe
18:02:25.540 [ec-1] INFO  d.p.t.r.algebra$UserRepository - [Trace-Id] - [6cb069c0-2792-11e9-9038-b9bcfc32f88f] - Persisting user: gvolpe
18:02:26.601 [ec-1] INFO  dev.profunktor.tracer.Tracer - [Trace-Id] - [6cb069c0-2792-11e9-9038-b9bcfc32f88f] - Response(status=201, headers=Headers(Content-Length: 0, Flow-Id: 6cb069c0-2792-11e9-9038-b9bcfc32f88f))
```

Quite useful to trace the flow of your application starting out at each request. In a normal application, you will have thousands of requests and tracing the call-chain in a failure scenario will be invaluable.

### Source code

This documentation is compiled with [tut](http://tpolecat.github.io/tut/) to guarantee it's always updated. However, it is always easier to have a project you can import and easily run so we've got you covered!

Find the source code in the [examples module](https://github.com/gvolpe/http4s-tracer/tree/master/examples/src).

