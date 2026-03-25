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

package models.genericregistration

import play.api.libs.json.{Format, JsError, JsString, JsSuccess, Reads, Writes}
import models.genericregistration
import models.genericregistration.Regime.given_Format_Regime

import scala.util.Try

enum Regime:
  case PPT extends Regime

object Regime {
  given Format[Regime] =
    Format(
      Reads {
        case JsString(value) =>
          Try(Regime.valueOf(value))
            .map(JsSuccess(_))
            .getOrElse(JsError(s"Unknown Regime: $value"))
        case _ =>
          JsError("String value expected")
      },
      Writes(regime => JsString(regime.toString))
    )
}

trait GrsJourneyCreationRequest[R <: GrsJourneyCreationRequest[R]] {
  self: R =>
  val continueUrl: String
  val optServiceName: Option[String]
  val deskProServiceId: String
  val signOutUrl: String
  val regime: Regime
  val accessibilityUrl: String
  val businessVerificationCheck: Boolean

  def setBusinessVerificationCheckFalse: R
}
