# Motivations

Nowadays most backend-software is written as a microservice that needs to interact with other internal and possibly external services. Although the benefits are quite obvious (independently deployable and managed, etc) there are some associated challenges.

With this setup it becomes very hard to diagnose what went wrong in the case of failure. We might have isolated logs but how can we tell a call to a database and an external service are associated to a single http request? This is a very common problem with a only a few solutions and not so many standards.

*Distributed tracing* is still an area of research but some standards have already emerged in the past years, most of
them based on the [Google Dapper](https://ai.google/research/pubs/pub36356) specification:

- [Open Tracing](https://opentracing.io/)
- [ZipKin](https://zipkin.io/)
- [Jaeger](https://www.jaegertracing.io/)

The solution proposed by these specifications are very broad and their intent is to become a standard no matter what programming language you're using. But it comes with a price. Setting this up is not a trivial task and the most important feature is to trace distributed calls which, in my opinion, can be simplified quite a lot with a minimalistic solution.

### The RIO Monad

The most common approach to carry "context" (this can be a "Trace-Id") in functional programming is by using the `ReaderT` monad transformer or more commonly called `Kleisli`.

There are a few blog posts out there recommending the use of the `RIO` monad, which is basically `ReaderT` + `IO` defined as a `newtype`, in production applications. The most remarkable ones and where I've got the inspiration to write this library are:

- [[Jul 2017] - The RIO Monad](https://www.fpcomplete.com/blog/2017/07/the-rio-monad) by [Michael Snoyman](https://github.com/snoyberg)
- [[Mar 2018] - Haskell modules for masses](http://etorreborre.blogspot.jp/2018/03/haskell-modules-for-masses.html) by [Eric Torreborre](https://twitter.com/etorreborre)

### Http4s Tracer

The implementation presented by this library follows the same idea but in a minimalistic way: just carrying a `TraceId` around instead of a whole application context. And it is mainly adapted to work nicely with `Http4s` while abstracting over the effect type using `Cats Effect` which are two amazing libraries of the [Typelevel](https://typelevel.org/) ecosystem.

The main type defined as `type Trace[F[_], A] = Kleisli[F, TraceId, A]`, a bit simpler than how `RIO` is defined because it's also doing less.

