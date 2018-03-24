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

import cats.data.Kleisli
import cats.effect.Sync
import com.github.gvolpe.tracer.Tracer.{TraceId, KFX}
import org.slf4j.{Logger, LoggerFactory}

import scala.reflect.ClassTag

trait TracerLog[F[_]] {
  def info[A: ClassTag](value: String): F[Unit]
  def error[A: ClassTag](error: Exception): F[Unit]
}

object TracerLog {
  implicit def defaultLog[F[_]](implicit F: Sync[F]): TracerLog[KFX[F, ?]] =
    new TracerLog[KFX[F, ?]] {
      def logger[A](implicit ct: ClassTag[A]): Logger =
        LoggerFactory.getLogger(ct.runtimeClass)

      override def info[A](value: String)(implicit ct: ClassTag[A]): Kleisli[F, TraceId, Unit] = Kleisli { id =>
        F.delay(logger[A].info(s"$id >> $value"))
      }

      override def error[A](error: Exception)(implicit ct: ClassTag[A]): Kleisli[F, TraceId, Unit] = Kleisli { id =>
        F.delay(logger[A].error(s"$id >> ${error.getMessage}"))
      }
    }

}
