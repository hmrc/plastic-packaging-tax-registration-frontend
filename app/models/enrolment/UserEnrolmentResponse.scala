/*
 * Copyright 2023 HM Revenue & Customs
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

package models.enrolment

import play.api.libs.json.{Json, OFormat}

sealed trait UserEnrolmentResponse

case class UserEnrolmentSuccessResponse(pptReference: String) extends UserEnrolmentResponse

case class UserEnrolmentFailedResponse(pptReference: String, failureCode: String)
    extends UserEnrolmentResponse

object UserEnrolmentSuccessResponse {

  implicit val format: OFormat[UserEnrolmentSuccessResponse] =
    Json.format[UserEnrolmentSuccessResponse]

}

object UserEnrolmentFailedResponse {

  implicit val format: OFormat[UserEnrolmentFailedResponse] =
    Json.format[UserEnrolmentFailedResponse]

}

object EnrolmentFailureCode {
  val VerificationFailed  = "VerificationFailed"
  val VerificationMissing = "VerificationMissing"
  val GroupEnrolled       = "GroupEnrolled"
}
