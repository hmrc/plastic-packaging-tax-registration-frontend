/*
 * Copyright 2024 HM Revenue & Customs
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

package models.genericregistration

import play.api.libs.json._

case class GrsCompanyProfile(
  companyNumber: String,
  companyName: String,
  unsanitisedCHROAddress: IncorporationAddressDetails
)

object GrsCompanyProfile {
  implicit val format: OFormat[GrsCompanyProfile] = Json.format[GrsCompanyProfile]
}

case class GrsIncorporationDetails(
  companyProfile: GrsCompanyProfile,
  ctutr: String,
  override val identifiersMatch: Boolean,
  override val businessVerification: Option[GrsBusinessVerification],
  override val registration: GrsRegistration
) extends GrsResponse

object GrsIncorporationDetails {
  implicit val format: OFormat[GrsIncorporationDetails] = Json.format[GrsIncorporationDetails]
}

case class IncorporationDetails(
  companyNumber: String,
  companyName: String,
  ctutr: Option[String],
  companyAddress: IncorporationAddressDetails,
  override val registration: Option[RegistrationDetails]
) extends HasRegistrationDetails {

  def isGroupMemberSameAsNominated(customerIdentification1: String): Boolean =
    companyNumber.equalsIgnoreCase(customerIdentification1)

}

object IncorporationDetails {

  implicit val format: Format[IncorporationDetails] = Json.format[IncorporationDetails]

  def apply(grsIncorporationDetails: GrsIncorporationDetails): IncorporationDetails =
    IncorporationDetails(grsIncorporationDetails.companyProfile.companyNumber,
                         grsIncorporationDetails.companyProfile.companyName,
                         Some(grsIncorporationDetails.ctutr),
                         grsIncorporationDetails.companyProfile.unsanitisedCHROAddress,
                         Some(
                           RegistrationDetails(
                             identifiersMatch =
                               grsIncorporationDetails.identifiersMatch,
                             verificationStatus =
                               grsIncorporationDetails.businessVerification.map { bv =>
                                 bv.verificationStatus
                               },
                             registrationStatus =
                               grsIncorporationDetails.registration.registrationStatus,
                             registeredBusinessPartnerId =
                               grsIncorporationDetails.registration.registeredBusinessPartnerId
                           )
                         )
    )

}
