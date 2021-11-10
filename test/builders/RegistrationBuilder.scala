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

package builders

import java.util.UUID
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrgType.{PARTNERSHIP, UK_COMPANY}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.RegType.RegType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.{
  Address,
  Date,
  LiabilityExpectedWeight,
  LiabilityWeight
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.EmailStatus
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  IncorporationAddressDetails,
  IncorporationDetails,
  IncorporationRegistrationDetails,
  PartnershipDetails
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration._
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.SubscriptionStatus.NOT_SUBSCRIBED

//noinspection ScalaStyle
trait RegistrationBuilder {

  private type RegistrationModifier = Registration => Registration

  def aRegistration(modifiers: RegistrationModifier*): Registration =
    modifiers.foldLeft(modelWithDefaults)((current, modifier) => modifier(current))

  private def modelWithDefaults: Registration =
    Registration(id = "id",
                 incorpJourneyId = Some(UUID.randomUUID().toString),
                 liabilityDetails = LiabilityDetails(weight = Some(LiabilityWeight(Some(1000))),
                                                     expectedWeight = Some(
                                                       LiabilityExpectedWeight(Some(true),
                                                                               Some(12000)
                                                       )
                                                     ),
                                                     startDate =
                                                       Some(Date(Some(1), Some(4), Some(2022))),
                                                     isLiable = Some(true),
                                                     expectToExceedThresholdWeight = Some(true)
                 ),
                 primaryContactDetails = PrimaryContactDetails(name = Some("Jack Gatsby"),
                                                               jobTitle = Some("Developer"),
                                                               email = Some("test@test.com"),
                                                               phoneNumber = Some("0203 4567 890"),
                                                               address = Some(
                                                                 Address(addressLine1 =
                                                                           "2 Scala Street",
                                                                         addressLine2 = "Soho",
                                                                         townOrCity = "London",
                                                                         postCode = "W1T 2HN"
                                                                 )
                                                               ),
                                                               journeyId = Some("journey-id")
                 ),
                 organisationDetails = OrganisationDetails(organisationType = Some(UK_COMPANY),
                                                           businessRegisteredAddress =
                                                             Some(
                                                               Address(addressLine1 =
                                                                         "2 Scala Street",
                                                                       addressLine2 = "Soho",
                                                                       townOrCity = "London",
                                                                       postCode = "W1T 2HN"
                                                               )
                                                             ),
                                                           incorporationDetails =
                                                             Some(
                                                               IncorporationDetails(
                                                                 companyNumber = "12345678",
                                                                 companyName =
                                                                   "Plastic Packaging Ltd",
                                                                 ctutr = "1234567890",
                                                                 businessVerificationStatus =
                                                                   "PASS",
                                                                 companyAddress =
                                                                   IncorporationAddressDetails(),
                                                                 registration =
                                                                   IncorporationRegistrationDetails(
                                                                     registrationStatus =
                                                                       "REGISTERED",
                                                                     registeredBusinessPartnerId =
                                                                       Some("X00000123456789")
                                                                   )
                                                               )
                                                             ),
                                                           subscriptionStatus = Some(NOT_SUBSCRIBED)
                 ),
                 metaData =
                   MetaData(verifiedEmails =
                     Seq(
                       EmailStatus(emailAddress = "test@test.com", verified = true, locked = false)
                     )
                   )
    )

  def withId(id: String): RegistrationModifier = _.copy(id = id)

  def withIncorpJourneyId(incorpJourneyId: Option[String]): RegistrationModifier =
    _.copy(incorpJourneyId = incorpJourneyId)

  def withRegistrationType(registrationType: Option[RegType]): RegistrationModifier =
    registration => registration.copy(registrationType = registrationType)

  def withLiabilityDetails(liabilityDetails: LiabilityDetails): RegistrationModifier =
    _.copy(liabilityDetails = liabilityDetails)

  def withNoLiabilityDetails(): RegistrationModifier =
    _.copy(liabilityDetails = LiabilityDetails())

  def withPrimaryContactDetails(
    primaryContactDetails: PrimaryContactDetails
  ): RegistrationModifier =
    _.copy(primaryContactDetails = primaryContactDetails)

  def withNoPrimaryContactDetails(): RegistrationModifier =
    _.copy(primaryContactDetails = PrimaryContactDetails())

  def withMetaData(metaData: MetaData): RegistrationModifier =
    _.copy(metaData = metaData)

  def withUserHeaders(headers: Map[String, String]): RegistrationModifier =
    _.copy(userHeaders = Some(headers))

  def withOrganisationDetails(organisationDetails: OrganisationDetails): RegistrationModifier =
    _.copy(organisationDetails = organisationDetails)

  def withPartnershipDetails(partnershipDetails: Option[PartnershipDetails]): RegistrationModifier =
    registration =>
      registration.copy(organisationDetails =
        registration.organisationDetails.copy(organisationType = Some(PARTNERSHIP),
                                              partnershipDetails = partnershipDetails
        )
      )

  def withRegistrationType(registrationType: Option[RegType]): RegistrationModifier =
    registration => registration.copy(registrationType = registrationType)

  def withGroupDetail(groupDetail: Option[GroupDetail]): RegistrationModifier =
    registration => registration.copy(groupDetail = groupDetail)

}
