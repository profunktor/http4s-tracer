http4s-tracer
=============

[![Build Status](https://travis-ci.org/gvolpe/http4s-tracer.svg?branch=master)](https://travis-ci.org/gvolpe/http4s-tracer)
[![Latest version](https://index.scala-lang.org/gvolpe/http4s-tracer/http4s-tracer/latest.svg?color=orange)](https://index.scala-lang.org/gvolpe/http4s-tracer/http4s-tracer)

It provides an `HttpMiddleware` that adds a `Trace-Id` header (name can be customized) with a unique `UUID` value and gives you an implicit `TracerLog` for any `F[_]: Sync]` that also logs the http request and http response with it.

Quite useful to trace the flow of your application starting out at each request. For example, given a `UsersHttpRoutes`, `UserAlgebra` and `UserRepository` you'll get an activity log like the following when trying to create a user:

```bash
4:13:48.985 [scala-execution-context-global-17] INFO com.github.gvolpe.tracer.Tracer$ - TraceId(02594e59-4b21-4d0a-aad5-5866a632fbb5) >> Request(method=POST, uri=/users, headers=Headers(HOST: localhost:8080, content-type: application/json, content-length: 30))
14:13:49.282 [scala-execution-context-global-17] INFO com.github.gvolpe.tracer.algebra$UserAlgebra - TraceId(02594e59-4b21-4d0a-aad5-5866a632fbb5) >> About to persist user: modersky
14:13:49.290 [scala-execution-context-global-17] INFO com.github.gvolpe.tracer.repository.algebra$UserRepository - TraceId(02594e59-4b21-4d0a-aad5-5866a632fbb5) >> Find user by username: modersky
14:13:49.298 [scala-execution-context-global-17] INFO com.github.gvolpe.tracer.repository.algebra$UserRepository - TraceId(02594e59-4b21-4d0a-aad5-5866a632fbb5) >> Persisting user: modersky
14:13:49.315 [scala-execution-context-global-17] INFO com.github.gvolpe.tracer.Tracer$ - TraceId(02594e59-4b21-4d0a-aad5-5866a632fbb5) >> Response(status=201, headers=Headers(Content-Length: 0))
```

In a normal application, you will have thousands of requests and tracing the call chain in a failure scenario will be invaluable.

### Dependencies

Add this to your `build.sbt`:

```
libraryDependencies += "com.github.gvolpe" %% "http4s-tracer" % "0.1"
```

`http4s-tracer` has the following dependencies:

| Dependency   | Version    |
| ------------ |:----------:|
| cats         | 1.1.1      |
| cats-effect  | 0.10.1     |
| fs2          | 0.10.4     |
| gfc-timeuuid | 0.0.8      |
| http4s       | 0.18.12    |

### Usage Guide

#### Define your Http Routes

Use `Http4sTracerDsl[F]` and `TracedHttpRoute` instead of `Http4sDsl[F]` and `HttpService` respectively. Eg:

```scala
class UserRoutes[F[_]: Sync](userService: UserAlgebra[KFX[F, ?]]) extends Http4sTracerDsl[F] {

  val routes: HttpService[F] = TracedHttpRoute[F] {
    case GET -> Root / username using traceId =>
      userService.find(Username(username)).run(traceId)
        .flatMap(user => Ok(user))
        .handleErrorWith { case UserNotFound(_) => NotFound(username) }
  }

}
```

***For authenticated routes use `Http4sAuthTracerDsl[F]` and `AuthTracedHttpRoute[T, F]` instead.***

Where `UserAlgebra` is defined as:

```scala
trait UserAlgebra[F[_]] {
  def find(username: Username): F[User]
}
```

And implemented in two parts: a `program` that has all the logic:

```scala
class UserProgram[F[_]](repo: UserRepository[F])
                       (implicit F: MonadError[F, Throwable]) extends UserAlgebra[F] {

  override def find(username: Username): F[User] = {
    val notFound = F.raiseError[User](UserNotFound(username))
    for {
      mu <- repo.find(username) // F[Option[User]]
      rs <- mu.fold(notFound)(F.pure)
    } yield rs
  }

}
```

And an `interpreter` that just adds the tracing log part to it, by following a `tagless final` design:

```scala
class UserTracerInterpreter[F[_]](repo: UserRepository[KFX[F, ?]])
                                 (implicit F: MonadError[F, Throwable],
                                           L: TracerLog[KFX[F, ?]])
    extends UserProgram[KFX[F, ?]](repo) {

  override def find(username: Username): KFX[F, User] =
    for {
      _ <- L.info[UserAlgebra[F]](s"Find user by username: ${username.value}")
      u <- super.find(username)
    } yield u

}
```

#### Use the given `Tracer` middleware on your http routes:

```scala
import com.github.gvolpe.tracer.instances.tracerlog._

val userRoutes: HttpService[F] = new UserRoutes[F](service).routes
val routes: HttpService[F] = Tracer(userRoutes, headerName = "MyAppId") // Customizable Header name, default "Trace-Id"
```

Notice that an implicit instance of `TracerLog[F]` is needed for `Tracer.apply`. You can either provide your own or just use the default one that uses a `org.slf4j.Logger` instance to log the trace of your application.

#### Go and have a rest, your application won't lose track of all its activity :)

Although defining an interpreter in terms of `Kleisli` adds a bit of boilerplate, at the end it pays off. And it's also super easy to test! You can just test your `program` by just using `IO` for example.

Take a look at the [complete example](https://github.com/gvolpe/http4s-tracer/tree/master/examples/src) for more details.

### Credits

This is an idea I first heard from [Eric Torreborre](https://twitter.com/etorreborre), also described in [his Haskell setup](http://etorreborre.blogspot.jp/2018/03/haskell-modules-for-masses.html) by defining a newtype `RIO` (a.k.a. `Reader IO Monad`). However, this implementation is adapted to work nicely with `Http4s` while abstracting over the effect type using `Cats Effect` and where the main type is defined as `type KFX[F[_], A] = Kleisli[F, TraceId, A]`, a bit simpler than how `RIO` is actually defined because it's also doing less (just carrying a `TraceId` around).
