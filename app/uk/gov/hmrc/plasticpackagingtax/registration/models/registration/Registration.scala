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

import org.joda.time.DateTime
import uk.gov.hmrc.plasticpackagingtax.registration.views.model.TaskStatus
import play.api.libs.json.{Format, JodaReads, JodaWrites, JsResult, JsValue, Json, OFormat}

case class Registration(
  id: String,
  incorpJourneyId: Option[String] = None,
  liabilityDetails: LiabilityDetails = LiabilityDetails(),
  primaryContactDetails: PrimaryContactDetails = PrimaryContactDetails(),
  organisationDetails: OrganisationDetails = OrganisationDetails(),
  metaData: MetaData = MetaData(),
  lastModifiedDateTime: Option[DateTime] = None
) {

  def toRegistration: Registration =
    Registration(id = this.id,
                 incorpJourneyId = this.incorpJourneyId,
                 liabilityDetails = this.liabilityDetails,
                 primaryContactDetails = this.primaryContactDetails,
                 organisationDetails = this.organisationDetails,
                 metaData = this.metaData,
                 lastModifiedDateTime = this.lastModifiedDateTime
    )

  def isRegistrationComplete: Boolean =
    isCheckAndSubmitReady && metaData.registrationCompleted

  def isCheckAndSubmitReady: Boolean =
    isCompanyDetailsComplete && isLiabilityDetailsComplete && isPrimaryContactDetailsComplete

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
      this.primaryContactDetails.status

}

object Registration {

  implicit val dateFormatDefault: Format[DateTime] = new Format[DateTime] {

    override def reads(json: JsValue): JsResult[DateTime] =
      JodaReads.DefaultJodaDateTimeReads.reads(json)

    override def writes(o: DateTime): JsValue = JodaWrites.JodaDateTimeNumberWrites.writes(o)
  }

  implicit val format: OFormat[Registration] = Json.format[Registration]
}
