/*
 * Copyright 2018-2020 ProfunKtor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.profunktor.tracer

import cats.effect.IO
import dev.profunktor.tracer.instances.tracerlog._
import munit.FunSuite
import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.dsl.io._
import org.http4s.implicits._

class TracerSpec extends FunSuite {

  // format: off
  override def munitValueTransforms =
    super.munitValueTransforms :+ new ValueTransform("IO", {
      case ioa: IO[_] => IO.suspend(ioa).unsafeToFuture
    })
  // format: on

  val customHeaderName  = "Test-Id"
  val customHeaderValue = "my-custom-value"

  val tracer: Tracer[IO]       = Tracer.create[IO]()
  val customTracer: Tracer[IO] = Tracer.create[IO](customHeaderName)

  val tracerApp: HttpApp[IO]       = tracer.middleware(TestHttpRoute.routes(tracer).orNotFound)
  val customTracerApp: HttpApp[IO] = customTracer.middleware(TestHttpRoute.routes(customTracer).orNotFound)

  def defaultAssertion(traceHeaderName: String): Response[IO] => IO[Unit] =
    resp =>
      IO {
        assert(resp.status == Status.Ok)
        assert(resp.headers.toList.map(_.name.value).contains(traceHeaderName))
    }

  def customAssertion(traceHeaderName: String): Response[IO] => IO[Unit] =
    resp =>
      IO {
        assert(resp.status == Status.Ok)
        assert(resp.headers.toList.map(_.name.value).contains(traceHeaderName))
        assert(resp.headers.toList.map(_.value).contains(customHeaderValue))
    }

  test("Default TraceId header is created") {
    for {
      req  <- GET(Uri.uri("/"))
      resp <- tracerApp(req)
      _    <- defaultAssertion(Tracer.DefaultTraceIdHeader)(resp)
    } yield ()
  }

  test("TraceId header is passed in the request (no TraceId created)") {
    for {
      req  <- GET(Uri.uri("/"), Header(Tracer.DefaultTraceIdHeader, customHeaderValue))
      resp <- tracerApp(req)
      _    <- customAssertion(Tracer.DefaultTraceIdHeader)(resp)
    } yield ()
  }

  test("Custom TraceId header (Test-Id) is created") {
    for {
      req  <- GET(Uri.uri("/"))
      resp <- customTracerApp(req)
      _    <- defaultAssertion(customHeaderName)(resp)
    } yield ()
  }

  test("TraceId header (Test-Id) is passed in the request") {
    for {
      req  <- GET(Uri.uri("/"), Header(customHeaderName, customHeaderValue))
      resp <- customTracerApp(req)
      _    <- customAssertion(customHeaderName)(resp)
    } yield ()
  }

}

object TestHttpRoute extends Http4sTracerDsl[IO] {
  def routes(implicit t: Tracer[IO]): HttpRoutes[IO] =
    TracedHttpRoute[IO] {
      case GET -> Root using traceId =>
        Ok(traceId.value)
    }
}
