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

package dev.profunktor.tracer.instances

import cats.effect.Sync
import cats.syntax.flatMap._
import dev.profunktor.tracer.Trace
import dev.profunktor.tracer.Trace._
import dev.profunktor.tracer.TracerLog
import org.slf4j.{Logger, LoggerFactory}

import scala.reflect.ClassTag

object tracerlog {

  implicit def defaultLog[F[_]](implicit F: Sync[F]): TracerLog[Trace[F, ?]] =
    new TracerLog[Trace[F, ?]] {
      def logger[A](implicit ct: ClassTag[A]): F[Logger] =
        F.delay(LoggerFactory.getLogger(ct.runtimeClass))

      override def info[A: ClassTag](value: => String): Trace[F, Unit] = Trace { id =>
        logger[A].flatMap { log =>
          if (log.isInfoEnabled) F.delay(log.info(s"$id - $value"))
          else F.unit
        }
      }

      override def error[A: ClassTag](value: => String): Trace[F, Unit] = Trace { id =>
        logger[A].flatMap { log =>
          if (log.isErrorEnabled) F.delay(log.error(s"$id - $value"))
          else F.unit
        }
      }

      override def warn[A: ClassTag](value: => String): Trace[F, Unit] = Trace { id =>
        logger[A].flatMap { log =>
          if (log.isWarnEnabled) F.delay(log.warn(s"$id - $value"))
          else F.unit
        }
      }
    }

}
