package com.github.gvolpe.tracer

//import cats.Applicative
//import cats.data.{Kleisli, OptionT}
//import com.github.gvolpe.tracer.Tracer.TraceId
//import org.http4s.{HttpService, Request, Response}

object TraceableHttpRoutes {
//  def apply[F[_]](pf: TraceId => PartialFunction[Request[F], F[Response[F]]])(
//      implicit F: Applicative[F]): HttpService[F] =
//    Kleisli { req: Request[F] =>
//      val traceId: Tracer.TraceId = Tracer.getTraceId[F](req)
//      val a: PartialFunction[Request[F], F[Response[F]]] = pf(traceId)
//      val b: PartialFunction[Request[F], OptionT[F, Response[F]]] = a.andThen(OptionT.liftF(_))
//      b.applyOrElse(req, Function.const(OptionT.none))
//      //      pf(traceId).andThen(OptionT.liftF(_)).applyOrElse(req, Function.const(OptionT.none))
//    }
}
