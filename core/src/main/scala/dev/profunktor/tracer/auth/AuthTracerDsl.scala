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

package dev.profunktor.tracer.auth

import dev.profunktor.tracer.Tracer.TraceId
import dev.profunktor.tracer.auth.AuthTracedHttpRoute.AuthTracedRequest
import org.http4s.AuthedRequest

trait AuthTracerDsl {
  object using {
    def unapply[T, F[_]](tr: AuthTracedRequest[F, T]): Option[(AuthedRequest[F, T], TraceId)] =
      Some(tr.request -> tr.traceId)
  }
}
