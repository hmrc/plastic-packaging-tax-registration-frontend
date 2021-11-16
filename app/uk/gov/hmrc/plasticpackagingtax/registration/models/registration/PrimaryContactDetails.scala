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

package uk.gov.hmrc.plasticpackagingtax.registration.models.registration

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.views.model.TaskStatus

case class PrimaryContactDetails(
  name: Option[String] = None,
  jobTitle: Option[String] = None,
  email: Option[String] = None,
  phoneNumber: Option[String] = None,
  useRegisteredAddress: Option[Boolean] = None,
  journeyId: Option[String] = None,
  address: Option[Address] = None
) {

  def status(emailVerified: String => Boolean): TaskStatus =
    if (isCompleted(emailVerified)) TaskStatus.Completed
    else if (isInProgress) TaskStatus.InProgress
    else TaskStatus.NotStarted

  private def isCompleted(emailVerified: String => Boolean): Boolean =
    name.isDefined && jobTitle.isDefined && email.exists(
      emailVerified
    ) && phoneNumber.isDefined && address.isDefined

  private def isInProgress: Boolean =
    name.isDefined || jobTitle.isDefined || email.isDefined || phoneNumber.isDefined || address.isDefined

}

object PrimaryContactDetails {
  implicit val format: OFormat[PrimaryContactDetails] = Json.format[PrimaryContactDetails]
}
