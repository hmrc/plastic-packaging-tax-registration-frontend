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

import base.PptTestData.testUserFeatures
import base.{MockAuthAction, PptTestData}
import builders.RegistrationBuilder
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Address
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrgType.{
  PARTNERSHIP,
  SOLE_TRADER,
  UK_COMPANY
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.PartnershipTypeEnum.{
  GENERAL_PARTNERSHIP,
  PartnershipTypeEnum,
  SCOTTISH_PARTNERSHIP
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.{
  EmailStatus,
  VerificationStatus
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration._
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.OrganisationDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{
  AuthenticatedRequest,
  JourneyRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.{
  ETMPSubscriptionStatus,
  SubscriptionCreateResponseSuccess,
  SubscriptionStatus
}
import utils.FakeRequestCSRFSupport.CSRFFakeRequest

import java.time.{ZoneOffset, ZonedDateTime}
import scala.language.implicitConversions

trait PptTestData extends RegistrationBuilder with MockAuthAction {

  implicit val journeyRequest: JourneyRequest[AnyContent] =
    new JourneyRequest(
      authenticatedRequest =
        new AuthenticatedRequest(FakeRequest().withCSRFToken, PptTestData.newUser()),
      registration = aRegistration(),
      appConfig = appConfig
    )

  implicit def generateRequest(
    userFeatureFlags: Map[String, Boolean] = testUserFeatures
  ): JourneyRequest[AnyContent] =
    new JourneyRequest(authenticatedRequest =
                         new AuthenticatedRequest(
                           FakeRequest().withCSRFToken,
                           PptTestData.newUser(featureFlags = userFeatureFlags)
                         ),
                       registration = aRegistration(),
                       appConfig = appConfig
    )

  val request: Request[AnyContent] = FakeRequest().withCSRFToken

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

  protected val testBusinessVerificationPassStatus = "PASS"

  protected val incorporationRegistrationDetails: IncorporationRegistrationDetails =
    IncorporationRegistrationDetails(registeredBusinessPartnerId =
                                       Some(safeNumber),
                                     registrationStatus = "REGISTERED"
    )

  protected val unregisteredIncorporationRegistrationDetails: IncorporationRegistrationDetails =
    IncorporationRegistrationDetails(registeredBusinessPartnerId = None,
                                     registrationStatus = "REGISTRATION_NOT_CALLED"
    )

  protected val incorporationDetails: IncorporationDetails =
    IncorporationDetails(testCompanyNumber,
                         testCompanyName,
                         testUtr,
                         testBusinessVerificationPassStatus,
                         testCompanyAddress,
                         incorporationRegistrationDetails
    )

  protected val unregisteredIncorporationDetails: IncorporationDetails =
    IncorporationDetails(testCompanyNumber,
                         testCompanyName,
                         testUtr,
                         testBusinessVerificationPassStatus,
                         testCompanyAddress,
                         unregisteredIncorporationRegistrationDetails
    )

  protected val verificationFailedIncorporationDetails: IncorporationDetails =
    IncorporationDetails(testCompanyNumber,
                         testCompanyName,
                         testUtr,
                         "FAIL",
                         testCompanyAddress,
                         unregisteredIncorporationRegistrationDetails
    )

  protected val unregisteredSoleTraderDetails: SoleTraderIncorporationDetails =
    SoleTraderIncorporationDetails(firstName = "Sole",
                                   lastName = "Trader",
                                   dateOfBirth = "12/12/1960",
                                   nino = "1234",
                                   sautr = Some("ABC"),
                                   registration = unregisteredIncorporationRegistrationDetails
    )

  protected val soleTraderIncorporationDetails: SoleTraderIncorporationDetails =
    SoleTraderIncorporationDetails(testFirstName,
                                   testLastName,
                                   testDob,
                                   testNino,
                                   Some(testSatur),
                                   incorporationRegistrationDetails
    )

  protected val generalPartnershipDetails: GeneralPartnershipDetails =
    GeneralPartnershipDetails(testSatur, testPostcode, incorporationRegistrationDetails)

  protected val scottishPartnershipDetails: ScottishPartnershipDetails =
    ScottishPartnershipDetails(testSatur, testPostcode, incorporationRegistrationDetails)

  protected val partnershipDetails: PartnershipDetails =
    PartnershipDetails(partnershipType = GENERAL_PARTNERSHIP,
                       generalPartnershipDetails = Some(
                         GeneralPartnershipDetails(testSatur,
                                                   testPostcode,
                                                   incorporationRegistrationDetails
                         )
                       )
    )

  protected val partnershipDetailsWithScottishPartnership: PartnershipDetails =
    PartnershipDetails(partnershipType = SCOTTISH_PARTNERSHIP,
                       scottishPartnershipDetails = Some(
                         ScottishPartnershipDetails(testSatur,
                                                    testPostcode,
                                                    incorporationRegistrationDetails
                         )
                       )
    )

  protected val subscriptionStatus: SubscriptionStatus = SubscriptionStatus(
    subscriptionStatus = ETMPSubscriptionStatus.NO_FORM_BUNDLE_FOUND,
    idValue = "XXPPTP123456789",
    idType = "ZPPT"
  )

  protected val nrsSubmissionId = "nrs-id-999"

  protected val subscriptionCreate: SubscriptionCreateResponseSuccess =
    SubscriptionCreateResponseSuccess(pptReference = "XXPPTP123456789",
                                      processingDate =
                                        ZonedDateTime.now(ZoneOffset.UTC),
                                      formBundleNumber = "123456789",
                                      nrsNotifiedSuccessfully = true,
                                      nrsSubmissionId = Some(nrsSubmissionId),
                                      nrsFailureReason = None,
                                      enrolmentInitiatedSuccessfully = true
    )

  protected val emailVerification: VerificationStatus = VerificationStatus(
    Seq(EmailStatus(emailAddress = "test@hmrc.com", verified = true, locked = false))
  )

  protected def registeredUkCompanyOrgDetails(): OrganisationDetails =
    OrganisationDetails(isBasedInUk = Some(true),
                        organisationType = Some(UK_COMPANY),
                        businessRegisteredAddress = Some(testBusinessAddress),
                        safeNumber = Some(safeNumber),
                        incorporationDetails = Some(incorporationDetails)
    )

  protected def registeredSoleTraderOrgDetails(): OrganisationDetails =
    OrganisationDetails(isBasedInUk = Some(true),
                        organisationType = Some(SOLE_TRADER),
                        businessRegisteredAddress = Some(testBusinessAddress),
                        safeNumber = Some(safeNumber),
                        soleTraderDetails = Some(soleTraderIncorporationDetails)
    )

  protected def registeredGeneralPartnershipOrgDetails(): OrganisationDetails =
    OrganisationDetails(isBasedInUk = Some(true),
                        organisationType = Some(PARTNERSHIP),
                        businessRegisteredAddress = Some(testBusinessAddress),
                        safeNumber = Some(safeNumber),
                        partnershipDetails = Some(partnershipDetails)
    )

  protected def registeredScottishPartnershipOrgDetails(): OrganisationDetails =
    OrganisationDetails(isBasedInUk = Some(true),
                        organisationType = Some(PARTNERSHIP),
                        businessRegisteredAddress = Some(testBusinessAddress),
                        safeNumber = Some(safeNumber),
                        partnershipDetails = Some(partnershipDetailsWithScottishPartnership)
    )

  protected def unregisteredUkCompanyOrgDetails(): OrganisationDetails =
    OrganisationDetails(isBasedInUk = Some(true),
                        organisationType = Some(UK_COMPANY),
                        businessRegisteredAddress = Some(testBusinessAddress),
                        safeNumber = None,
                        incorporationDetails = Some(unregisteredIncorporationDetails)
    )

  protected def unregisteredSoleTraderOrgDetails(): OrganisationDetails =
    OrganisationDetails(isBasedInUk = Some(true),
                        organisationType = Some(SOLE_TRADER),
                        businessRegisteredAddress = Some(testBusinessAddress),
                        safeNumber = None,
                        soleTraderDetails = Some(unregisteredSoleTraderDetails)
    )

  protected def unregisteredPartnershipDetails(
    partnershipType: PartnershipTypeEnum
  ): OrganisationDetails =
    OrganisationDetails(isBasedInUk = Some(true),
                        organisationType = Some(PARTNERSHIP),
                        partnershipDetails = Some(PartnershipDetails(partnershipType))
    )

  protected def verificationFailedUkCompanyOrgDetails(): OrganisationDetails =
    OrganisationDetails(isBasedInUk = Some(true),
                        organisationType = Some(UK_COMPANY),
                        businessRegisteredAddress = Some(testBusinessAddress),
                        safeNumber = Some(safeNumber),
                        incorporationDetails = Some(verificationFailedIncorporationDetails)
    )

}
