# Overview

`http4s-tracer` is an end-to-end tracing system for `http4s`.

So what does this mean? That it gives you the ability to trace the call-chain from request to response, going through
the different components in between such as external http calls, database access, etc, even in a distributed environment.

### Minimalistic design

Its design is very simple and minimalistic. These are main components:

- `Trace[F]` monad which is just an alias for `Kleisli[F, TraceId, ?]`.
- `Tracer[F]` typeclass that let's you access the http middleware.
- `Http4sTracerDsl[F]` as a replacement for `Http4sDsl[F]`.
- `TracedHttpRoute[F]` as a replacement for `HttpRoutes.of[F]`.
- `TracerLog[F]` typeclass for structured logging with tracing information.

With all this machinery in place, the system will trace the call-chain of every single request by either adding a `Trace-Id` header with a unique identifier or by passing around the one received. Note that the header name is customizable.

### Separation of concerns

The tracing-aware components should only be your `HttpRoutes`. It is not required by any means that the tracing logic should propagate throughout your system. This has some immediate benefits:

- Your business logic remains pure.
- Easy to test components in isolation.

This separation of concerns is at the core of `http4s-tracer`'s design.

### Structured logging

A normal application commonly serves hundreds of requests concurrently and being able to trace the call-graph of a single one could be invaluable in failure scenarios. Here's an example of how the logs look like when using the default `TracerLog` instance:

```bash
18:02:25.366 [blaze-selector-0-2] INFO  o.h.b.c.nio1.NIO1SocketServerGroup - Accepted connection from /0:0:0:0:0:0:0:1:58284
18:02:25.375 [ec-1] INFO  dev.profunktor.tracer.Tracer - [Trace-Id] - [6cb069c0-2792-11e9-9038-b9bcfc32f88f] - Request(method=POST, uri=/users, headers=Headers(HOST: localhost:8080, content-type: application/json, content-length: 8))
18:02:25.527 [ec-1] INFO  d.p.tracer.algebra.UserAlgebra - [Trace-Id] - [6cb069c0-2792-11e9-9038-b9bcfc32f88f] - About to persist user: gvolpe
18:02:25.527 [ec-1] INFO  d.p.t.r.algebra$UserRepository - [Trace-Id] - [6cb069c0-2792-11e9-9038-b9bcfc32f88f] - Find user by username: gvolpe
18:02:25.540 [ec-1] INFO  d.p.t.http.client.UserRegistry - [Trace-Id] - [6cb069c0-2792-11e9-9038-b9bcfc32f88f] - Registering user: gvolpe
18:02:25.540 [ec-1] INFO  d.p.t.r.algebra$UserRepository - [Trace-Id] - [6cb069c0-2792-11e9-9038-b9bcfc32f88f] - Persisting user: gvolpe
18:02:26.601 [ec-1] INFO  dev.profunktor.tracer.Tracer - [Trace-Id] - [6cb069c0-2792-11e9-9038-b9bcfc32f88f] - Response(status=201, headers=Headers(Content-Length: 0, Flow-Id: 6cb069c0-2792-11e9-9038-b9bcfc32f88f))
```

