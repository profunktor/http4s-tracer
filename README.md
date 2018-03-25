http4s-tracer
=============

It provides an `HttpMiddleware` that adds a `Trace-Id` header with a unique `UUID` value and gives you an implicit `TracerLog` for any `F[_]: Sync]` that also logs the http request and http response with it.

Quite useful to trace the flow of your application starting out at each request. Given a `UsersHttpRoutes`, `UserService` and `UserRepository` you'll get an activity log like the following:

```
TraceId(72b079c8-fc92-4c4f-aa5a-c0cd91ea221c) >> Performing Http Request GET /users
TraceId(72b079c8-fc92-4c4f-aa5a-c0cd91ea221c) >> UserService fetching users
TraceId(72b079c8-fc92-4c4f-aa5a-c0cd91ea221c) >> UserRepository fetching users from DB
TraceId(72b079c8-fc92-4c4f-aa5a-c0cd91ea221c) >> Performing Http Response GET /users
```

In a normal application, you will have thousands of requests and tracing the call chain in a failure scenario will be invaluable.

***DISCLAIMER***: This is an idea I first heard from [Eric Torreborre](https://twitter.com/etorreborre), also described in [his Haskell setup](http://etorreborre.blogspot.jp/2018/03/haskell-modules-for-masses.html) by defining a newtype `RIO` (a.k.a. `Reader IO Monad`). However, this implementation is adapted to work nicely with `Http4s` while abstracting over the effect type using `Cats Effect`.
