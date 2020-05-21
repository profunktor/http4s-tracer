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

import dev.profunktor.tracer.instances.tracer._
import dev.profunktor.tracer.instances.tracerlog._
import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._

object ZIOServer extends CatsApp {

  def run(args: List[String]): UIO[Int] =
    new Main[Task].server.run.map(_.fold(_ => 1, _ => 0))

}
