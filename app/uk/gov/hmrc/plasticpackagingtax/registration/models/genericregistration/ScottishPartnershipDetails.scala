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

package uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration

import play.api.libs.json._

case class GrsScottishPartnershipDetails(
  sautr: String,
  postcode: String,
  override val identifiersMatch: Boolean,
  override val businessVerification: GrsBusinessVerification,
  override val registration: GrsRegistration
) extends GrsResponse

object GrsScottishPartnershipDetails {

  implicit val format: OFormat[GrsScottishPartnershipDetails] =
    Json.format[GrsScottishPartnershipDetails]

}

case class ScottishPartnershipDetails(
  sautr: String,
  postcode: String,
  override val registration: RegistrationDetails
) extends HasRegistrationDetails

object ScottishPartnershipDetails {
  implicit val format: OFormat[ScottishPartnershipDetails] = Json.format[ScottishPartnershipDetails]

  def apply(
    grsScottishPartnershipDetails: GrsScottishPartnershipDetails
  ): ScottishPartnershipDetails =
    ScottishPartnershipDetails(sautr = grsScottishPartnershipDetails.sautr,
                               postcode = grsScottishPartnershipDetails.postcode,
                               registration = RegistrationDetails(
                                 identifiersMatch = grsScottishPartnershipDetails.identifiersMatch,
                                 verificationStatus =
                                   grsScottishPartnershipDetails.businessVerification.verificationStatus,
                                 registrationStatus =
                                   grsScottishPartnershipDetails.registration.registrationStatus,
                                 registeredBusinessPartnerId =
                                   grsScottishPartnershipDetails.registration.registeredBusinessPartnerId
                               )
    )

}
