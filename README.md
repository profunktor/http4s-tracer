http4s-tracer
=============

It provides an `HttpMiddleware` that adds a `Trace-Id` header with a unique `UUID` value and gives you an implicit `TracerLog` for any `F[_]: Sync]` that also logs the http request and http response with it.

Quite useful to trace the flow of your application starting out at each request. Given a `UsersHttpRoutes`, `UserAlgebra` and `UserRepository` you'll get an activity log like the following when trying to create a user:

```
4:13:48.985 [scala-execution-context-global-17] INFO com.github.gvolpe.tracer.Tracer$ - TraceId(02594e59-4b21-4d0a-aad5-5866a632fbb5) >> Request(method=POST, uri=/users, headers=Headers(HOST: localhost:8080, content-type: application/json, content-length: 30))
14:13:49.282 [scala-execution-context-global-17] INFO com.github.gvolpe.tracer.algebra$UserAlgebra - TraceId(02594e59-4b21-4d0a-aad5-5866a632fbb5) >> About to persist user: modersky
14:13:49.290 [scala-execution-context-global-17] INFO com.github.gvolpe.tracer.repository.algebra$UserRepository - TraceId(02594e59-4b21-4d0a-aad5-5866a632fbb5) >> Find user by username: modersky
14:13:49.298 [scala-execution-context-global-17] INFO com.github.gvolpe.tracer.repository.algebra$UserRepository - TraceId(02594e59-4b21-4d0a-aad5-5866a632fbb5) >> Persisting user: modersky
14:13:49.315 [scala-execution-context-global-17] INFO com.github.gvolpe.tracer.Tracer$ - TraceId(02594e59-4b21-4d0a-aad5-5866a632fbb5) >> Response(status=201, headers=Headers(Content-Length: 0))
```

In a normal application, you will have thousands of requests and tracing the call chain in a failure scenario will be invaluable.

### Credits

This is an idea I first heard from [Eric Torreborre](https://twitter.com/etorreborre), also described in [his Haskell setup](http://etorreborre.blogspot.jp/2018/03/haskell-modules-for-masses.html) by defining a newtype `RIO` (a.k.a. `Reader IO Monad`). However, this implementation is adapted to work nicely with `Http4s` while abstracting over the effect type using `Cats Effect` and where the main type is defined as `type KFX[F[_], A] = Kleisli[F, TraceId, A]`, a bit simpler than how `RIO` is actually defined because it's also doing less (just carrying a `TraceId` around).
