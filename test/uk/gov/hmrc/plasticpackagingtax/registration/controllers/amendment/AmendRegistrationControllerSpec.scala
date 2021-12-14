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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment

import base.PptTestData.newUser
import base.unit.{ControllerSpec, MockAmendmentJourneyAction}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.inject.NewInstanceInjector.instanceOf
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsString, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact._
import uk.gov.hmrc.plasticpackagingtax.registration.models.enrolment.PptEnrolment
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.services.CountryService
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.amendment.amend_registration_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact._
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partials.amendment.amend_error_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.FakeRequestCSRFSupport.CSRFFakeRequest

import scala.concurrent.Future

class AmendRegistrationControllerSpec
    extends ControllerSpec with MockAmendmentJourneyAction with TableDrivenPropertyChecks {

  private val countryService = instanceOf[CountryService]

  private val mcc = stubMessagesControllerComponents()

  private val amendRegistrationPage = mock[amend_registration_page]
  private val amendNamePage         = mock[full_name_page]
  private val amendJobTitlePage     = mock[job_title_page]
  private val amendEmailPage        = mock[email_address_page]
  private val amendPhoneNumberPage  = mock[phone_number_page]
  private val amendAddressPage      = mock[address_page]
  private val amendErrorPage        = mock[amend_error_page]

  when(amendRegistrationPage.apply(any())(any(), any())).thenReturn(
    HtmlFormat.raw("registration amendment")
  )

  when(amendNamePage.apply(any(), any(), any())(any(), any())).thenReturn(
    HtmlFormat.raw("name amendment")
  )

  when(amendJobTitlePage.apply(any(), any(), any())(any(), any())).thenReturn(
    HtmlFormat.raw("job title amendment")
  )

  when(amendEmailPage.apply(any(), any(), any())(any(), any())).thenReturn(
    HtmlFormat.raw("email amendment")
  )

  when(amendPhoneNumberPage.apply(any(), any(), any())(any(), any())).thenReturn(
    HtmlFormat.raw("phone number amendment")
  )

  when(amendAddressPage.apply(any(), any(), any(), any())(any(), any())).thenReturn(
    HtmlFormat.raw("address amendment")
  )

  private val controller =
    new AmendRegistrationController(mockAuthAllowEnrolmentAction,
                                    mcc,
                                    mockAmendmentJourneyAction,
                                    amendRegistrationPage,
                                    amendNamePage,
                                    amendJobTitlePage,
                                    amendEmailPage,
                                    amendPhoneNumberPage,
                                    amendAddressPage,
                                    countryService,
                                    amendErrorPage
    )

  private def authorisedUserWithPptSubscription() =
    authorizedUser(user =
      newUser().copy(enrolments =
        Enrolments(
          Set(
            new Enrolment(PptEnrolment.Identifier,
                          Seq(EnrolmentIdentifier(PptEnrolment.Key, "XMPPT0000000123")),
                          "activated"
            )
          )
        )
      )
    )

  private val populatedRegistration = aRegistration()

  override protected def beforeEach(): Unit = {
    mockRegistrationAmendmentRepository.reset()
    simulateGetSubscriptionSuccess(populatedRegistration)
  }

  "Amend Registration Controller" should {

    "display the amend registration page" when {

      "user is authenticated" in {
        authorisedUserWithPptSubscription()

        val resp = controller.displayPage()(getRequest())

        status(resp) mustBe OK
        contentAsString(resp) mustBe "registration amendment"
      }
    }

    "throw exception" when {
      "unauthenticated user attempts to access the amendment page" in {
        unAuthorizedUser()

        val resp = controller.displayPage()(getRequest())

        intercept[RuntimeException] {
          status(resp)
        }
      }

      "user is authenticated but attempt to retrieve registration via connector fails" in {
        authorisedUserWithPptSubscription()
        simulateGetSubscriptionFailure()

        val resp = controller.displayPage()(getRequest())

        intercept[RuntimeException] {
          status(resp)
        }
      }
    }

    "show page" when {
      val showPageTestData =
        Table(("Test Name", "Display Call", "Expected Page Content"),
              ("main contact name",
               (req: Request[AnyContent]) => controller.contactName()(req),
               "name amendment"
              ),
              ("job title",
               (req: Request[AnyContent]) => controller.jobTitle()(req),
               "job title amendment"
              ),
              ("email", (req: Request[AnyContent]) => controller.email()(req), "email amendment"),
              ("phone number",
               (req: Request[AnyContent]) => controller.phoneNumber()(req),
               "phone number amendment"
              ),
              ("address",
               (req: Request[AnyContent]) => controller.address()(req),
               "address amendment"
              )
        )

      forAll(showPageTestData) {
        (
          testName: String,
          call: (Request[AnyContent]) => Future[Result],
          expectedContent: String
        ) =>
          s"$testName page requested and registration populated" in {
            authorisedUserWithPptSubscription()

            val resp = call(getRequest())

            status(resp) mustBe OK
            contentAsString(resp) mustBe expectedContent
          }

          s"$testName page requested and registration unpopulated" in {
            simulateGetSubscriptionSuccess(Registration("123"))
            authorisedUserWithPptSubscription()

            val resp = call(getRequest())

            status(resp) mustBe OK
            contentAsString(resp) mustBe expectedContent
          }
      }
    }

    val invalidAddress = Address(addressLine1 = "", townOrCity = "", postCode = Some("XXX"))
    val validAddress = Address(addressLine1 = "2-3 Scala Street",
                               addressLine2 = Some("Soho"),
                               townOrCity = "London",
                               postCode = Some("E17 3RE")
    )
    val updateTestData =
      Table(
        ("TestName",
         "Invalid Form",
         "Valid Form",
         "Update Call",
         "Update Test",
         "Invalid Expected Page Content"
        ),
        ("main contact name",
         () => FullName(""),
         () => FullName("John Johnson"),
         (req: Request[AnyContent]) => controller.updateContactName()(req),
         (reg: Registration) => reg.primaryContactDetails.name mustBe Some("John Johnson"),
         "name amendment"
        ),
        ("job title",
         () => JobTitle(""),
         () => JobTitle("CEO"),
         (req: Request[AnyContent]) => controller.updateJobTitle()(req),
         (reg: Registration) => reg.primaryContactDetails.jobTitle mustBe Some("CEO"),
         "job title amendment"
        ),
        ("email",
         () => EmailAddress("xxx"),
         () => EmailAddress("john@ppt.com"),
         (req: Request[AnyContent]) => controller.updateEmail()(req),
         (reg: Registration) => reg.primaryContactDetails.email mustBe Some("john@ppt.com"),
         "email amendment"
        ),
        ("phone number",
         () => PhoneNumber("xxx"),
         () => PhoneNumber("07123 123456"),
         (req: Request[AnyContent]) => controller.updatePhoneNumber()(req),
         (reg: Registration) => reg.primaryContactDetails.phoneNumber mustBe Some("07123 123456"),
         "phone number amendment"
        ),
        ("address",
         () => invalidAddress,
         () => validAddress,
         (req: Request[AnyContent]) => controller.updateAddress()(req),
         (reg: Registration) => reg.primaryContactDetails.address mustBe Some(validAddress),
         "address amendment"
        )
      )

    "redisplay page" when {
      forAll(updateTestData) {
        (
          testName: String,
          createInvalidForm: () => AnyRef,
          _,
          call: Request[AnyContent] => Future[Result],
          _,
          expectedPageContent
        ) =>
          s"supplied $testName fails validation" in {
            val registration = aRegistration()
            authorisedUserWithPptSubscription()
            mockRegistrationAmendmentRepository.put("123", registration)

            val resp = call(postRequestEncoded(form = createInvalidForm(), sessionId = "123"))

            status(resp) mustBe BAD_REQUEST
            contentAsString(resp) mustBe expectedPageContent
          }
      }
    }

    "update registration and redirect to registration amendment page" when {
      forAll(updateTestData) {
        (
          testName: String,
          _,
          createValidForm: () => AnyRef,
          call: Request[AnyContent] => Future[Result],
          test: Registration => scalatest.Assertion,
          _
        ) =>
          s"$testName updated" in {
            val registration = aRegistration()
            authorisedUserWithPptSubscription()
            mockRegistrationAmendmentRepository.put("123", registration)

            val resp = await(call(postRequestEncoded(form = createValidForm(), sessionId = "123")))

            val registrationCaptor: ArgumentCaptor[Registration] =
              ArgumentCaptor.forClass(classOf[Registration])
            verify(mockSubscriptionConnector).updateSubscription(any(),
                                                                 registrationCaptor.capture()
            )(any())

            test(registrationCaptor.getValue)
          }
      }
    }
  }

  private def getRequest(): Request[AnyContentAsEmpty.type] =
    FakeRequest("GET", "").withSession((AmendmentJourneyAction.SessionId, "123")).withCSRFToken

}
