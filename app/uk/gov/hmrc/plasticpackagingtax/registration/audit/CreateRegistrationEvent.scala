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

package uk.gov.hmrc.plasticpackagingtax.registration.audit

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration._

case class CreateRegistrationEvent(
  id: String,
  pptReference: Option[String],
  incorpJourneyId: Option[String] = None,
  liabilityDetails: LiabilityDetails = LiabilityDetails(),
  primaryContactDetails: PrimaryContactDetails = PrimaryContactDetails(),
  organisationDetails: OrganisationDetails = OrganisationDetails(),
  metaData: MetaData = MetaData(),
  userHeaders: Map[String, String] = Map.empty
)

object CreateRegistrationEvent {
  implicit val format: OFormat[CreateRegistrationEvent] = Json.format[CreateRegistrationEvent]
  val eventType: String                                 = "CREATE_PPT_REGISTRATION"

  def apply(registration: Registration, pptReference: Option[String]): CreateRegistrationEvent =
    CreateRegistrationEvent(id = registration.id,
                            pptReference = pptReference,
                            incorpJourneyId = registration.incorpJourneyId,
                            liabilityDetails = registration.liabilityDetails,
                            primaryContactDetails = registration.primaryContactDetails,
                            organisationDetails = registration.organisationDetails,
                            metaData = registration.metaData
    )

}
