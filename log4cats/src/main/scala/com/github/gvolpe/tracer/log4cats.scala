/*
 * Copyright 2018 com.github.gvolpe
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

package com.github.gvolpe.tracer

import com.github.gvolpe.tracer.Trace._
import io.chrisdavenport.log4cats.Logger

import scala.reflect.ClassTag

object log4cats {

  implicit def log4CatsInstance[F[_]](implicit L: Logger[F]): TracerLog[Trace[F, ?]] =
    new TracerLog[Trace[F, ?]] {
      override def info[A: ClassTag](value: => String): Trace[F, Unit] = Trace { id =>
        L.info(s"$id - $value")
      }
      override def error[A: ClassTag](value: => String): Trace[F, Unit] = Trace { id =>
        L.error(s"$id - $value")
      }
      override def warn[A: ClassTag](value: => String): Trace[F, Unit] = Trace { id =>
        L.warn(s"$id - $value")
      }
    }

}
