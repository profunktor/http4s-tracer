package com.github.gvolpe.tracer

import cats.effect.Sync
import com.github.gvolpe.tracer.Tracer.KFX
import com.github.gvolpe.tracer.algebra.UserAlgebra
import com.github.gvolpe.tracer.http.UserRoutes
import com.github.gvolpe.tracer.interpreter.UserTracerInterpreter
import com.github.gvolpe.tracer.repository.UserTracerRepository
import com.github.gvolpe.tracer.repository.algebra.UserRepository
import org.http4s.HttpService

class Module[F[_]: Sync] {

  private val repo: UserRepository[KFX[F, ?]] =
    new UserTracerRepository[F]

  private val service: UserAlgebra[KFX[F, ?]] =
    new UserTracerInterpreter[F](repo)

  val routes: HttpService[F] =
    new UserRoutes[F](service).routes

}
