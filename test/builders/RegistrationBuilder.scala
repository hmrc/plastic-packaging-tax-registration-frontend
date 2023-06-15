/*
 * Copyright 2023 HM Revenue & Customs
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

import forms.contact.Address
import forms.liability.RegType.{GROUP, RegType}
import forms.liability.{LiabilityWeight, RegType}
import forms.organisation.OrgType.{PARTNERSHIP, SOLE_TRADER, UK_COMPANY}
import forms.{Date, OldDate}
import models.addresslookup.CountryCode.GB
import models.emailverification.EmailStatus
import models.genericregistration._
import models.registration._
import models.subscriptions.SubscriptionStatus.NOT_SUBSCRIBED
import services.CountryService
import utils.AddressConversionUtils

import java.time.LocalDate

//noinspection ScalaStyle
trait RegistrationBuilder {

  val addressConversionUtils: AddressConversionUtils = new AddressConversionUtils(new CountryService)

  private type RegistrationModifier = Registration => Registration

  def aRegistration(modifiers: RegistrationModifier*): Registration =
    modifiers.foldLeft(modelWithDefaults)((current, modifier) => modifier(current))

  private def modelWithDefaults: Registration =
    Registration(
      id = "id",
      incorpJourneyId = Some("some-journey-id"),
      registrationType = Some(RegType.SINGLE_ENTITY),
      liabilityDetails = LiabilityDetails(
        exceededThresholdWeight = Some(false),
        expectToExceedThresholdWeight = Some(true),
        dateRealisedExpectedToExceedThresholdWeight =
          Some(Date(LocalDate.parse("2022-03-05"))),
        expectedWeightNext12m =
          Some(LiabilityWeight(Some(12000))),
        startDate =
          Some(OldDate(Some(1), Some(4), Some(2022))),
        isLiable = Some(true),
        newLiabilityFinished = Some(NewLiability),
        newLiabilityStarted = Some(NewLiability)
      ),
      primaryContactDetails = PrimaryContactDetails(
        name = Some("Jack Gatsby"),
        jobTitle = Some("Developer"),
        email = Some("test@test.com"),
        phoneNumber = Some("0203 4567 890"),
        address = Some(
          Address(
            addressLine1 =
              "2 Scala Street",
            addressLine2 =
              Some("Soho"),
            addressLine3 = None,
            townOrCity = "London",
            maybePostcode = Some("W1T 2HN"),
            countryCode = GB
          )
        ),
        journeyId = Some("journey-id")
      ),
      organisationDetails = OrganisationDetails(
        organisationType = Some(UK_COMPANY),
        businessRegisteredAddress =
          Some(
            Address(
              addressLine1 =
                "2 Scala Street",
              addressLine2 = Some("Soho"),
              addressLine3 = None,
              townOrCity = "London",
              maybePostcode = Some("W1T 2HN"),
              countryCode = GB
            )
          ),
        incorporationDetails =
          Some(
            IncorporationDetails(
              companyNumber = "12345678",
              companyName =
                "Plastic Packaging Ltd",
              ctutr = Some("1234567890"),
              companyAddress =
                IncorporationAddressDetails(),
              registration =
                Some(
                  RegistrationDetails(
                    identifiersMatch = true,
                    verificationStatus =
                      Some("PASS"),
                    registrationStatus =
                      "REGISTERED",
                    registeredBusinessPartnerId =
                      Some("X00000123456789")
                  )
                )
            )
          ),
        subscriptionStatus = Some(NOT_SUBSCRIBED)
      ),
      metaData =
        MetaData(verifiedEmails =
          Seq(EmailStatus(emailAddress = "test@test.com", verified = true, locked = false))
        )
    )


  def withId(id: String): RegistrationModifier = _.copy(id = id)

  def withIncorpJourneyId(incorpJourneyId: Option[String]): RegistrationModifier =
    _.copy(incorpJourneyId = incorpJourneyId)

  def withIncorpDetails(incorp: IncorporationDetails): RegistrationModifier =
    r => r.copy(organisationDetails = r.organisationDetails.copy(incorporationDetails = Some(incorp)))

  def withRegistrationType(registrationType: Option[RegType]): RegistrationModifier =
    _.copy(registrationType = registrationType)

  def withLiabilityDetails(liabilityDetails: LiabilityDetails): RegistrationModifier =
    _.copy(liabilityDetails = liabilityDetails)

  def withNoLiabilityDetails(): RegistrationModifier =
    _.copy(liabilityDetails = LiabilityDetails())

  def withPrimaryContactDetails(primaryContactDetails: PrimaryContactDetails): RegistrationModifier =
    _.copy(primaryContactDetails = primaryContactDetails)

  def withNoPrimaryContactDetails(): RegistrationModifier =
    _.copy(primaryContactDetails = PrimaryContactDetails())

  def withMetaData(metaData: MetaData): RegistrationModifier =
    _.copy(metaData = metaData)

  def withUserHeaders(headers: Map[String, String]): RegistrationModifier =
    _.copy(userHeaders = Some(headers))

  def withOrganisationDetails(organisationDetails: OrganisationDetails): RegistrationModifier = {
    reg =>
      val updatedRegistration = reg.copy(organisationDetails = organisationDetails)
      if (updatedRegistration.organisationDetails.businessRegisteredAddress.isEmpty)
        updatedRegistration.populateBusinessRegisteredAddress(addressConversionUtils)
      else
        updatedRegistration
  }

  def withRegisteredBusinessAddress(businessAddress: Address): RegistrationModifier =
    reg =>
      reg.copy(organisationDetails =
        reg.organisationDetails.copy(businessRegisteredAddress = Some(businessAddress))
      )

  def withSoleTraderDetails(soleTraderDetails: Option[SoleTraderDetails]): RegistrationModifier = {
    registration =>
      registration.copy(organisationDetails =
        registration.organisationDetails.copy(organisationType = Some(SOLE_TRADER), soleTraderDetails = soleTraderDetails)
      )
  }

  def withPartnershipDetails(partnershipDetails: Option[PartnershipDetails]): RegistrationModifier =
    reg =>
      reg.copy(organisationDetails =
        reg.organisationDetails.copy(organisationType = Some(PARTNERSHIP), partnershipDetails = partnershipDetails)
      )

  def withGroupDetail(groupDetail: Option[GroupDetail]): RegistrationModifier =
    _.copy(registrationType = Some(GROUP), groupDetail = groupDetail)

}
