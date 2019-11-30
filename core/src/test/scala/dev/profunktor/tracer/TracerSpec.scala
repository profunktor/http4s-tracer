/*
 * Copyright 2018-2019 ProfunKtor
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
import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}
import org.scalatest.funsuite.AnyFunSuite

class TracerSpec extends AnyFunSuite with TracerFixture {

  forAll(examples) { (name, request, tracer, assertions) =>
    test(name) {
      IOAssertion {
        for {
          req  <- request
          resp <- tracer(req)
          _    <- assertions(resp)
        } yield ()
      }
    }
  }

}

// format: off
trait TracerFixture extends PropertyChecks {

  val customHeaderName  = "Test-Id"
  val customHeaderValue = "my-custom-value"

  val tracer: Tracer[IO] = Tracer.create[IO]()
  val customTracer: Tracer[IO] = Tracer.create[IO](customHeaderName)

  val tracerApp: HttpApp[IO]        = tracer.middleware(TestHttpRoute.routes(tracer).orNotFound)
  val customTracerApp: HttpApp[IO]  = customTracer.middleware(TestHttpRoute.routes(customTracer).orNotFound)

  def defaultAssertion(traceHeaderName: String): Response[IO] => IO[Unit] = resp =>
    IO {
      assert(resp.status == Status.Ok)
      assert(resp.headers.toList.map(_.name.value).contains(traceHeaderName))
    }

  def customAssertion(traceHeaderName: String): Response[IO] => IO[Unit] = resp =>
    IO {
      assert(resp.status == Status.Ok)
      assert(resp.headers.toList.map(_.name.value).contains(traceHeaderName))
      assert(resp.headers.toList.map(_.value).contains(customHeaderValue))
    }

  val examples = Table(
    ("name", "request", "tracer", "assertions"),
    ("Default TraceId header is created", GET(Uri.uri("/")), tracerApp, defaultAssertion(Tracer.DefaultTraceIdHeader)),
    ("TraceId header is passed in the request (no TraceId created)", GET(Uri.uri("/"), Header(Tracer.DefaultTraceIdHeader, customHeaderValue)), tracerApp, customAssertion(Tracer.DefaultTraceIdHeader)),
    ("Custom TraceId header (Test-Id) is created", GET(Uri.uri("/")), customTracerApp, defaultAssertion(customHeaderName)),
    ("TraceId header (Test-Id) is passed in the request", GET(Uri.uri("/"), Header(customHeaderName, customHeaderValue)), customTracerApp, customAssertion(customHeaderName))
  )

}

object TestHttpRoute extends Http4sTracerDsl[IO] {
  def routes(implicit t: Tracer[IO]): HttpRoutes[IO] = TracedHttpRoute[IO] {
    case GET -> Root using traceId =>
      Ok(traceId.value)
  }
}
