/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.plasticpackagingtax.registration.models.enrolment

import play.api.libs.json.{Format, Json, OFormat, Reads, Writes}
import uk.gov.hmrc.plasticpackagingtax.registration.models.enrolment.EnrolmentFailureCode.EnrolmentFailureCode

trait UserEnrolmentResponse

case class UserEnrolmentSuccessResponse(pptReference: String) extends UserEnrolmentResponse

case class UserEnrolmentFailedResponse(pptReference: String, failureCode: EnrolmentFailureCode)
    extends UserEnrolmentResponse

object UserEnrolmentSuccessResponse {

  implicit val format: OFormat[UserEnrolmentSuccessResponse] =
    Json.format[UserEnrolmentSuccessResponse]

}

object UserEnrolmentFailedResponse {

  implicit val format: OFormat[UserEnrolmentFailedResponse] =
    Json.format[UserEnrolmentFailedResponse]

}

object EnrolmentFailureCode extends Enumeration {
  type EnrolmentFailureCode = Value
  val Failed: Value             = Value
  val VerificationFailed: Value = Value

  implicit val format: Format[EnrolmentFailureCode] =
    Format(Reads.enumNameReads(EnrolmentFailureCode), Writes.enumNameWrites)

}
