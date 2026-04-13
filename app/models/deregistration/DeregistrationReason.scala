/*
 * Copyright 2026 HM Revenue & Customs
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

package models.deregistration

import play.api.libs.json.{Format, JsError, JsString, JsSuccess, Reads, Writes}

import scala.util.Try

enum DeregistrationReason(val value: String):
  case RegisteredIncorrectly      extends DeregistrationReason("Registered Incorrectly")
  case CeasedTrading              extends DeregistrationReason("Ceased Trading")
  case BelowDeminimis             extends DeregistrationReason("Below De-minimus")
  case TakenIntoGroupRegistration extends DeregistrationReason("Taken into Group Registration")
  override def toString: String = value // for backwards compatibility only

object DeregistrationReason {
  def withNameOpt(name: String): Option[DeregistrationReason] = DeregistrationReason.values.find(_.value == name)

  implicit def value(reason: DeregistrationReason): String = reason.value

  given Format[DeregistrationReason] =
    Format(
      Reads {
        case JsString(value) =>
          DeregistrationReason.values.find(_.value == value)
            .map(JsSuccess(_))
            .getOrElse(JsError(s"Unknown DeregistrationReason: $value"))
        case _ =>
          JsError("String value expected")
      },
      Writes(deRegistrationReason => JsString(deRegistrationReason.value))
    )
}
