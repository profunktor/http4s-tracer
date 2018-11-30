---
layout: docs
title:  "Overview"
number: 1
position: 1
---

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
10:50:16.587 [ec-1] INFO  c.g.g.t.Tracer - [Trace-Id] -[490bd050-f442-11e8-a46d-578226236d02] - Request(method=POST, uri=/users, ...)
10:50:16.768 [ec-1] INFO  c.g.g.t.a.UserAlgebra - [Trace-Id] - [490bd050-f442-11e8-a46d-578226236d02] - About to persist user: gvolpe
10:50:16.769 [ec-1] INFO  c.g.g.t.r.a$UserRepository - [Trace-Id] - [490bd050-f442-11e8-a46d-578226236d02] - Find user by username: gvolpe
10:50:16.770 [ec-1] INFO  c.g.g.t.r.a$UserRepository - [Trace-Id] - [490bd050-f442-11e8-a46d-578226236d02] - Persisting user: gvolpe
10:50:16.773 [ec-1] INFO  c.g.g.t.Tracer - [Trace-Id] - [490bd050-f442-11e8-a46d-578226236d02] - Response(status=201, ...)
```

