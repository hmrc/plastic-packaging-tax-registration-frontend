/*
 * Copyright 2025 HM Revenue & Customs
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

package forms.mappings

import forms.YesNoValues
import play.api.data.FormError
import play.api.data.format.Formatter

import scala.util.control.Exception.nonFatalCatch

trait Formatters {

  private[mappings] def yesNoFormatter(msgKey: String): Formatter[Boolean] =
    new Formatter[Boolean] {

      def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Boolean] =
        data.get(key) match {
          case Some(YesNoValues.YES) => Right(true)
          case Some(YesNoValues.NO)  => Right(false)
          case _                     => Left(Seq(FormError(key, msgKey)))
        }

      def unbind(key: String, value: Boolean) = Map(key -> { if (value) YesNoValues.YES else YesNoValues.NO })
    }

  private[mappings] def stringFormatter(errorKey: String): Formatter[String] =
    new Formatter[String] {

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] =
        data.get(key) match {
          case None | Some("") => Left(Seq(FormError(key, errorKey)))
          case Some(s)         => Right(s)
        }

      override def unbind(key: String, value: String): Map[String, String] =
        Map(key -> value)

    }

  private[mappings] def intFormatter(
    requiredKey: String,
    wholeNumberKey: String,
    nonNumericKey: String,
    args: Seq[String] = Seq.empty
  ): Formatter[Int] =
    new Formatter[Int] {

      val decimalRegexp = """^-?(\d*\.\d*)$"""

      private val baseFormatter = stringFormatter(requiredKey)

      override def bind(key: String, data: Map[String, String]) =
        baseFormatter
          .bind(key, data)
          .map(_.replace(",", ""))
          .flatMap {
            case s if s.matches(decimalRegexp) =>
              Left(Seq(FormError(key, wholeNumberKey, args)))
            case s =>
              nonFatalCatch
                .either(s.toInt)
                .left.map(_ => Seq(FormError(key, nonNumericKey, args)))
          }

      override def unbind(key: String, value: Int) =
        baseFormatter.unbind(key, value.toString)

    }

}
