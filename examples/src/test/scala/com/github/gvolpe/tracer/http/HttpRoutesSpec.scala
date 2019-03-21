/*
 * Copyright 2018-2019 Gabriel Volpe
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

package com.github.gvolpe.tracer.http

import cats.effect.IO
import com.github.gvolpe.tracer.IOAssertion
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.{HttpRoutes, Request, Status}
import org.scalatest.{Assertion, FunSuite}
import org.scalatest.prop.TableFor3
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}

class HttpRoutesSpec extends FunSuite with PropertyChecks with Http4sClientDsl[IO] {

  def propertySpec(requests: TableFor3[String, IO[Request[IO]], Status], routes: HttpRoutes[IO]): Unit =
    forAll(requests) { (description, request, expectedStatus) =>
      test(description) {
        IOAssertion {
          for {
            req <- request
            rs  <- routes(req).value
            ast <- rs.fold(IO[Assertion](fail("Empty response"))) { response =>
                    IO(assert(response.status == expectedStatus))
                  }
          } yield ast
        }
      }
    }

}
