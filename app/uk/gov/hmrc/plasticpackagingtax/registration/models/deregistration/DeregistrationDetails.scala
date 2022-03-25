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

package uk.gov.hmrc.plasticpackagingtax.registration.models.deregistration

import play.api.libs.json._
import uk.gov.hmrc.plasticpackagingtax.registration.models.deregistration.DeregistrationReason.DeregistrationReason

object DeregistrationReason extends Enumeration {
  type DeregistrationReason = Value

  val RegisteredIncorrectly               = Value("Registered Incorrectly")
  val CeasedTrading                       = Value("Ceased Trading")
  val NotMetAndDoNotExpectToMeetThreshold = Value("Below De-minimus")
  val WantToRegisterAsGroup               = Value("Taken into Group Registration")

  implicit val format: Format[DeregistrationReason] =
    Format(Reads.enumNameReads(DeregistrationReason), Writes.enumNameWrites)

}

case class DeregistrationDetails(deregister: Option[Boolean], reason: Option[DeregistrationReason])

object DeregistrationDetails {
  implicit val format: OFormat[DeregistrationDetails] = Json.format[DeregistrationDetails]
}
