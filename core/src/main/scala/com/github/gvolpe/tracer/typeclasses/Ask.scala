package com.github.gvolpe.tracer.typeclasses

import cats.Applicative
import cats.syntax.applicative._
import com.github.gvolpe.tracer.Tracer.TraceIdHeaderName

/**
  * Light version of cats.mtl.ApplicativeAsk[F, A].
  *
  * Mainly used to customize the TraceId header name.
  * */
trait Ask[F[_], A] {
  def ask: F[A]
}

object Ask {
  def headerNameDefaultInstance[F[_]: Applicative]: Ask[F, TraceIdHeaderName] =
    new Ask[F, TraceIdHeaderName] {
      override def ask: F[TraceIdHeaderName] = TraceIdHeaderName("Trace-Id").pure[F]
    }
}