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

package models.emailverification

import play.api.libs.json.*

import scala.util.Try

enum EmailVerificationJourneyStatus(val value: String):
  case COMPLETE           extends EmailVerificationJourneyStatus("complete")
  case INCORRECT_PASSCODE extends EmailVerificationJourneyStatus("incorrectPasscode")
  case TOO_MANY_ATTEMPTS  extends EmailVerificationJourneyStatus("tooManyAttempts")
  case JOURNEY_NOT_FOUND  extends EmailVerificationJourneyStatus("journeyNotFound")

object EmailVerificationJourneyStatus {
  given Format[EmailVerificationJourneyStatus] =
    Format(
      Reads {
        case JsString(value) =>
          Try(EmailVerificationJourneyStatus.valueOf(value))
            .map(JsSuccess(_))
            .getOrElse(JsError(s"Unknown EmailVerificationJourneyStatus: $value"))
        case _ =>
          JsError("String value expected")
      },
      Writes(emailVerificationJourneyStatus => JsString(emailVerificationJourneyStatus.toString))
    )
}
