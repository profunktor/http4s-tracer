package com.github.gvolpe.tracer

import cats.effect.Sync
import com.github.gvolpe.tracer.Tracer.Traceable
import com.github.gvolpe.tracer.http.UserRoutes
import com.github.gvolpe.tracer.repository.{DefaultUserRepository, UserRepository}
import com.github.gvolpe.tracer.service.{DefaultUserService, UserService}
import org.http4s.HttpService

class Module[F[_]: Sync] {

  private val repo: UserRepository[Traceable[F, ?]] =
    new DefaultUserRepository[F]

  private val service: UserService[Traceable[F, ?]] =
    new DefaultUserService[F](repo)

  val routes: HttpService[F] =
    new UserRoutes[F](service).routes

}
