http4s-tracer
=============

[![Build Status](https://travis-ci.org/gvolpe/http4s-tracer.svg?branch=master)](https://travis-ci.org/gvolpe/http4s-tracer)
[![Gitter Chat](https://badges.gitter.im/http4s-tracer/http4s-tracer.svg)](https://gitter.im/http4s-tracer/http4s-tracer)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.gvolpe/http4s-tracer_2.12.svg)](http://search.maven.org/#search%7Cga%7C1%7Chttp4s-tracer)

It provides an `HttpMiddleware` that adds a `Trace-Id` header (name can be customized) with a unique `UUID` value and gives you an implicit `TracerLog` for any `F[_]: Sync]` that also logs the http request and http response with it.

Quite useful to trace the flow of your application starting out at each request. For example, given a `UsersHttpRoutes`, `UserAlgebra` and `UserRepository` you'll get an activity log like the following when trying to create a user:

```bash
4:13:48.985 [scala-execution-context-global-17] INFO com.github.gvolpe.tracer.Tracer$ - TraceId(02594e59-4b21-4d0a-aad5-5866a632fbb5) >> Request(method=POST, uri=/users, headers=Headers(HOST: localhost:8080, content-type: application/json, content-length: 30))
14:13:49.282 [scala-execution-context-global-17] INFO com.github.gvolpe.tracer.algebra$UserAlgebra - TraceId(02594e59-4b21-4d0a-aad5-5866a632fbb5) >> About to persist user: modersky
14:13:49.290 [scala-execution-context-global-17] INFO com.github.gvolpe.tracer.repository.algebra$UserRepository - TraceId(02594e59-4b21-4d0a-aad5-5866a632fbb5) >> Find user by username: modersky
14:13:49.298 [scala-execution-context-global-17] INFO com.github.gvolpe.tracer.repository.algebra$UserRepository - TraceId(02594e59-4b21-4d0a-aad5-5866a632fbb5) >> Persisting user: modersky
14:13:49.315 [scala-execution-context-global-17] INFO com.github.gvolpe.tracer.Tracer$ - TraceId(02594e59-4b21-4d0a-aad5-5866a632fbb5) >> Response(status=201, headers=Headers(Content-Length: 0, Trace-Id: 02594e59-4b21-4d0a-aad5-5866a632fbb5))
```

In a normal application, you will have thousands of requests and tracing the call chain in a failure scenario will be invaluable.

### Dependencies

Add this to your `build.sbt`:

```
libraryDependencies += "com.github.gvolpe" %% "http4s-tracer" % Version
```

`http4s-tracer` has the following dependencies:

| Dependency   | Version    |
| ------------ |:----------:|
| cats         | 1.3.1      |
| cats-effect  | 1.0.0      |
| fs2          | 1.0.0-RC2  |
| gfc-timeuuid | 0.0.8      |
| http4s       | 0.19.0-M3  |

### Credits

This is an idea I first heard from [Eric Torreborre](https://twitter.com/etorreborre), also described in [his Haskell setup](http://etorreborre.blogspot.jp/2018/03/haskell-modules-for-masses.html) by defining a newtype `RIO` (a.k.a. `Reader IO Monad`) and inspired bt the [RIO Monad](https://www.fpcomplete.com/blog/2017/07/the-rio-monad) described by [Michael Snoyman](https://github.com/snoyberg) the year before. However, this implementation is adapted to work nicely with `Http4s` while abstracting over the effect type using `Cats Effect` and where the main type is defined as `type KFX[F[_], A] = Kleisli[F, TraceId, A]`, a bit simpler than how `RIO` is actually defined because it's also doing less (just carrying a `TraceId` around).
