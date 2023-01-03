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

package spec

import base.MockAuthAction
import base.PptTestData.newUser
import builders.RegistrationBuilder
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import forms.contact.Address
import forms.contact.Address.UKAddress
import forms.enrolment.{DateData, IsUkAddress, Postcode, PptReference, RegistrationDate}
import forms.organisation.OrgType.{SOLE_TRADER, UK_COMPANY, _}
import forms.organisation.PartnerTypeEnum.{PartnerTypeEnum, _}
import forms.organisation.{OrgType, PartnerTypeEnum}
import models.SignedInUser
import models.emailverification.{EmailStatus, VerificationStatus}
import models.enrolment.PptEnrolment
import models.genericregistration._
import models.registration.group.{GroupMember, GroupMemberContactDetails, OrganisationDetails => GroupOrgDetails}
import models.registration.{GroupDetail, OrganisationDetails, UserEnrolmentDetails}
import models.request.{AuthenticatedRequest, JourneyRequest}
import models.subscriptions.{SubscriptionCreateOrUpdateResponseSuccess, SubscriptionStatus, SubscriptionStatusResponse}
import services.CountryService
import utils.AddressConversionUtils
import utils.FakeRequestCSRFSupport._

import java.time.{ZoneOffset, ZonedDateTime}
import scala.language.implicitConversions

trait PptTestData extends RegistrationBuilder with MockAuthAction {

  override val addressConversionUtils: AddressConversionUtils = new AddressConversionUtils(new CountryService)

  val request: Request[AnyContent] = FakeRequest().withCSRFToken

  val authenticatedRequest: AuthenticatedRequest[AnyContent] =
    new AuthenticatedRequest(FakeRequest().withCSRFToken, newUser())

  implicit val journeyRequest: JourneyRequest[AnyContent] =
    JourneyRequest(authenticatedRequest = authenticatedRequest, registration = aRegistration(), pptReference = None)

  def pptReferenceFromUsersEnrolments(user: SignedInUser): Option[String] =
    user.enrolments.getEnrolment(PptEnrolment.Identifier).flatMap(_.identifiers.headOption.map(_.value))

  val journeyRequestWithEnrolledUser: JourneyRequest[AnyContent] =
    JourneyRequest(
      authenticatedRequest =
        new AuthenticatedRequest(FakeRequest().withCSRFToken, userWithPPTEnrolment),
      registration = aRegistration(),
      pptReference = pptReferenceFromUsersEnrolments(userWithPPTEnrolment)
    )

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

  protected val testCompanyAddress = IncorporationAddressDetails(
    address_line_1 = Some("testLine1"),
    address_line_2 = Some("testLine2"),
    locality = Some("test town"),
    care_of = Some("test name"),
    po_box = Some("123"),
    postal_code = Some("AA11AA"),
    premises = Some("1"),
    country = Some("United Kingdom")
  )

  protected val testBusinessAddress =
    UKAddress(addressLine1 = "2 Scala Street", addressLine2 = Some("Soho"), addressLine3 = None, townOrCity = "London", postCode = "W1T 2HN")

  protected val testUtr = "0123456789"

  protected val testBusinessVerificationPassStatus = "PASS"

  protected val registrationDetails: RegistrationDetails =
    RegistrationDetails(
      identifiersMatch = true,
      verificationStatus = Some("PASS"),
      registrationStatus = "REGISTERED",
      registeredBusinessPartnerId = Some(safeNumber)
    )

  protected val failedRegistrationDetails: RegistrationDetails =
    RegistrationDetails(
      identifiersMatch = true,
      verificationStatus = None,
      registrationStatus = "REGISTRATION_FAILED",
      registeredBusinessPartnerId = None
    )

  protected val unregisteredRegistrationDetails: RegistrationDetails =
    RegistrationDetails(
      identifiersMatch = true,
      verificationStatus = Some("UNCHALLENGED"),
      registrationStatus = "REGISTRATION_NOT_CALLED",
      registeredBusinessPartnerId = None
    )

  protected val verificationFailedRegistrationDetails: RegistrationDetails =
    RegistrationDetails(
      identifiersMatch = true,
      verificationStatus = Some("FAIL"),
      registrationStatus = "REGISTRATION_NOT_CALLED",
      registeredBusinessPartnerId = None
    )

  protected val identifiersUnmatchedRegistrationDetails: RegistrationDetails =
    RegistrationDetails(
      identifiersMatch = false,
      verificationStatus = Some("UNCHALLENGED"),
      registrationStatus = "REGISTRATION_NOT_CALLED",
      registeredBusinessPartnerId = None
    )

  protected val incorporationDetails: IncorporationDetails =
    IncorporationDetails(testCompanyNumber, testCompanyName, Some(testUtr), testCompanyAddress, Some(registrationDetails))

  protected val grsRegistrationDetails: GrsRegistration =
    GrsRegistration(registrationStatus = "REGISTERED", registeredBusinessPartnerId = Some(safeNumber))

  protected val grsIncorporationDetails: GrsIncorporationDetails =
    GrsIncorporationDetails(
      companyProfile = GrsCompanyProfile(testCompanyNumber, testCompanyName, testCompanyAddress),
      ctutr = testUtr,
      identifiersMatch = true,
      businessVerification = Some(GrsBusinessVerification(testBusinessVerificationPassStatus)),
      registration = grsRegistrationDetails
    )

  protected val unregisteredIncorporationDetails: IncorporationDetails =
    IncorporationDetails(testCompanyNumber, testCompanyName, Some(testUtr), testCompanyAddress, Some(unregisteredRegistrationDetails))

  protected val verificationFailedIncorporationDetails: IncorporationDetails =
    IncorporationDetails(testCompanyNumber, testCompanyName, Some(testUtr), testCompanyAddress, Some(verificationFailedRegistrationDetails))

  protected val verificationFailedSoleTraderDetails: SoleTraderDetails =
    SoleTraderDetails(
      firstName = "Sole",
      lastName = "Trader",
      dateOfBirth = Some("12/12/1960"),
      ninoOrTrn = "1234",
      sautr = Some("ABC"),
      registration = Some(verificationFailedRegistrationDetails)
    )

  protected val unregisteredSoleTraderDetails: SoleTraderDetails =
    SoleTraderDetails(
      firstName = "Sole",
      lastName = "Trader",
      dateOfBirth = Some("12/12/1960"),
      ninoOrTrn = "1234",
      sautr = Some("ABC"),
      registration = Some(unregisteredRegistrationDetails)
    )

  protected val soleTraderDetails: SoleTraderDetails =
    SoleTraderDetails(testFirstName, testLastName, Some(testDob), testNino, Some(testSatur), Some(registrationDetails))

  protected val grsSoleTraderIncorporationDetails: GrsSoleTraderDetails =
    GrsSoleTraderDetails(
      GrsFullname(testFirstName, testLastName),
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
    CompanyProfile(companyName = testCompanyName, companyNumber = testCompanyNumber, companyAddress = testCompanyAddress)

  protected val generalPartnershipDetails: PartnershipDetails =
    PartnershipDetails(
      partnershipType = GENERAL_PARTNERSHIP,
      partnershipName = Some("General Partnership"),
      partnershipBusinessDetails =
        Some(PartnershipBusinessDetails(testSatur, testPostcode, None, Some(registrationDetails)))
    )

  protected val scottishPartnershipDetails: PartnershipDetails =
    PartnershipDetails(
      partnershipType = SCOTTISH_PARTNERSHIP,
      partnershipName = Some("Scottish Partnership"),
      partnershipBusinessDetails =
        Some(PartnershipBusinessDetails(testSatur, testPostcode, None, Some(registrationDetails)))
    )

  protected def nominatedPartner(
    partnerTypeEnum: PartnerTypeEnum,
    soleTraderDetails: Option[SoleTraderDetails] = None,
    incorporationDetails: Option[IncorporationDetails] = None,
    partnerPartnershipDetails: Option[PartnerPartnershipDetails] = None
  ): Partner =
    Partner(
      id = "3534345",
      partnerType = partnerTypeEnum,
      soleTraderDetails = soleTraderDetails,
      partnerPartnershipDetails = partnerPartnershipDetails,
      incorporationDetails = incorporationDetails
    )

  protected val llpPartnershipDetails: PartnershipDetails =
    PartnershipDetails(
      partnershipType = LIMITED_LIABILITY_PARTNERSHIP,
      partnershipName = None,
      partnershipBusinessDetails =
        Some(PartnershipBusinessDetails(testSatur, testPostcode, None, Some(registrationDetails)))
    )

  protected val limitedPartnershipDetails: PartnershipDetails =
    PartnershipDetails(
      partnershipType = LIMITED_PARTNERSHIP,
      partnershipName = None,
      partnershipBusinessDetails =
        Some(PartnershipBusinessDetails(testSatur, testPostcode, None, Some(registrationDetails)))
    )

  protected val scottishLimitedPartnershipDetails: PartnershipDetails =
    PartnershipDetails(
      partnershipType = SCOTTISH_LIMITED_PARTNERSHIP,
      partnershipName = None,
      partnershipBusinessDetails =
        Some(PartnershipBusinessDetails(testSatur, testPostcode, None, Some(registrationDetails)))
    )

  protected def partnershipDetailsWithBusinessAddress(partnerTypeEnum: PartnerTypeEnum): PartnershipDetails =
    PartnershipDetails(
      partnershipType = partnerTypeEnum,
      partnershipBusinessDetails =
        Some(PartnershipBusinessDetails(testSatur, testPostcode, companyProfile = Some(companyProfile), Some(registrationDetails)))
    )

  protected val subscriptionStatus: SubscriptionStatusResponse =
    SubscriptionStatusResponse(status = SubscriptionStatus.NOT_SUBSCRIBED, pptReference = Some("XXPPTP123456789"))

  protected val nrsSubmissionId = "nrs-id-999"

  protected val subscriptionCreateOrUpdate: SubscriptionCreateOrUpdateResponseSuccess =
    SubscriptionCreateOrUpdateResponseSuccess(
      pptReference = "XXPPTP123456789",
      processingDate =
        ZonedDateTime.now(ZoneOffset.UTC),
      formBundleNumber = "123456789",
      nrsNotifiedSuccessfully = true,
      nrsSubmissionId = Some(nrsSubmissionId),
      nrsFailureReason = None,
      enrolmentInitiatedSuccessfully = Some(true)
    )

  protected val subscriptionCreateWithEnrolmentFailure: SubscriptionCreateOrUpdateResponseSuccess =
    SubscriptionCreateOrUpdateResponseSuccess(
      pptReference = "XXPPTP123456789",
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
    OrganisationDetails(
      organisationType = Some(OrgType.UK_COMPANY),
      businessRegisteredAddress = Some(testBusinessAddress),
      incorporationDetails = Some(incorporationDetails)
    )

  protected def registeredRegisteredSocietyOrgDetails(): OrganisationDetails =
    OrganisationDetails(
      organisationType = Some(OrgType.REGISTERED_SOCIETY),
      businessRegisteredAddress = Some(testBusinessAddress),
      incorporationDetails = Some(incorporationDetails)
    )

  protected def registeredSoleTraderOrgDetails(): OrganisationDetails =
    OrganisationDetails(
      organisationType = Some(OrgType.SOLE_TRADER),
      businessRegisteredAddress = Some(testBusinessAddress),
      soleTraderDetails = Some(soleTraderDetails)
    )

  protected def registeredGeneralPartnershipOrgDetails(): OrganisationDetails =
    OrganisationDetails(
      organisationType = Some(OrgType.PARTNERSHIP),
      businessRegisteredAddress = Some(testBusinessAddress),
      partnershipDetails = Some(generalPartnershipDetails)
    )

  protected def registeredScottishPartnershipOrgDetails(): OrganisationDetails =
    OrganisationDetails(
      organisationType = Some(OrgType.PARTNERSHIP),
      businessRegisteredAddress = Some(testBusinessAddress),
      partnershipDetails = Some(scottishPartnershipDetails)
    )

  protected def registeredLimitedLiabilityhPartnershipOrgDetails(): OrganisationDetails =
    OrganisationDetails(
      organisationType = Some(OrgType.PARTNERSHIP),
      businessRegisteredAddress = Some(testBusinessAddress),
      partnershipDetails =
        Some(partnershipDetailsWithBusinessAddress(LIMITED_LIABILITY_PARTNERSHIP))
    )

  protected def unregisteredUkCompanyOrgDetails(): OrganisationDetails =
    OrganisationDetails(
      organisationType = Some(UK_COMPANY),
      businessRegisteredAddress = Some(testBusinessAddress),
      incorporationDetails = Some(unregisteredIncorporationDetails)
    )

  protected def unregisteredSoleTraderOrgDetails(): OrganisationDetails =
    OrganisationDetails(
      organisationType = Some(SOLE_TRADER),
      businessRegisteredAddress = Some(testBusinessAddress),
      soleTraderDetails = Some(unregisteredSoleTraderDetails)
    )

  protected def unregisteredPartnershipDetails(partnershipType: PartnerTypeEnum, partnershipName: Option[String] = None): OrganisationDetails =
    OrganisationDetails(
      organisationType = Some(PARTNERSHIP),
      partnershipDetails =
        Some(PartnershipDetails(partnershipType = partnershipType, partnershipName = partnershipName, partnershipBusinessDetails = None))
    )

  protected def verificationFailedUkCompanyOrgDetails(): OrganisationDetails =
    OrganisationDetails(
      organisationType = Some(UK_COMPANY),
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

  protected val groupMember = aGroupMember()

  protected def aGroupMember() =
    GroupMember(
      id = "123456ABC",
      customerIdentification1 = testCompanyNumber,
      customerIdentification2 = Some("id2"),
      organisationDetails =
        Some(GroupOrgDetails(OrgType.UK_COMPANY.toString, "Company Name", Some(safeNumber))),
      contactDetails = Some(
        GroupMemberContactDetails(
          phoneNumber = Some("077123"),
          firstName = "Test",
          lastName = "User",
          email = Some("test@test.com"),
          address = Some(
            UKAddress(
              addressLine1 = "ContactAddressLine1",
              addressLine2 = Some("ContactAddressLine2"),
              addressLine3 = Some("ContactAddressLine3"),
              townOrCity = "ContactAddressLine4",
              postCode = "EF34GH"
            )
          )
        )
      ),
      addressDetails =
        UKAddress(
          addressLine1 = "BusinessAddressLine1",
          addressLine2 = Some("BusinessAddressLine2"),
          addressLine3 = Some("BusinessAddressLine3"),
          townOrCity = "BusinessAddressLine4",
          postCode = "AB12CD"
        )
    )

  protected val groupDetails =
    GroupDetail(membersUnderGroupControl = Some(true), members = Seq.empty)

  protected val groupDetailsWithMembers =
    GroupDetail(membersUnderGroupControl = Some(true), members = Seq(aGroupMember(), aGroupMember()))

  protected val addressDetails =
    UKAddress(addressLine1 = "Street1", addressLine2 = Some("Street2"), addressLine3 = Some("Street3"), townOrCity = "Street4", postCode = "T5 6TA")

  protected def groupMemberForOrganisationType(organisationType: OrgType) =
    groupMember.copy(organisationDetails =
      Some(GroupOrgDetails(organisationType.toString, "Company Name", Some(safeNumber)))
    )

  protected val generalPartnershipDetailsWithPartners =
    PartnershipDetails(
      partnershipType = PartnerTypeEnum.GENERAL_PARTNERSHIP,
      partnershipName = Some("Partners in Plastic"),
      partnershipBusinessDetails = Some(
        PartnershipBusinessDetails(
          sautr = "123ABC",
          postcode = "LS1 1AA",
          companyProfile = None,
          registration = Some(
            RegistrationDetails(
              identifiersMatch = true,
              verificationStatus = Some("Verified"),
              registrationStatus = "REGISTERED",
              registeredBusinessPartnerId =
                Some("XM123456")
            )
          )
        )
      ),
      partners =
        Seq(aSoleTraderPartner(), aLimitedCompanyPartner(), aPartnershipPartner()),
      inflightPartner = None
    )

  protected def aSoleTraderPartner() =
    Partner(
      partnerType = PartnerTypeEnum.SOLE_TRADER,
      id = "123",
      soleTraderDetails = Some(
        SoleTraderDetails(
          firstName = "Ben",
          lastName = "Benkson",
          dateOfBirth = Some("01/01/2000"),
          ninoOrTrn = "12345678ABC",
          sautr = Some("654321"),
          registration = Some(
            RegistrationDetails(
              identifiersMatch = true,
              verificationStatus = Some("Verified"),
              registrationStatus = "REGISTERED",
              registeredBusinessPartnerId = Some("XM654321")
            )
          )
        )
      ),
      contactDetails = Some(
        PartnerContactDetails(
          firstName = Some("Ben"),
          lastName = Some("Benkson"),
          emailAddress = Some("ben@benksons.com"),
          phoneNumber = Some("076542345687"),
          jobTitle = Some("CEO"),
          address = Some(
            Address(
              addressLine1 = "1 High Street",
              addressLine2 = Some("Cloverfield"),
              addressLine3 = None,
              townOrCity = "Leeds",
              maybePostcode = Some("LS1 1AA"),
              countryCode = "GB"
            )
          )
        )
      )
    )

  protected def aLimitedCompanyPartner() =
    Partner(
      partnerType = PartnerTypeEnum.UK_COMPANY,
      id = "456",
      incorporationDetails = Some(
        IncorporationDetails(
          companyNumber = "123456",
          companyName = "Plastic Packaging Ltd",
          ctutr = Some("123456ABC"),
          companyAddress = IncorporationAddressDetails(),
          registration = Some(
            RegistrationDetails(
              identifiersMatch = true,
              verificationStatus = Some("Verified"),
              registrationStatus = "REGISTERED",
              registeredBusinessPartnerId =
                Some("XM5334545")
            )
          )
        )
      ),
      contactDetails = Some(
        PartnerContactDetails(
          firstName = Some("Jon"),
          lastName = Some("Jobson"),
          emailAddress = Some("jon@pp.com"),
          phoneNumber = Some("07876235834"),
          address = Some(
            Address(
              addressLine1 = "3 Old Street",
              addressLine2 = Some("Cloverfield"),
              addressLine3 = None,
              townOrCity = "Leeds",
              maybePostcode = Some("LS1 1BB"),
              countryCode = "GB"
            )
          )
        )
      )
    )

  protected def aPartnershipPartner() =
    Partner(
      partnerType = PartnerTypeEnum.SCOTTISH_PARTNERSHIP,
      partnerPartnershipDetails = Some(
        PartnerPartnershipDetails(
          partnershipName = Some("The Plastic Partnership"),
          partnershipBusinessDetails = Some(
            PartnershipBusinessDetails(
              sautr = "234923487362",
              postcode = "LS1 1HS",
              companyProfile = None,
              registration = Some(
                RegistrationDetails(
                  identifiersMatch = true,
                  verificationStatus =
                    Some("Verified"),
                  registrationStatus =
                    "REGISTERED",
                  registeredBusinessPartnerId =
                    Some("XM234976234")
                )
              )
            )
          )
        )
      ),
      contactDetails = Some(
        PartnerContactDetails(
          firstName = Some("Clive"),
          lastName = Some("Clarkson"),
          emailAddress = Some("clive@tpp.com"),
          phoneNumber = Some("07876235342"),
          address = Some(
            Address(
              addressLine1 = "15 Big Road",
              addressLine2 = Some("Cloverfield"),
              addressLine3 = None,
              townOrCity = "Leeds",
              maybePostcode = Some("LS1 1CC"),
              countryCode = "GB"
            )
          )
        )
      )
    )

}
