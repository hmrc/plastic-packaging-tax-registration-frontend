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
  liabilityDetails: LiabilityDetails = LiabilityDetails()
) {

  def toRegistration: Registration =
    Registration(id = this.id,
                 incorpJourneyId = this.incorpJourneyId,
                 liabilityDetails = this.liabilityDetails
    )

  def isRegistrationComplete: Boolean =
    isCompanyDetailsComplete && isLiabilityDetailsComplete && isPrimaryContactDetailsComplete && isCheckAndSubmitComplete

  def isCheckAndSubmitComplete: Boolean = checkAndSubmitStatus == TaskStatus.Completed

  def checkAndSubmitStatus: TaskStatus =
    if (isCompanyDetailsComplete && isLiabilityDetailsComplete && isPrimaryContactDetailsComplete)
      TaskStatus.NotStarted
    else
      TaskStatus.CannotStartYet

  def numberOfCompletedSections: Int =
    Array(isCompanyDetailsComplete,
          isLiabilityDetailsComplete,
          isPrimaryContactDetailsComplete
    ).count(p => p)

  def isCompanyDetailsComplete: Boolean = companyDetailsStatus == TaskStatus.Completed

  def companyDetailsStatus: TaskStatus =
    if (incorpJourneyId.isEmpty)
      TaskStatus.NotStarted
    else
      TaskStatus.Completed

  def isLiabilityDetailsComplete: Boolean = liabilityDetailsStatus == TaskStatus.Completed

  def liabilityDetailsStatus: TaskStatus =
    if (incorpJourneyId.isEmpty)
      TaskStatus.CannotStartYet
    else
      this.liabilityDetails.status

  def isPrimaryContactDetailsComplete: Boolean = primaryContactDetailsStatus == TaskStatus.Completed

  def primaryContactDetailsStatus: TaskStatus =
    if (incorpJourneyId.isEmpty)
      TaskStatus.CannotStartYet
    else
      TaskStatus.NotStarted

}

object Registration {
  implicit val format: OFormat[Registration] = Json.format[Registration]

}
