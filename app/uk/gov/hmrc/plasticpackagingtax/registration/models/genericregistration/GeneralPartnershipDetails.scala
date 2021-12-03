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

import play.api.libs.json.{Json, OFormat}

case class GrsGeneralPartnershipDetails(
  sautr: String,
  postcode: String,
  override val identifiersMatch: Boolean,
  override val businessVerification: Option[GrsBusinessVerification],
  override val registration: GrsRegistration
) extends GrsResponse

object GrsGeneralPartnershipDetails {

  implicit val format: OFormat[GrsGeneralPartnershipDetails] =
    Json.format[GrsGeneralPartnershipDetails]

}

case class GeneralPartnershipDetails(
  sautr: String,
  postcode: String,
  override val registration: Option[RegistrationDetails]
) extends HasRegistrationDetails

object GeneralPartnershipDetails {
  implicit val format: OFormat[GeneralPartnershipDetails] = Json.format[GeneralPartnershipDetails]

  def apply(grsGeneralPartnershipDetails: GrsGeneralPartnershipDetails): GeneralPartnershipDetails =
    GeneralPartnershipDetails(sautr = grsGeneralPartnershipDetails.sautr,
                              postcode = grsGeneralPartnershipDetails.postcode,
                              registration = Some(
                                RegistrationDetails(
                                  identifiersMatch = grsGeneralPartnershipDetails.identifiersMatch,
                                  verificationStatus =
                                    grsGeneralPartnershipDetails.businessVerification.map { bv =>
                                      bv.verificationStatus
                                    },
                                  registrationStatus =
                                    grsGeneralPartnershipDetails.registration.registrationStatus,
                                  registeredBusinessPartnerId =
                                    grsGeneralPartnershipDetails.registration.registeredBusinessPartnerId
                                )
                              )
    )

}
