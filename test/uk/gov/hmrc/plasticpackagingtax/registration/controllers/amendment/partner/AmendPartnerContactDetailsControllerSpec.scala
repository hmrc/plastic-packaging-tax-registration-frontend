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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.partner

import base.unit.{AddressCaptureSpec, ControllerSpec, MockAmendmentJourneyAction}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.mvc._
import play.api.test.Helpers.{await, contentAsString, redirectLocation, status}
import play.api.test.{FakeRequest, Injecting}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.{
  routes => amendmentRoutes
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact._
import uk.gov.hmrc.plasticpackagingtax.registration.forms.group.MemberName
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.EmailVerificationJourneyStatus
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  AmendRegistrationUpdateService,
  Registration
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.services.{
  AddressCaptureConfig,
  EmailVerificationService
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact.{
  email_address_passcode_confirmation_page,
  email_address_passcode_page,
  too_many_attempts_passcode_page
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partner.{
  partner_email_address_page,
  partner_job_title_page,
  partner_member_name_page,
  partner_phone_number_page
}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.FakeRequestCSRFSupport.CSRFFakeRequest

import scala.concurrent.Future

class AmendPartnerContactDetailsControllerSpec
    extends ControllerSpec with AddressCaptureSpec with MockAmendmentJourneyAction
    with TableDrivenPropertyChecks with Injecting {

  private val mcc = stubMessagesControllerComponents()

  private val inMemoryRegistrationUpdater = new AmendRegistrationUpdateService(
    inMemoryRegistrationAmendmentRepository
  )

  private val mockEmailVerificationService = mock[EmailVerificationService]

  private val mockContactNamePage             = mock[partner_member_name_page]
  private val mockContactEmailPage            = mock[partner_email_address_page]
  private val email_address_passcode_page     = mock[email_address_passcode_page]
  private val too_many_attempts_passcode_page = mock[too_many_attempts_passcode_page]
  private val emailCorrectPasscodePage        = mock[email_address_passcode_confirmation_page]
  private val mockContactPhoneNumberPage      = mock[partner_phone_number_page]
  private val mockJobTitlePage                = mock[partner_job_title_page]

  private val partnershipRegistration = aRegistration(
    withPartnershipDetails(Some(generalPartnershipDetailsWithPartners))
  )

  private val nominatedPartner             = partnershipRegistration.nominatedPartner.get
  private val nominatedPartnerFirstName    = nominatedPartner.contactDetails.get.firstName.get
  private val nominatedPartnerLastName     = nominatedPartner.contactDetails.get.lastName.get
  private val nominatedPartnerEmailAddress = nominatedPartner.contactDetails.get.emailAddress.get
  private val nominatedPartnerPhoneNumber  = nominatedPartner.contactDetails.get.phoneNumber.get
  private val nominatedPartnerJobTitle     = nominatedPartner.contactDetails.get.jobTitle.get

  private val otherPartner             = partnershipRegistration.otherPartners.head
  private val otherPartnerFirstName    = otherPartner.contactDetails.get.firstName.get
  private val otherPartnerLastName     = otherPartner.contactDetails.get.lastName.get
  private val otherPartnerEmailAddress = otherPartner.contactDetails.get.emailAddress.get
  private val otherPartnerPhoneNumber  = otherPartner.contactDetails.get.phoneNumber.get

  private val controller = new AmendPartnerContactDetailsController(
    authenticate = mockAuthAllowEnrolmentAction,
    mcc = mcc,
    amendmentJourneyAction = mockAmendmentJourneyAction,
    contactNamePage = mockContactNamePage,
    contactEmailPage = mockContactEmailPage,
    emailPasscodePage = email_address_passcode_page,
    emailCorrectPasscodePage = emailCorrectPasscodePage,
    emailIncorrectPasscodeTooManyAttemptsPage =
      too_many_attempts_passcode_page,
    registrationUpdater = inMemoryRegistrationUpdater,
    emailVerificationService = mockEmailVerificationService,
    contactPhoneNumberPage = mockContactPhoneNumberPage,
    jobTitlePage = mockJobTitlePage,
    addressCaptureService = mockAddressCaptureService
  )

  override protected def beforeEach(): Unit = {
    authorisedUserWithPptSubscription()
    simulateGetSubscriptionSuccess(partnershipRegistration)
    simulateUpdateSubscriptionSuccess()

    when(mockContactNamePage.apply(any(), any(), any(), any())(any(), any())).thenReturn(
      HtmlFormat.raw("Amend Partner Contact Name")
    )
    when(mockContactEmailPage.apply(any(), any(), any(), any())(any(), any())).thenReturn(
      HtmlFormat.raw("Amend Partner Contact Email Address")
    )
    when(mockContactPhoneNumberPage.apply(any(), any(), any(), any())(any(), any())).thenReturn(
      HtmlFormat.raw("Amend Partner Contact Phone Number")
    )
    when(mockJobTitlePage.apply(any(), any(), any(), any())(any(), any())).thenReturn(
      HtmlFormat.raw("Amend Partner Job Title")
    )
    when(
      email_address_passcode_page.apply(any(), any(), any(), any(), any())(any(), any())
    ).thenReturn(HtmlFormat.empty)
    when(emailCorrectPasscodePage.apply(any(), any(), any())(any(), any())).thenReturn(
      HtmlFormat.empty
    )
  }

  override protected def afterEach(): Unit = {
    reset(mockSubscriptionConnector, mockEmailVerificationService)
    inMemoryRegistrationAmendmentRepository.reset()
    super.afterEach()
  }

  "Amend Partner Contact Details Controller" should {

    val showPageTestData =
      Table(("Test Name", "Display Call", "Verification"),
            ("nominated partner contact name",
             (req: Request[AnyContent]) => controller.contactName(nominatedPartner.id)(req),
             () => {
               val formCaptor: ArgumentCaptor[Form[MemberName]] =
                 ArgumentCaptor.forClass(classOf[Form[MemberName]])
               verify(mockContactNamePage).apply(formCaptor.capture(),
                                                 ArgumentMatchers.eq(nominatedPartner.name),
                                                 ArgumentMatchers.eq(
                                                   amendmentRoutes.AmendRegistrationController.displayPage()
                                                 ),
                                                 ArgumentMatchers.eq(
                                                   routes.AmendPartnerContactDetailsController.updateContactName(
                                                     nominatedPartner.id
                                                   )
                                                 )
               )(any(), any())
               formCaptor.getValue.value mustBe Some(
                 MemberName(nominatedPartnerFirstName, nominatedPartnerLastName)
               )
             }
            ),
            ("other partner contact name",
             (req: Request[AnyContent]) => controller.contactName(otherPartner.id)(req),
             () => {
               val formCaptor: ArgumentCaptor[Form[MemberName]] =
                 ArgumentCaptor.forClass(classOf[Form[MemberName]])
               verify(mockContactNamePage).apply(formCaptor.capture(),
                                                 ArgumentMatchers.eq(otherPartner.name),
                                                 ArgumentMatchers.eq(
                                                   routes.PartnerContactDetailsCheckAnswersController.displayPage(
                                                     otherPartner.id
                                                   )
                                                 ),
                                                 ArgumentMatchers.eq(
                                                   routes.AmendPartnerContactDetailsController.updateContactName(
                                                     otherPartner.id
                                                   )
                                                 )
               )(any(), any())
               formCaptor.getValue.value mustBe Some(
                 MemberName(otherPartnerFirstName, otherPartnerLastName)
               )
             }
            ),
            ("nominated partner email address",
             (req: Request[AnyContent]) => controller.emailAddress(nominatedPartner.id)(req),
             () => {
               val formCaptor: ArgumentCaptor[Form[EmailAddress]] =
                 ArgumentCaptor.forClass(classOf[Form[EmailAddress]])
               verify(mockContactEmailPage).apply(formCaptor.capture(),
                                                  ArgumentMatchers.eq(
                                                    amendmentRoutes.AmendRegistrationController.displayPage()
                                                  ),
                                                  ArgumentMatchers.eq(
                                                    routes.AmendPartnerContactDetailsController.updateEmailAddress(
                                                      nominatedPartner.id
                                                    )
                                                  ),
                                                  ArgumentMatchers.eq(nominatedPartner.name)
               )(any(), any())
               formCaptor.getValue.value mustBe Some(EmailAddress(nominatedPartnerEmailAddress))
             }
            ),
            ("other partner email address",
             (req: Request[AnyContent]) => controller.emailAddress(otherPartner.id)(req),
             () => {
               val formCaptor: ArgumentCaptor[Form[EmailAddress]] =
                 ArgumentCaptor.forClass(classOf[Form[EmailAddress]])
               verify(mockContactEmailPage).apply(formCaptor.capture(),
                                                  ArgumentMatchers.eq(
                                                    routes.PartnerContactDetailsCheckAnswersController.displayPage(
                                                      otherPartner.id
                                                    )
                                                  ),
                                                  ArgumentMatchers.eq(
                                                    routes.AmendPartnerContactDetailsController.updateEmailAddress(
                                                      otherPartner.id
                                                    )
                                                  ),
                                                  ArgumentMatchers.eq(otherPartner.name)
               )(any(), any())
               formCaptor.getValue.value mustBe Some(EmailAddress(otherPartnerEmailAddress))
             }
            ),
            ("nominated partner phone number",
             (req: Request[AnyContent]) => controller.phoneNumber(nominatedPartner.id)(req),
             () => {
               val formCaptor: ArgumentCaptor[Form[PhoneNumber]] =
                 ArgumentCaptor.forClass(classOf[Form[PhoneNumber]])
               verify(mockContactPhoneNumberPage).apply(formCaptor.capture(),
                                                        ArgumentMatchers.eq(
                                                          amendmentRoutes.AmendRegistrationController.displayPage()
                                                        ),
                                                        ArgumentMatchers.eq(
                                                          routes.AmendPartnerContactDetailsController.updatePhoneNumber(
                                                            nominatedPartner.id
                                                          )
                                                        ),
                                                        ArgumentMatchers.eq(nominatedPartner.name)
               )(any(), any())
               formCaptor.getValue.value mustBe Some(PhoneNumber(nominatedPartnerPhoneNumber))
             }
            ),
            ("other partner phone number",
             (req: Request[AnyContent]) => controller.phoneNumber(otherPartner.id)(req),
             () => {
               val formCaptor: ArgumentCaptor[Form[PhoneNumber]] =
                 ArgumentCaptor.forClass(classOf[Form[PhoneNumber]])
               verify(mockContactPhoneNumberPage).apply(formCaptor.capture(),
                                                        ArgumentMatchers.eq(
                                                          routes.PartnerContactDetailsCheckAnswersController.displayPage(
                                                            otherPartner.id
                                                          )
                                                        ),
                                                        ArgumentMatchers.eq(
                                                          routes.AmendPartnerContactDetailsController.updatePhoneNumber(
                                                            otherPartner.id
                                                          )
                                                        ),
                                                        ArgumentMatchers.eq(otherPartner.name)
               )(any(), any())
               formCaptor.getValue.value mustBe Some(PhoneNumber(otherPartnerPhoneNumber))
             }
            ),
            ("nominated partner job title",
             (req: Request[AnyContent]) => controller.jobTitle(nominatedPartner.id)(req),
             () => {
               val formCaptor: ArgumentCaptor[Form[JobTitle]] =
                 ArgumentCaptor.forClass(classOf[Form[JobTitle]])
               verify(mockJobTitlePage).apply(formCaptor.capture(),
                                              ArgumentMatchers.eq(nominatedPartner.name),
                                              ArgumentMatchers.eq(
                                                amendmentRoutes.AmendRegistrationController.displayPage()
                                              ),
                                              ArgumentMatchers.eq(
                                                routes.AmendPartnerContactDetailsController.updateJobTitle(
                                                  nominatedPartner.id
                                                )
                                              )
               )(any(), any())
               formCaptor.getValue.value mustBe Some(JobTitle(nominatedPartnerJobTitle))
             }
            )
      )

    forAll(showPageTestData) {
      (testName: String, call: (Request[AnyContent]) => Future[Result], verification: () => Any) =>
        s"Show the $testName page" in {
          val resp = await(call(getRequest()))
          resp.header.status mustBe OK
          verification()
        }
    }

    "redirect to address capture with correct callback" in {
      val expectedAddressCaptureConfig = AddressCaptureConfig(
        backLink =
          routes.PartnerContactDetailsCheckAnswersController.displayPage(nominatedPartner.id).url,
        successLink =
          routes.AmendPartnerContactDetailsController.updateAddress(nominatedPartner.id).url,
        alfHeadingsPrefix = "addressLookup.partner",
        pptHeadingKey = "addressCapture.contact.heading",
        entityName = Some(nominatedPartner.name),
        pptHintKey = None
      )
      simulateSuccessfulAddressCaptureInit(Some(expectedAddressCaptureConfig))

      val resp = await(controller.address(nominatedPartner.id)(getRequest()))

      redirectLocation(Future.successful(resp)) mustBe Some(addressCaptureRedirect.url)
    }

    val updateSubscriptionTestData =
      Table(
        ("TestName",
         "Invalid Form",
         "Valid Form",
         "Update Call",
         "Update Test",
         "Redirect Location",
         "Invalid Expected Page Content"
        ),
        ("nominated partner contact name",
         () => MemberName("", ""),
         () => MemberName("John", "Johnson"),
         (req: Request[AnyContent]) => controller.updateContactName(nominatedPartner.id)(req),
         (reg: Registration) => {
           reg.nominatedPartner.get.contactDetails.get.firstName.get mustBe "John"
           reg.nominatedPartner.get.contactDetails.get.lastName.get mustBe "Johnson"
         },
         amendmentRoutes.AmendRegistrationController.displayPage(),
         "Amend Partner Contact Name"
        ),
        ("other partner contact name",
         () => MemberName("", ""),
         () => MemberName("Gerald", "Gebritte"),
         (req: Request[AnyContent]) => controller.updateContactName(otherPartner.id)(req),
         (reg: Registration) => {
           reg.otherPartners.head.contactDetails.get.firstName.get mustBe "Gerald"
           reg.otherPartners.head.contactDetails.get.lastName.get mustBe "Gebritte"
         },
         routes.PartnerContactDetailsCheckAnswersController.displayPage(otherPartner.id),
         "Amend Partner Contact Name"
        ),
        ("other partner email address",
         () => EmailAddress("xxx"),
         () => EmailAddress("updated-email@ppt.com"),
         (req: Request[AnyContent]) => controller.updateEmailAddress(otherPartner.id)(req),
         (reg: Registration) => {
           reg.otherPartners.head.contactDetails.get.emailAddress.get mustBe "updated-email@ppt.com"
         },
         routes.PartnerContactDetailsCheckAnswersController.displayPage(otherPartner.id),
         "Amend Partner Contact Email Address"
        ),
        ("nominated partner phone number",
         () => PhoneNumber("xxx"),
         () => PhoneNumber("075792743"),
         (req: Request[AnyContent]) => controller.updatePhoneNumber(nominatedPartner.id)(req),
         (reg: Registration) => {
           reg.nominatedPartner.get.contactDetails.get.phoneNumber.get mustBe "075792743"
         },
         amendmentRoutes.AmendRegistrationController.displayPage(),
         "Amend Partner Contact Phone Number"
        ),
        ("other partner phone number",
         () => PhoneNumber("xxx"),
         () => PhoneNumber("075792743"),
         (req: Request[AnyContent]) => controller.updatePhoneNumber(otherPartner.id)(req),
         (reg: Registration) => {
           reg.otherPartners.head.contactDetails.get.phoneNumber.get mustBe "075792743"
         },
         routes.PartnerContactDetailsCheckAnswersController.displayPage(otherPartner.id),
         "Amend Partner Contact Phone Number"
        )
      )

    "redisplay page" when {
      forAll(updateSubscriptionTestData) {
        (
          testName: String,
          createInvalidForm: () => AnyRef,
          _,
          call: Request[AnyContent] => Future[Result],
          _,
          _,
          expectedPageContent
        ) =>
          s"supplied $testName fails validation" in {
            val resp = call(postRequestEncoded(form = createInvalidForm(), sessionId = "123"))

            status(resp) mustBe BAD_REQUEST
            contentAsString(resp) mustBe expectedPageContent
          }
      }
    }

    "update subscription and redirect to registration amendment page" when {
      forAll(updateSubscriptionTestData) {
        (
          testName: String,
          _,
          createValidForm: () => AnyRef,
          call: Request[AnyContent] => Future[Result],
          test: Registration => scalatest.Assertion,
          redirect: Call,
          _
        ) =>
          s"$testName updated" in {
            val resp = call(postRequestEncoded(form = createValidForm(), sessionId = "123"))

            status(resp) mustBe SEE_OTHER
            redirectLocation(resp) mustBe Some(redirect.url)

            val registrationCaptor: ArgumentCaptor[Registration] =
              ArgumentCaptor.forClass(classOf[Registration])
            verify(mockSubscriptionConnector).updateSubscription(any(),
                                                                 registrationCaptor.capture()
            )(any())

            test(registrationCaptor.getValue)
          }
      }

      val updateAddressTestData =
        Table(("Partner Type", "Partner ID", "Redirect", "Updated Address Extractor"),
              ("nominated",
               nominatedPartner.id,
               amendmentRoutes.AmendRegistrationController.displayPage(),
               (registration: Registration) =>
                 registration.nominatedPartner.get.contactDetails.get.address.get
              ),
              ("other",
               otherPartner.id,
               routes.PartnerContactDetailsCheckAnswersController.displayPage(otherPartner.id),
               (registration: Registration) =>
                 registration.otherPartners.head.contactDetails.get.address.get
              )
        )

      "contact address updated" when {
        forAll(updateAddressTestData) {
          (
            partnerType: String,
            partnerId: String,
            redirect: Call,
            addressExtractor: Registration => Address
          ) =>
            s"amending $partnerType partner" in {
              simulateValidAddressCapture()

              val resp = controller.updateAddress(partnerId)(getRequest())

              redirectLocation(resp) mustBe Some(redirect.url)

              val registrationCaptor: ArgumentCaptor[Registration] =
                ArgumentCaptor.forClass(classOf[Registration])
              verify(mockSubscriptionConnector).updateSubscription(any(),
                                                                   registrationCaptor.capture()
              )(any())

              val updatedRegistration = registrationCaptor.getValue
              addressExtractor(updatedRegistration) mustBe validCapturedAddress
            }
        }
      }

      "user submits a new email for a nominated partner and is sent for email verification" in {
        authorisedUserWithPptSubscription()
        simulateGetSubscriptionSuccess(partnershipRegistration)
        // No subscription update expected.

        // Email verification will check if this address is already verified; trying to let the user through with no
        // friction if we already know this address
        when(
          mockEmailVerificationService.isEmailVerified(ArgumentMatchers.eq("new-email@ppt.com"),
                                                       any()
          )(any())
        ).thenReturn(Future.successful(false))
        // Email verification will be called to setup an email verification journey for the new email address
        when(
          mockEmailVerificationService.sendVerificationCode(
            ArgumentMatchers.eq("new-email@ppt.com"),
            any(),
            ArgumentMatchers.eq(
              routes.AmendPartnerContactDetailsController.updateEmailAddress(
                nominatedPartner.id
              ).url
            )
          )(any())
        ).thenReturn(Future.successful("an-email-verification-journey-id"))

        val resp = await(
          controller.updateEmailAddress(nominatedPartner.id)(
            postRequestEncoded(EmailAddress("new-email@ppt.com"))
          )
        )

        status(Future.successful(resp)) mustBe SEE_OTHER
        redirectLocation(Future.successful(resp)) mustBe Some(
          routes.AmendPartnerContactDetailsController.confirmEmailCode(nominatedPartner.id).url
        )

        inMemoryRegistrationAmendmentRepository.get().map { updatedRegistration =>
          updatedRegistration.get.primaryContactDetails.prospectiveEmail mustBe Some(
            "new-email@ppt.com"
          )
          updatedRegistration.get.primaryContactDetails.journeyId mustBe Some(
            "an-email-verification-journey-id"
          )
        }
      }

      "user is prompted to enter verification code for nominated partner email address" in {
        authorisedUserWithPptSubscription()
        val primaryContactDetailsWithEmailVerificationJourney =
          partnershipRegistration.primaryContactDetails.copy(
            journeyId = Some("email-verification-journey-id"),
            prospectiveEmail = Some("verified-amended-email@localhost")
          )
        simulateGetSubscriptionSuccess(
          partnershipRegistration.copy(primaryContactDetails =
            primaryContactDetailsWithEmailVerificationJourney
          )
        )
        simulateUpdateSubscriptionSuccess()

        val resp = controller.confirmEmailCode(nominatedPartner.id)(getRequest())

        status(resp) mustBe OK
      }

      "user submits correct email verification code for nominated partner email address" in {
        authorisedUserWithPptSubscription()
        val primaryContactDetailsWithEmailVerificationJourney =
          partnershipRegistration.primaryContactDetails.copy(
            journeyId = Some("email-verification-journey-id"),
            prospectiveEmail = Some("verified-amended-email@localhost")
          )
        simulateGetSubscriptionSuccess(
          partnershipRegistration.copy(primaryContactDetails =
            primaryContactDetailsWithEmailVerificationJourney
          )
        )
        simulateUpdateSubscriptionSuccess()

        // Email verification will be called to check the user submitted code
        when(
          mockEmailVerificationService.checkVerificationCode(
            ArgumentMatchers.eq("ABCDE"),
            ArgumentMatchers.eq("verified-amended-email@localhost"),
            ArgumentMatchers.eq("email-verification-journey-id")
          )(any())
        ).thenReturn(Future.successful(EmailVerificationJourneyStatus.COMPLETE))

        val resp = controller.checkEmailVerificationCode(nominatedPartner.id)(
          postRequestEncoded(EmailAddressPasscode("ABCDE"))
        )

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(
          routes.AmendPartnerContactDetailsController.emailVerified(nominatedPartner.id).url
        )
      }

      "user is prompted for confirm verified nominated partner email address" in {
        authorisedUserWithPptSubscription()
        val primaryContactDetailsWithEmailVerificationJourney =
          partnershipRegistration.primaryContactDetails.copy(
            journeyId = Some("email-verification-journey-id"),
            prospectiveEmail = Some("verified-amended-email@localhost")
          )
        simulateGetSubscriptionSuccess(
          partnershipRegistration.copy(primaryContactDetails =
            primaryContactDetailsWithEmailVerificationJourney
          )
        )
        simulateUpdateSubscriptionSuccess()

        val resp = controller.emailVerified(nominatedPartner.id)(getRequest())

        status(resp) mustBe OK
      }

      "nominated partner verified and confirmed email address is updated" in {
        authorisedUserWithPptSubscription()
        val primaryContactDetailsWithEmailVerificationJourney =
          partnershipRegistration.primaryContactDetails.copy(
            journeyId = Some("email-verification-journey-id"),
            prospectiveEmail = Some("verified-amended-email@localhost")
          )
        simulateGetSubscriptionSuccess(
          partnershipRegistration.copy(primaryContactDetails =
            primaryContactDetailsWithEmailVerificationJourney
          )
        )
        simulateUpdateSubscriptionSuccess()

        // Email verification will be called to check this email address has actually been verified
        // and that the user has not url skipped to the end of the journey
        when(mockEmailVerificationService.isEmailVerified(any(), any())(any())).thenReturn(
          Future.successful(true)
        )

        val resp = controller.confirmEmailUpdate(nominatedPartner.id)(getRequest())
        status(resp) mustBe SEE_OTHER

        val updatedRegistration = getUpdatedRegistration()
        updatedRegistration.findPartner(nominatedPartner.id).flatMap(
          _.contactDetails.flatMap(_.emailAddress)
        ) mustBe Some("verified-amended-email@localhost")
      }

      "nominated partner job title is updated" in {
        // Non table test to fill coverage hole
        authorisedUserWithPptSubscription()
        simulateGetSubscriptionSuccess(partnershipRegistration)
        simulateUpdateSubscriptionSuccess()

        val resp = controller.updateJobTitle(nominatedPartner.id)(
          postRequestEncoded(JobTitle("New job title"))
        )
        status(resp) mustBe SEE_OTHER

        getUpdatedRegistration().findPartner(nominatedPartner.id).flatMap(
          _.contactDetails.flatMap(_.jobTitle)
        ) mustBe Some("New job title")
      }
    }
  }

  private def getRequest(): Request[AnyContentAsEmpty.type] =
    FakeRequest("GET", "").withSession((AmendmentJourneyAction.SessionId, "123")).withCSRFToken

}
