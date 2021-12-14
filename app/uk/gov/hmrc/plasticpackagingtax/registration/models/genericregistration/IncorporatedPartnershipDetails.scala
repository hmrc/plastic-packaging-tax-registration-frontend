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

case class CompanyProfile(
  companyNumber: String,
  companyName: String,
  companyAddress: IncorporationAddressDetails
)

object CompanyProfile {
  implicit val format: OFormat[CompanyProfile] = Json.format[CompanyProfile]
}

case class GrsIncorporatedPartnershipDetails(
  sautr: String,
  postcode: String,
  companyProfile: Option[GrsCompanyProfile],
  override val identifiersMatch: Boolean,
  override val businessVerification: Option[GrsBusinessVerification],
  override val registration: GrsRegistration
) extends GrsResponse

object GrsIncorporatedPartnershipDetails {

  implicit val format: OFormat[GrsIncorporatedPartnershipDetails] =
    Json.format[GrsIncorporatedPartnershipDetails]

}

case class IncorporatedPartnershipDetails(
  sautr: String,
  postcode: String,
  companyProfile: Option[CompanyProfile],
  override val registration: Option[RegistrationDetails]
) extends HasRegistrationDetails

object IncorporatedPartnershipDetails {

  implicit val format: OFormat[IncorporatedPartnershipDetails] =
    Json.format[IncorporatedPartnershipDetails]

  def apply(
    grsLimitedPartnershipDetails: GrsIncorporatedPartnershipDetails
  ): IncorporatedPartnershipDetails =
    IncorporatedPartnershipDetails(sautr = grsLimitedPartnershipDetails.sautr,
                                   postcode = grsLimitedPartnershipDetails.postcode,
                                   companyProfile = toCompanyProfile(grsCompanyProfile =
                                     grsLimitedPartnershipDetails.companyProfile
                                   ),
                                   registration = Some(
                                     RegistrationDetails(
                                       identifiersMatch =
                                         grsLimitedPartnershipDetails.identifiersMatch,
                                       verificationStatus =
                                         grsLimitedPartnershipDetails.businessVerification.map {
                                           bv =>
                                             bv.verificationStatus
                                         },
                                       registrationStatus =
                                         grsLimitedPartnershipDetails.registration.registrationStatus,
                                       registeredBusinessPartnerId =
                                         grsLimitedPartnershipDetails.registration.registeredBusinessPartnerId
                                     )
                                   )
    )

  private def toCompanyProfile(
    grsCompanyProfile: Option[GrsCompanyProfile]
  ): Option[CompanyProfile] =
    grsCompanyProfile match {
      case Some(profile) =>
        Some(
          CompanyProfile(companyNumber = profile.companyNumber,
                         companyName = profile.companyName,
                         companyAddress = profile.unsanitisedCHROAddress
          )
        )
      case None => None
    }

}
