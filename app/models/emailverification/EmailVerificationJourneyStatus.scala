/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.libs.json._

object EmailVerificationJourneyStatus extends Enumeration {
  type JourneyStatus = Value
  val COMPLETE: Value           = Value("complete")
  val INCORRECT_PASSCODE: Value = Value("incorrectPasscode")
  val TOO_MANY_ATTEMPTS: Value  = Value("tooManyAttempts")
  val JOURNEY_NOT_FOUND: Value  = Value("journeyNotFound")

  implicit val format: Format[JourneyStatus] =
    Format(Reads.enumNameReads(EmailVerificationJourneyStatus), Writes.enumNameWrites)

}
