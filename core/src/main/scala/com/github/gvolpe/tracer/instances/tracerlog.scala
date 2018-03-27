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

package com.github.gvolpe.tracer.instances

import cats.data.Kleisli
import cats.effect.Sync
import com.github.gvolpe.tracer.Tracer.KFX
import com.github.gvolpe.tracer.TracerLog
import org.slf4j.{Logger, LoggerFactory}

import scala.reflect.ClassTag

object tracerlog {

  implicit def defaultLog[F[_]](implicit F: Sync[F]): TracerLog[KFX[F, ?]] =
    new TracerLog[KFX[F, ?]] {
      def logger[A](implicit ct: ClassTag[A]): Logger =
        LoggerFactory.getLogger(ct.runtimeClass)

      override def info[A: ClassTag](value: String): KFX[F, Unit] = Kleisli { id =>
        F.delay(logger[A].info(s"$id >> $value"))
      }

      override def error[A: ClassTag](error: Exception): KFX[F, Unit] = Kleisli { id =>
        F.delay(logger[A].error(s"$id >> ${error.getMessage}"))
      }

      override def warn[A: ClassTag](value: String): KFX[F, Unit] = Kleisli { id =>
        F.delay(logger[A].warn(s"$id >> $value"))
      }
    }

}
