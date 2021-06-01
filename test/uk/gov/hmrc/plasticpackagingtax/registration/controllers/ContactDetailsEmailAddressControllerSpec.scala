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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers

import base.unit.ControllerSpec
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers.{await, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.forms.EmailAddress
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.PrimaryContactDetails
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.email_address_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class ContactDetailsEmailAddressControllerSpec extends ControllerSpec with DefaultAwaitTimeout {

  private val page = mock[email_address_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new ContactDetailsEmailAddressController(authenticate = mockAuthAction,
                                             journeyAction = mockJourneyAction,
                                             emailVerificationConnector =
                                               mockEmailVerificationConnector,
                                             registrationConnector = mockRegistrationConnector,
                                             mcc = mcc,
                                             page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "ContactDetailsEmailAddressController" should {

    "return 200" when {

      "user is authorised and display page method is invoked" in {
        authorizedUser()
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

      "user is authorised, a registration already exists and display page method is invoked" in {
        authorizedUser()
        mockRegistrationFind(aRegistration())
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }
    }

    forAll(Seq(saveAndContinueFormAction, saveAndComeBackLaterFormAction)) { formAction =>
      "return 303 (OK) for " + formAction._1 when {
        "user submits an email address" in {
          authorizedUser()
          mockRegistrationFind(aRegistration())
          mockRegistrationUpdate(aRegistration())

          val result =
            controller.submit()(postRequestEncoded(EmailAddress("test@test.com"), formAction))

          status(result) mustBe SEE_OTHER
          modifiedRegistration.primaryContactDetails.email mustBe Some("test@test.com")
          formAction._1 match {
            case "SaveAndContinue" =>
              redirectLocation(result) mustBe Some(
                routes.ContactDetailsTelephoneNumberController.displayPage().url
              )
            case "SaveAndComeBackLater" =>
              redirectLocation(result) mustBe Some(routes.RegistrationController.displayPage().url)
          }
          reset(mockRegistrationConnector)
        }
      }
    }

    "return prepopulated form" when {

      def pageForm: Form[EmailAddress] = {
        val captor = ArgumentCaptor.forClass(classOf[Form[EmailAddress]])
        verify(page).apply(captor.capture())(any(), any())
        captor.getValue
      }

      "data exist" in {
        authorizedUser()
        mockRegistrationFind(
          aRegistration(
            withPrimaryContactDetails(PrimaryContactDetails(email = Some("test@test.com")))
          )
        )

        await(controller.displayPage()(getRequest()))

        pageForm.get.value mustBe "test@test.com"
      }
    }

    "return 400 (BAD_REQUEST)" when {

      "user submits invalid email address" in {
        authorizedUser()
        val result =
          controller.submit()(postRequest(Json.toJson(EmailAddress("test@"))))

        status(result) mustBe BAD_REQUEST
      }
    }

    "return an error" when {

      "user is not authorised" in {
        unAuthorizedUser()
        val result = controller.displayPage()(getRequest())

        intercept[RuntimeException](status(result))
      }

      "user submits form and the registration update fails" in {
        authorizedUser()
        mockRegistrationFailure()
        val result =
          controller.submit()(postRequest(Json.toJson(EmailAddress("test@test.com"))))

        intercept[DownstreamServiceError](status(result))
      }

      "user submits form and a registration update runtime exception occurs" in {
        authorizedUser()
        mockRegistrationException()
        val result =
          controller.submit()(postRequest(Json.toJson(EmailAddress("test@test.com"))))

        intercept[RuntimeException](status(result))
      }
    }
  }
}
