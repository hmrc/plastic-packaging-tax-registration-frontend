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

case class GrsCompanyProfile(
  companyNumber: String,
  companyName: String,
  unsanitisedCHROAddress: IncorporationAddressDetails
)

object GrsCompanyProfile {
  implicit val format: OFormat[GrsCompanyProfile] = Json.format[GrsCompanyProfile]
}

case class GrsBusinessVerification(verificationStatus: String)

object GrsBusinessVerification {
  implicit val format: OFormat[GrsBusinessVerification] = Json.format[GrsBusinessVerification]
}

case class GrsIncorporationDetails(
  companyProfile: GrsCompanyProfile,
  ctutr: String,
  businessVerification: GrsBusinessVerification,
  registration: IncorporationRegistrationDetails
)

object GrsIncorporationDetails {
  implicit val format: OFormat[GrsIncorporationDetails] = Json.format[GrsIncorporationDetails]
}

case class IncorporationDetails(
  companyNumber: String,
  companyName: String,
  ctutr: String,
  businessVerificationStatus: String,
  companyAddress: IncorporationAddressDetails,
  override val registration: IncorporationRegistrationDetails
) extends RegistrationDetails

object IncorporationDetails {

  implicit val format: Format[IncorporationDetails] = Json.format[IncorporationDetails]

  def apply(grsIncorporationDetails: GrsIncorporationDetails): IncorporationDetails =
    IncorporationDetails(grsIncorporationDetails.companyProfile.companyNumber,
                         grsIncorporationDetails.companyProfile.companyName,
                         grsIncorporationDetails.ctutr,
                         grsIncorporationDetails.businessVerification.verificationStatus,
                         grsIncorporationDetails.companyProfile.unsanitisedCHROAddress,
                         grsIncorporationDetails.registration
    )

}
