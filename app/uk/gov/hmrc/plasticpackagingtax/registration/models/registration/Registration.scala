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
import uk.gov.hmrc.plasticpackagingtax.registration.views.model.TaskStatus

case class Registration(
  id: String,
  incorpJourneyId: Option[String] = None,
  liabilityDetails: LiabilityDetails = LiabilityDetails(),
  primaryContactDetails: PrimaryContactDetails = PrimaryContactDetails(),
  organisationDetails: OrganisationDetails = OrganisationDetails(),
  metaData: MetaData = MetaData(),
  userHeaders: Option[Map[String, String]] = None
) {

  def toRegistration: Registration =
    Registration(id = this.id,
                 incorpJourneyId = this.incorpJourneyId,
                 liabilityDetails = this.liabilityDetails,
                 primaryContactDetails = this.primaryContactDetails,
                 organisationDetails = this.organisationDetails,
                 metaData = this.metaData
    )

  def checkAndSubmitStatus: TaskStatus =
    if (isRegistrationComplete)
      TaskStatus.Completed
    else if (metaData.registrationReviewed)
      TaskStatus.InProgress
    else if (isCheckAndSubmitReady)
      TaskStatus.NotStarted
    else
      TaskStatus.CannotStartYet

  def numberOfCompletedSections: Int =
    Array(isCompanyDetailsComplete,
          isLiabilityDetailsComplete,
          isPrimaryContactDetailsComplete,
          isRegistrationComplete
    ).count(p => p)

  def isRegistrationComplete: Boolean =
    isCheckAndSubmitReady && metaData.registrationCompleted

  def isCheckAndSubmitReady: Boolean =
    isCompanyDetailsComplete && isLiabilityDetailsComplete && isPrimaryContactDetailsComplete

  def isCompanyDetailsComplete: Boolean = companyDetailsStatus == TaskStatus.Completed

  def companyDetailsStatus: TaskStatus =
    if (!isLiabilityDetailsComplete)
      TaskStatus.CannotStartYet
    else organisationDetails.status

  def isLiabilityDetailsComplete: Boolean = liabilityDetailsStatus == TaskStatus.Completed

  def liabilityDetailsStatus: TaskStatus =
    this.liabilityDetails.status

  def isPrimaryContactDetailsComplete: Boolean = primaryContactDetailsStatus == TaskStatus.Completed

  def primaryContactDetailsStatus: TaskStatus =
    if (companyDetailsStatus != TaskStatus.Completed)
      TaskStatus.CannotStartYet
    else
      this.primaryContactDetails.status(metaData.emailVerified)

  def asCompleted(): Registration =
    this.copy(metaData = this.metaData.copy(registrationCompleted = true))

  val isStarted: Boolean = liabilityDetails.status != TaskStatus.NotStarted
}

object Registration {
  implicit val format: OFormat[Registration] = Json.format[Registration]

}
