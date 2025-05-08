/*
 * Copyright 2025 HM Revenue & Customs
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

package audit

import play.api.libs.json.{Json, OFormat}
import forms.liability.RegType.RegType
import models.registration._

import java.time.LocalDate

case class CreateRegistrationEvent(
  id: String,
  pptReference: Option[String],
  dateOfRegistration: Option[LocalDate],
  incorpJourneyId: Option[String] = None,
  registrationType: Option[RegType],
  liabilityDetails: LiabilityDetails = LiabilityDetails(),
  primaryContactDetails: PrimaryContactDetails = PrimaryContactDetails(),
  organisationDetails: OrganisationDetails = OrganisationDetails(),
  groupDetail: Option[GroupDetail],
  metaData: MetaData = MetaData(),
  userHeaders: Map[String, String] = Map.empty,
  internalId: Option[String] = None,
  isGroup: Boolean
)

object CreateRegistrationEvent {
  implicit val format: OFormat[CreateRegistrationEvent] = Json.format[CreateRegistrationEvent]
  val eventType: String                                 = "createPPTRegistration"

  def apply(
    registration: Registration,
    pptReference: Option[String],
    internalId: Option[String]
  ): CreateRegistrationEvent =
    new CreateRegistrationEvent(
      id = registration.id,
      pptReference = pptReference,
      dateOfRegistration = registration.dateOfRegistration,
      incorpJourneyId = registration.incorpJourneyId,
      registrationType = registration.registrationType,
      liabilityDetails = registration.liabilityDetails,
      primaryContactDetails = registration.primaryContactDetails,
      organisationDetails = registration.organisationDetails,
      groupDetail = registration.groupDetail,
      metaData = registration.metaData,
      internalId = internalId,
      isGroup = registration.isGroup
    )

}
