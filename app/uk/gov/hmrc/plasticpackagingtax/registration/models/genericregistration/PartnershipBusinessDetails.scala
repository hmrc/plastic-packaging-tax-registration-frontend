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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address

case class CompanyProfile(
  companyNumber: String,
  companyName: String,
  companyAddress: IncorporationAddressDetails
)

object CompanyProfile {
  implicit val format: OFormat[CompanyProfile] = Json.format[CompanyProfile]
}

case class GrsPartnershipBusinessDetails(
  sautr: String,
  postcode: String,
  companyProfile: Option[GrsCompanyProfile],
  override val identifiersMatch: Boolean,
  override val businessVerification: Option[GrsBusinessVerification],
  override val registration: GrsRegistration
) extends GrsResponse

object GrsPartnershipBusinessDetails {

  implicit val format: OFormat[GrsPartnershipBusinessDetails] =
    Json.format[GrsPartnershipBusinessDetails]

}

case class PartnershipBusinessDetails(
  sautr: String,
  postcode: String,
  companyProfile: Option[CompanyProfile],
  override val registration: Option[RegistrationDetails]
) extends HasRegistrationDetails {

  def companyName: Option[String] = companyProfile.map(_.companyName)

  def companyAddress: Option[Address] = companyProfile.map(_.companyAddress.toPptAddress)

  def isGroupMemberSameAsNominatedPartnership(customerIdentification1: String): Boolean =
    companyProfile.exists(_.companyNumber.equalsIgnoreCase(customerIdentification1))

}

object PartnershipBusinessDetails {

  implicit val format: OFormat[PartnershipBusinessDetails] =
    Json.format[PartnershipBusinessDetails]

  def apply(
    grsPartnershipBusinessDetails: GrsPartnershipBusinessDetails
  ): PartnershipBusinessDetails =
    PartnershipBusinessDetails(sautr = grsPartnershipBusinessDetails.sautr,
                               postcode = grsPartnershipBusinessDetails.postcode,
                               companyProfile = toCompanyProfile(grsCompanyProfile =
                                 grsPartnershipBusinessDetails.companyProfile
                               ),
                               registration = Some(
                                 RegistrationDetails(
                                   identifiersMatch =
                                     grsPartnershipBusinessDetails.identifiersMatch,
                                   verificationStatus =
                                     grsPartnershipBusinessDetails.businessVerification.map {
                                       bv =>
                                         bv.verificationStatus
                                     },
                                   registrationStatus =
                                     grsPartnershipBusinessDetails.registration.registrationStatus,
                                   registeredBusinessPartnerId =
                                     grsPartnershipBusinessDetails.registration.registeredBusinessPartnerId
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
