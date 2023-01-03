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

package models.registration

import play.api.libs.json.{Json, OFormat}
import forms.enrolment.{
  IsUkAddress,
  Postcode,
  PptReference,
  RegistrationDate
}
import models.enrolment.UserEnrolmentRequest

case class UserEnrolmentDetails(
  pptReference: Option[PptReference] = None,
  isUkAddress: Option[IsUkAddress] = None,
  postcode: Option[Postcode] = None,
  registrationDate: Option[RegistrationDate] = None
) {

  def isComplete: Boolean =
    pptReference.isDefined && isUkAddress.isDefined && registrationDate.isDefined && (isUkAddress.exists(
      !_.requiresPostCode
    ) || postcode.isDefined)

  def toUserEnrolmentRequest: UserEnrolmentRequest =
    UserEnrolmentRequest(pptReference = pptReference.map(_.value).getOrElse(""),
                         registrationDate = registrationDate.get.value.asLocalDate,
                         postcode = Some(postcode.map(_.value).getOrElse(""))
    )

}

object UserEnrolmentDetails {
  implicit val format: OFormat[UserEnrolmentDetails] = Json.format[UserEnrolmentDetails]
}
