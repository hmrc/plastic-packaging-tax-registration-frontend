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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment

import base.unit.{AddressCaptureSpec, ControllerSpec, MockAmendmentJourneyAction}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact._
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.services.AddressCaptureConfig
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact._
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import uk.gov.hmrc.plasticpackagingtax.registration.utils.FakeRequestCSRFSupport._

import scala.concurrent.Future

class AmendContactDetailsControllerSpec
    extends ControllerSpec with MockAmendmentJourneyAction with AddressCaptureSpec
    with TableDrivenPropertyChecks {

  private val mcc = stubMessagesControllerComponents()

  private val amendNamePage        = mock[full_name_page]
  private val amendJobTitlePage    = mock[job_title_page]
  private val amendPhoneNumberPage = mock[phone_number_page]

  when(amendNamePage.apply(any(), any(), any())(any(), any())).thenReturn(
    HtmlFormat.raw("name amendment")
  )

  when(amendJobTitlePage.apply(any(), any(), any())(any(), any())).thenReturn(
    HtmlFormat.raw("job title amendment")
  )

  when(amendPhoneNumberPage.apply(any(), any(), any())(any(), any())).thenReturn(
    HtmlFormat.raw("phone number amendment")
  )

  private val controller =
    new AmendContactDetailsController(mockEnrolledAuthAction,
                                      mcc,
                                      mockAmendmentJourneyAction,
                                      amendNamePage,
                                      amendJobTitlePage,
                                      amendPhoneNumberPage,
                                      mockAddressCaptureService
    )

  private val populatedRegistration = aRegistration()

  override protected def beforeEach(): Unit = {
    inMemoryRegistrationAmendmentRepository.reset()
    reset(mockSubscriptionConnector)
    simulateGetSubscriptionSuccess(populatedRegistration)
    simulateUpdateSubscriptionSuccess()
  }

  "Amend Contact Details Controller" should {

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
              ("phone number",
               (req: Request[AnyContent]) => controller.phoneNumber()(req),
               "phone number amendment"
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
        ("phone number",
         () => PhoneNumber("xxx"),
         () => PhoneNumber("07123 123456"),
         (req: Request[AnyContent]) => controller.updatePhoneNumber()(req),
         (reg: Registration) => reg.primaryContactDetails.phoneNumber mustBe Some("07123 123456"),
         "phone number amendment"
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
            inMemoryRegistrationAmendmentRepository.put("123", registration)

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
            inMemoryRegistrationAmendmentRepository.put("123", registration)

            await(call(postRequestEncoded(form = createValidForm(), sessionId = "123")))

            val registrationCaptor: ArgumentCaptor[Registration] =
              ArgumentCaptor.forClass(classOf[Registration])
            verify(mockSubscriptionConnector).updateSubscription(any(),
                                                                 registrationCaptor.capture()
            )(any())

            test(registrationCaptor.getValue)
          }
      }
    }

    "redirect to address capture when updating address" in {
      val expectedAddressCaptureConfig =
        AddressCaptureConfig(backLink = routes.AmendRegistrationController.displayPage().url,
                             successLink = routes.AmendContactDetailsController.updateAddress().url,
                             alfHeadingsPrefix = "addressLookup.contact",
                             pptHeadingKey = "addressCapture.contact.heading",
                             entityName = populatedRegistration.organisationDetails.businessName,
                             pptHintKey = None
        )
      simulateSuccessfulAddressCaptureInit(Some(expectedAddressCaptureConfig))

      val resp = controller.address()(getRequest())

      redirectLocation(resp) mustBe Some(addressCaptureRedirect.url)
    }

    "update address on address capture callback" in {
      simulateValidAddressCapture()

      val resp = controller.updateAddress()(getRequest())

      redirectLocation(resp) mustBe Some(routes.AmendRegistrationController.displayPage().url)

      val registrationCaptor: ArgumentCaptor[Registration] =
        ArgumentCaptor.forClass(classOf[Registration])
      verify(mockSubscriptionConnector).updateSubscription(any(), registrationCaptor.capture())(
        any()
      )
      registrationCaptor.getValue.primaryContactDetails.address mustBe Some(validCapturedAddress)

    }
  }

  private def getRequest(): Request[AnyContentAsEmpty.type] =
    FakeRequest("GET", "").withSession((AmendmentJourneyAction.SessionId, "123")).withCSRFToken

}
