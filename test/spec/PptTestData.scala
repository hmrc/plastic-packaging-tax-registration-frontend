/*
 * Copyright 2022 HM Revenue & Customs
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
import base.{MockAuthAction, PptTestData => TestData}
import builders.RegistrationBuilder
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.plasticpackagingtax.registration.forms.DateData
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.forms.enrolment.{
  IsUkAddress,
  Postcode,
  PptReference,
  RegistrationDate
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType._
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnershipTypeEnum._
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.{
  EmailStatus,
  VerificationStatus
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration._
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.{
  GroupMember,
  GroupMemberContactDetails,
  OrganisationDetails => GroupOrgDetails
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  GroupDetail,
  OrganisationDetails,
  UserEnrolmentDetails
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{
  AuthenticatedRequest,
  JourneyRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.{
  SubscriptionCreateOrUpdateResponseSuccess,
  SubscriptionStatus,
  SubscriptionStatusResponse
}
import utils.FakeRequestCSRFSupport.CSRFFakeRequest

import java.time.{ZoneOffset, ZonedDateTime}
import scala.language.implicitConversions

trait PptTestData extends RegistrationBuilder with MockAuthAction {

  implicit val journeyRequest: JourneyRequest[AnyContent] =
    new JourneyRequest(authenticatedRequest =
                         new AuthenticatedRequest(FakeRequest().withCSRFToken, TestData.newUser()),
                       registration = aRegistration(),
                       appConfig = appConfig
    )

  implicit def generateRequest(
    userFeatureFlags: Map[String, Boolean] = testUserFeatures
  ): JourneyRequest[AnyContent] =
    new JourneyRequest(authenticatedRequest =
                         new AuthenticatedRequest(FakeRequest().withCSRFToken,
                                                  TestData.newUser(featureFlags = userFeatureFlags)
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
                                                                 country = Some("GB")
  )

  protected val testBusinessAddress = Address(addressLine1 = "2 Scala Street",
                                              addressLine2 = Some("Soho"),
                                              townOrCity = "London",
                                              postCode = Some("W1T 2HN")
  )

  protected val testUtr = "0123456789"

  protected val testBusinessVerificationPassStatus = "PASS"

  protected val registrationDetails: RegistrationDetails =
    RegistrationDetails(identifiersMatch = true,
                        verificationStatus = Some("PASS"),
                        registrationStatus = "REGISTERED",
                        registeredBusinessPartnerId = Some(safeNumber)
    )

  protected val unregisteredRegistrationDetails: RegistrationDetails =
    RegistrationDetails(identifiersMatch = true,
                        verificationStatus = Some("UNCHALLENGED"),
                        registrationStatus = "REGISTRATION_NOT_CALLED",
                        registeredBusinessPartnerId = None
    )

  protected val verificationFailedRegistrationDetails: RegistrationDetails =
    RegistrationDetails(identifiersMatch = true,
                        verificationStatus = Some("FAIL"),
                        registrationStatus = "REGISTRATION_NOT_CALLED",
                        registeredBusinessPartnerId = None
    )

  protected val identifiersUnmatchedRegistrationDetails: RegistrationDetails =
    RegistrationDetails(identifiersMatch = false,
                        verificationStatus = Some("UNCHALLENGED"),
                        registrationStatus = "REGISTRATION_NOT_CALLED",
                        registeredBusinessPartnerId = None
    )

  protected val incorporationDetails: IncorporationDetails =
    IncorporationDetails(testCompanyNumber,
                         testCompanyName,
                         testUtr,
                         testCompanyAddress,
                         Some(registrationDetails)
    )

  protected val grsRegistrationDetails: GrsRegistration =
    GrsRegistration(registrationStatus = "REGISTERED",
                    registeredBusinessPartnerId = Some(safeNumber)
    )

  protected val grsIncorporationDetails: GrsIncorporationDetails =
    GrsIncorporationDetails(
      companyProfile = GrsCompanyProfile(testCompanyNumber, testCompanyName, testCompanyAddress),
      ctutr = testUtr,
      identifiersMatch = true,
      businessVerification = Some(GrsBusinessVerification(testBusinessVerificationPassStatus)),
      registration = grsRegistrationDetails
    )

  protected val unregisteredIncorporationDetails: IncorporationDetails =
    IncorporationDetails(testCompanyNumber,
                         testCompanyName,
                         testUtr,
                         testCompanyAddress,
                         Some(unregisteredRegistrationDetails)
    )

  protected val verificationFailedIncorporationDetails: IncorporationDetails =
    IncorporationDetails(testCompanyNumber,
                         testCompanyName,
                         testUtr,
                         testCompanyAddress,
                         Some(verificationFailedRegistrationDetails)
    )

  protected val unregisteredSoleTraderDetails: SoleTraderDetails =
    SoleTraderDetails(firstName = "Sole",
                      lastName = "Trader",
                      dateOfBirth = Some("12/12/1960"),
                      ninoOrTrn = "1234",
                      sautr = Some("ABC"),
                      registration = Some(unregisteredRegistrationDetails)
    )

  protected val soleTraderDetails: SoleTraderDetails =
    SoleTraderDetails(testFirstName,
                      testLastName,
                      Some(testDob),
                      testNino,
                      Some(testSatur),
                      Some(registrationDetails)
    )

  protected val grsSoleTraderIncorporationDetails: GrsSoleTraderDetails =
    GrsSoleTraderDetails(GrsFullname(testFirstName, testLastName),
                         testDob,
                         Some(testNino),
                         Some(testSatur),
                         None,
                         identifiersMatch = true,
                         businessVerification =
                           Some(GrsBusinessVerification(testBusinessVerificationPassStatus)),
                         registration = grsRegistrationDetails
    )

  protected val partnershipBusinessDetails: PartnershipBusinessDetails =
    PartnershipBusinessDetails(testSatur, testPostcode, None, Some(registrationDetails))

  protected val partnershipBusinessDetailsWithRegisteredNotCalled: PartnershipBusinessDetails =
    PartnershipBusinessDetails(testSatur, testPostcode, None, Some(unregisteredRegistrationDetails))

  protected val companyProfile: CompanyProfile =
    CompanyProfile(companyName = testCompanyName,
                   companyNumber = testCompanyNumber,
                   companyAddress = testCompanyAddress
    )

  protected val generalPartnershipDetails: PartnershipDetails =
    PartnershipDetails(partnershipType = GENERAL_PARTNERSHIP,
                       partnershipName = Some("General Partnership"),
                       partnershipBusinessDetails =
                         Some(
                           PartnershipBusinessDetails(testSatur,
                                                      testPostcode,
                                                      None,
                                                      Some(registrationDetails)
                           )
                         )
    )

  protected val scottishPartnershipDetails: PartnershipDetails =
    PartnershipDetails(partnershipType = SCOTTISH_PARTNERSHIP,
                       partnershipName = Some("Scottish Partnership"),
                       partnershipBusinessDetails =
                         Some(
                           PartnershipBusinessDetails(testSatur,
                                                      testPostcode,
                                                      None,
                                                      Some(registrationDetails)
                           )
                         )
    )

  protected val llpPartnershipDetails: PartnershipDetails =
    PartnershipDetails(partnershipType = LIMITED_LIABILITY_PARTNERSHIP,
                       partnershipName = None,
                       partnershipBusinessDetails =
                         Some(
                           PartnershipBusinessDetails(testSatur,
                                                      testPostcode,
                                                      None,
                                                      Some(registrationDetails)
                           )
                         )
    )

  protected val limitedPartnershipDetails: PartnershipDetails =
    PartnershipDetails(partnershipType = LIMITED_PARTNERSHIP,
                       partnershipName = None,
                       partnershipBusinessDetails =
                         Some(
                           PartnershipBusinessDetails(testSatur,
                                                      testPostcode,
                                                      None,
                                                      Some(registrationDetails)
                           )
                         )
    )

  protected val scottishLimitedPartnershipDetails: PartnershipDetails =
    PartnershipDetails(partnershipType = SCOTTISH_LIMITED_PARTNERSHIP,
                       partnershipName = None,
                       partnershipBusinessDetails =
                         Some(
                           PartnershipBusinessDetails(testSatur,
                                                      testPostcode,
                                                      None,
                                                      Some(registrationDetails)
                           )
                         )
    )

  protected def partnershipDetailsWithBusinessAddress(
    partnershipTypeEnum: PartnershipTypeEnum
  ): PartnershipDetails =
    PartnershipDetails(partnershipType = partnershipTypeEnum,
                       partnershipBusinessDetails =
                         Some(
                           PartnershipBusinessDetails(testSatur,
                                                      testPostcode,
                                                      companyProfile = Some(companyProfile),
                                                      Some(registrationDetails)
                           )
                         )
    )

  protected val subscriptionStatus: SubscriptionStatusResponse = SubscriptionStatusResponse(
    status = SubscriptionStatus.NOT_SUBSCRIBED,
    pptReference = Some("XXPPTP123456789")
  )

  protected val nrsSubmissionId = "nrs-id-999"

  protected val subscriptionCreateOrUpdate: SubscriptionCreateOrUpdateResponseSuccess =
    SubscriptionCreateOrUpdateResponseSuccess(pptReference = "XXPPTP123456789",
                                              processingDate =
                                                ZonedDateTime.now(ZoneOffset.UTC),
                                              formBundleNumber = "123456789",
                                              nrsNotifiedSuccessfully = true,
                                              nrsSubmissionId = Some(nrsSubmissionId),
                                              nrsFailureReason = None,
                                              enrolmentInitiatedSuccessfully = Some(true)
    )

  protected val subscriptionCreateWithEnrolmentFailure: SubscriptionCreateOrUpdateResponseSuccess =
    SubscriptionCreateOrUpdateResponseSuccess(pptReference = "XXPPTP123456789",
                                              processingDate =
                                                ZonedDateTime.now(ZoneOffset.UTC),
                                              formBundleNumber = "123456789",
                                              nrsNotifiedSuccessfully = true,
                                              nrsSubmissionId = Some(nrsSubmissionId),
                                              nrsFailureReason = None,
                                              enrolmentInitiatedSuccessfully = Some(false)
    )

  protected val emailVerification: VerificationStatus = VerificationStatus(
    Seq(EmailStatus(emailAddress = "test@hmrc.com", verified = true, locked = false))
  )

  protected def registeredUkCompanyOrgDetails(): OrganisationDetails =
    OrganisationDetails(organisationType = Some(UK_COMPANY),
                        businessRegisteredAddress = Some(testBusinessAddress),
                        incorporationDetails = Some(incorporationDetails)
    )

  protected def registeredRegisteredSocietyOrgDetails(): OrganisationDetails =
    OrganisationDetails(organisationType = Some(REGISTERED_SOCIETY),
                        businessRegisteredAddress = Some(testBusinessAddress),
                        incorporationDetails = Some(incorporationDetails)
    )

  protected def registeredSoleTraderOrgDetails(): OrganisationDetails =
    OrganisationDetails(organisationType = Some(SOLE_TRADER),
                        businessRegisteredAddress = Some(testBusinessAddress),
                        soleTraderDetails = Some(soleTraderDetails)
    )

  protected def registeredGeneralPartnershipOrgDetails(): OrganisationDetails =
    OrganisationDetails(organisationType = Some(PARTNERSHIP),
                        businessRegisteredAddress = Some(testBusinessAddress),
                        partnershipDetails = Some(generalPartnershipDetails)
    )

  protected def registeredScottishPartnershipOrgDetails(): OrganisationDetails =
    OrganisationDetails(organisationType = Some(PARTNERSHIP),
                        businessRegisteredAddress = Some(testBusinessAddress),
                        partnershipDetails = Some(scottishPartnershipDetails)
    )

  protected def registeredLimitedLiabilityhPartnershipOrgDetails(): OrganisationDetails =
    OrganisationDetails(organisationType = Some(PARTNERSHIP),
                        businessRegisteredAddress = Some(testBusinessAddress),
                        partnershipDetails =
                          Some(partnershipDetailsWithBusinessAddress(LIMITED_LIABILITY_PARTNERSHIP))
    )

  protected def unregisteredUkCompanyOrgDetails(): OrganisationDetails =
    OrganisationDetails(organisationType = Some(UK_COMPANY),
                        businessRegisteredAddress = Some(testBusinessAddress),
                        incorporationDetails = Some(unregisteredIncorporationDetails)
    )

  protected def unregisteredSoleTraderOrgDetails(): OrganisationDetails =
    OrganisationDetails(organisationType = Some(SOLE_TRADER),
                        businessRegisteredAddress = Some(testBusinessAddress),
                        soleTraderDetails = Some(unregisteredSoleTraderDetails)
    )

  protected def unregisteredPartnershipDetails(
    partnershipType: PartnershipTypeEnum,
    partnershipName: Option[String] = None
  ): OrganisationDetails =
    OrganisationDetails(organisationType = Some(PARTNERSHIP),
                        partnershipDetails = Some(
                          PartnershipDetails(partnershipType = partnershipType,
                                             partnershipName = partnershipName,
                                             partnershipBusinessDetails = None
                          )
                        )
    )

  protected def verificationFailedUkCompanyOrgDetails(): OrganisationDetails =
    OrganisationDetails(organisationType = Some(UK_COMPANY),
                        businessRegisteredAddress = Some(testBusinessAddress),
                        incorporationDetails = Some(verificationFailedIncorporationDetails)
    )

  protected val userEnrolmentDetails = UserEnrolmentDetails(
    pptReference = Some(PptReference("XAPPT000123456")),
    isUkAddress = Some(IsUkAddress(Some(true))),
    postcode = Some(Postcode("AB1 2BC")),
    registrationDate =
      Some(RegistrationDate(DateData("1", "2", "2021")))
  )

  protected val groupMember = GroupMember(customerIdentification1 = testCompanyNumber,
                                          customerIdentification2 = Some("id2"),
                                          organisationDetails =
                                            Some(
                                              GroupOrgDetails(UK_COMPANY.toString,
                                                              "Company Name",
                                                              Some(safeNumber)
                                              )
                                            ),
                                          contactDetails = Some(
                                            GroupMemberContactDetails(phoneNumber = Some("077123"),
                                                                      firstName = "Test",
                                                                      lastName = "User",
                                                                      email = Some("test@test.com")
                                            )
                                          ),
                                          addressDetails = Address(addressLine1 = "line1",
                                                                   addressLine2 = Some("lin2"),
                                                                   addressLine3 = Some("line3"),
                                                                   townOrCity = "line4",
                                                                   postCode = Some("AB12CD"),
                                                                   countryCode = "GB"
                                          )
  )

  protected val groupDetails =
    GroupDetail(membersUnderGroupControl = Some(true), members = Seq.empty)

  protected val addressDetails = Address(addressLine1 = "Street1",
                                         addressLine2 = Some("Street2"),
                                         addressLine3 = Some("Street3"),
                                         townOrCity = "Street4",
                                         postCode = Some("T5 6TA"),
                                         countryCode = "GB"
  )

  protected def groupMemberForOrganisationType(organisationType: OrgType) =
    groupMember.copy(organisationDetails =
      Some(GroupOrgDetails(organisationType.toString, "Company Name", Some(safeNumber)))
    )

}
