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

import base.unit.{ControllerSpec, MockAmendmentJourneyAction}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, reset, verify, when}
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Call, Request, Result}
import play.api.test.{FakeRequest, Injecting}
import play.api.test.Helpers.{await, contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.addresslookup.AddressLookupFrontendConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.{
  routes => amendmentRoutes
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.{
  Address,
  EmailAddress,
  JobTitle,
  PhoneNumber
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.group.MemberName
import uk.gov.hmrc.plasticpackagingtax.registration.models.addresslookup.{
  AddressLookupAddress,
  AddressLookupConfigV2,
  AddressLookupConfirmation,
  AddressLookupOnRamp
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction
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
    extends ControllerSpec with MockAmendmentJourneyAction with TableDrivenPropertyChecks
    with Injecting {

  private val mcc           = stubMessagesControllerComponents()
  private val realAppConfig = inject[AppConfig]

  private val mockAddressLookupFrontendConnector = mock[AddressLookupFrontendConnector]

  private val mockContactNamePage        = mock[partner_member_name_page]
  private val mockContactEmailPage       = mock[partner_email_address_page]
  private val mockContactPhoneNumberPage = mock[partner_phone_number_page]
  private val mockJobTitlePage           = mock[partner_job_title_page]

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
    contactPhoneNumberPage = mockContactPhoneNumberPage,
    jobTitlePage = mockJobTitlePage,
    addressLookupFrontendConnector = mockAddressLookupFrontendConnector,
    appConfig = realAppConfig
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
    when(mockAddressLookupFrontendConnector.initialiseJourney(any())(any(), any())).thenReturn(
      Future(AddressLookupOnRamp("/on-ramp"))
    )
  }

  override protected def afterEach(): Unit = {
    reset(mockSubscriptionConnector)
    inMemoryRegistrationAmendmentRepository.reset()
    super.afterEach()
  }

  "Amend Partner Contract Details Controller" should {

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

    "redirect to ALF with correct callback" in {
      val resp = await(controller.address(nominatedPartner.id)(getRequest()))
      redirectLocation(Future.successful(resp)) mustBe Some("/on-ramp")

      val alfConfigCaptor: ArgumentCaptor[AddressLookupConfigV2] =
        ArgumentCaptor.forClass(classOf[AddressLookupConfigV2])
      verify(mockAddressLookupFrontendConnector).initialiseJourney(alfConfigCaptor.capture())(any(),
                                                                                              any()
      )
      alfConfigCaptor.getValue.options.continueUrl mustBe realAppConfig.selfUrl(
        routes.AmendPartnerContactDetailsController.updateAddress(nominatedPartner.id, None)
      )
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
        ("nominated partner email address",
         () => EmailAddress("xxx"),
         () => EmailAddress("updated-email@ppt.com"),
         (req: Request[AnyContent]) => controller.updateEmailAddress(nominatedPartner.id)(req),
         (reg: Registration) => {
           reg.nominatedPartner.get.contactDetails.get.emailAddress.get mustBe "updated-email@ppt.com"
         },
         amendmentRoutes.AmendRegistrationController.displayPage(),
         "Amend Partner Contact Email Address"
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
              val alfJourneyId = "123"
              val updatedAddress =
                AddressLookupAddress(lines = List("100 High Street", "Lindley", "Huddersfield"),
                                     postcode = Some("HD1 1GH"),
                                     country = None
                )
              val addressConfirmation = AddressLookupConfirmation(auditRef = "ABC123",
                                                                  id = Some(alfJourneyId),
                                                                  address = updatedAddress
              )
              when(
                mockAddressLookupFrontendConnector.getAddress(ArgumentMatchers.eq(alfJourneyId))(
                  any(),
                  any()
                )
              )
                .thenReturn(Future.successful(addressConfirmation))

              val resp = controller.updateAddress(partnerId, Some(alfJourneyId))(getRequest())
              status(resp) mustBe SEE_OTHER
              redirectLocation(resp) mustBe Some(redirect.url)

              val registrationCaptor: ArgumentCaptor[Registration] =
                ArgumentCaptor.forClass(classOf[Registration])
              verify(mockSubscriptionConnector).updateSubscription(any(),
                                                                   registrationCaptor.capture()
              )(any())

              val updatedRegistration = registrationCaptor.getValue
              addressExtractor(updatedRegistration) mustBe Address(addressConfirmation)
            }
        }
      }
    }
  }

  private def getRequest(): Request[AnyContentAsEmpty.type] =
    FakeRequest("GET", "").withSession((AmendmentJourneyAction.SessionId, "123")).withCSRFToken

}