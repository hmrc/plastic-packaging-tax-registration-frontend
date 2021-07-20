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

package spec

import uk.gov.hmrc.plasticpackagingtax.registration.forms.{OrgType}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Address
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.{
  EmailStatus,
  VerificationStatus
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  IncorporationAddressDetails,
  IncorporationDetails,
  IncorporationRegistrationDetails,
  PartnershipDetails,
  SoleTraderIncorporationDetails
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.OrganisationDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.{
  ETMPSubscriptionStatus,
  SubscriptionCreateResponse,
  SubscriptionStatus
}

import java.time.{ZoneOffset, ZonedDateTime}

trait PptTestData {

  protected val testCompanyName   = "Example Limited"
  protected val testCompanyNumber = "123456789"
  protected val testFirstName     = "John"
  protected val testLastName      = "Rambo"
  protected val testDob           = "12/09/1967"
  protected val testNino          = "SE12345678"
  protected val testSatur         = "123456789"
  protected val testPostcode      = "AA1 1AA"
  protected val safeNumber        = "XXPPTP123456789"
  protected val testUserHeaders   = Map("Host" -> "localhost", "headerKey" -> "headerValue")

  protected val testCompanyAddress = IncorporationAddressDetails(address_line_1 = Some("testLine1"),
                                                                 address_line_2 = Some("testLine2"),
                                                                 locality = Some("test town"),
                                                                 care_of = Some("test name"),
                                                                 po_box = Some("123"),
                                                                 postal_code = Some("AA11AA"),
                                                                 premises = Some("1"),
                                                                 country = Some("United Kingdom")
  )

  protected val testBusinessAddress = Address(addressLine1 = "2 Scala Street",
                                              addressLine2 = Some("Soho"),
                                              townOrCity = "London",
                                              postCode = "W1T 2HN"
  )

  protected val testUtr = "0123456789"

  protected val incorporationRegistrationDetails: IncorporationRegistrationDetails =
    IncorporationRegistrationDetails(registeredBusinessPartnerId =
                                       Some(safeNumber),
                                     registrationStatus = "REGISTERED"
    )

  protected val incorporationDetails: IncorporationDetails =
    IncorporationDetails(testCompanyNumber,
                         testCompanyName,
                         testUtr,
                         testCompanyAddress,
                         incorporationRegistrationDetails
    )

  protected val soleTraderIncorporationDetails: SoleTraderIncorporationDetails =
    SoleTraderIncorporationDetails(testFirstName,
                                   testLastName,
                                   testDob,
                                   testNino,
                                   Some(testSatur),
                                   incorporationRegistrationDetails
    )

  protected val partnershipDetails: PartnershipDetails =
    PartnershipDetails(testSatur, testPostcode)

  protected val subscriptionStatus: SubscriptionStatus = SubscriptionStatus(
    subscriptionStatus = ETMPSubscriptionStatus.NO_FORM_BUNDLE_FOUND,
    idValue = "XXPPTP123456789",
    idType = "ZPPT"
  )

  protected val subscriptionCreate: SubscriptionCreateResponse = SubscriptionCreateResponse(
    pptReference = "XXPPTP123456789",
    processingDate =
      ZonedDateTime.now(ZoneOffset.UTC),
    formBundleNumber = "123456789"
  )

  protected def registeredUkOrgDetails(orgType: OrgType.Value): OrganisationDetails =
    OrganisationDetails(isBasedInUk = Some(true),
                        organisationType = Some(orgType),
                        businessRegisteredAddress = Some(testBusinessAddress),
                        safeNumber = Some(safeNumber),
                        incorporationDetails = Some(incorporationDetails)
    )

  protected def unregisteredUkOrgDetails(orgType: OrgType.Value): OrganisationDetails =
    OrganisationDetails(isBasedInUk = Some(true), organisationType = Some(orgType))

  protected val emailVerification = VerificationStatus(
    Seq(EmailStatus(emailAddress = "test@hmrc.com", verified = true, locked = false))
  )

}
