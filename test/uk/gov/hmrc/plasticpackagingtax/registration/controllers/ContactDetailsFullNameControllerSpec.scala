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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.forms.FullName
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.PrimaryContactDetails
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.full_name_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class ContactDetailsFullNameControllerSpec extends ControllerSpec {
  private val page = mock[full_name_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new ContactDetailsFullNameController(authenticate = mockAuthAction,
                                         mockJourneyAction,
                                         mockRegistrationConnector,
                                         mcc = mcc,
                                         page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[Form[FullName]])(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Primary Contact Details Full Name Controller" should {

    "return 200" when {

      "user is authorised and display page method is invoked" in {
        authorizedUser()
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

      "user is authorised, a registration already exists and display page method is invoked" in {
        authorizedUser()
        mockRegistrationFind(
          aRegistration(
            withPrimaryContactDetails(
              PrimaryContactDetails(fullName = Some(FullName("FirstName", "LastName")))
            )
          )
        )
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }
    }

    forAll(Seq(saveAndContinueFormAction, saveAndComeBackLaterFormAction)) { formAction =>
      "return 303 (OK) for " + formAction._1 when {
        "user submits or saves the contact full name" in {
          authorizedUser()
          mockRegistrationFind(aRegistration())
          mockRegistrationUpdate(aRegistration())

          val result =
            controller.submit()(postRequestEncoded(FullName("FirstName", "LastName"), formAction))

          status(result) mustBe SEE_OTHER

          modifiedRegistration.primaryContactDetails.fullName mustBe Some(
            FullName("FirstName", "LastName")
          )

          formAction._1 match {
            case "SaveAndContinue" =>
              redirectLocation(result) mustBe Some(
                routes.ContactDetailsJobTitleController.displayPage().url
              )
            case "SaveAndComeBackLater" =>
              redirectLocation(result) mustBe Some(routes.RegistrationController.displayPage().url)
          }
        }
      }

      "return 400 (BAD_REQUEST) for " + formAction._1 when {
        "user does not enter" when {
          "first name" in {
            authorizedUser()
            val result =
              controller.submit()(postRequestEncoded(FullName("", "LastName"), formAction))

            status(result) mustBe BAD_REQUEST
          }

          "last name" in {
            authorizedUser()
            val result =
              controller.submit()(postRequestEncoded(FullName("FirstName", ""), formAction))

            status(result) mustBe BAD_REQUEST
          }

          "both first name and last name" in {
            authorizedUser()
            val result = controller.submit()(postRequestEncoded(FullName("", ""), formAction))

            status(result) mustBe BAD_REQUEST
          }
        }

        "user enters a long" when {
          "first name" in {
            authorizedUser()
            val result = controller.submit()(
              postRequestEncoded(FullName("averyveryveryveryverylongname", "LastName"), formAction)
            )

            status(result) mustBe BAD_REQUEST
          }

          "last name" in {
            authorizedUser()
            val result = controller.submit()(
              postRequestEncoded(FullName("FirstName", "averyveryveryveryverylongname"), formAction)
            )

            status(result) mustBe BAD_REQUEST
          }
        }

        "user enters non-alphabetic characters" when {
          "first name" in {
            authorizedUser()
            val result =
              controller.submit()(
                postRequestEncoded(FullName("FirstNam807980234£$", "LastName"), formAction)
              )

            status(result) mustBe BAD_REQUEST
          }

          "last name" in {
            authorizedUser()
            val result =
              controller.submit()(
                postRequestEncoded(FullName("FirstName", "F!!!m807980234£$"), formAction)
              )

            status(result) mustBe BAD_REQUEST
          }
        }
      }

      "return an error for " + formAction._1 when {

        "user is not authorised" in {
          unAuthorizedUser()
          val result = controller.displayPage()(getRequest())

          intercept[RuntimeException](status(result))
        }

        "user submits form and the registration update fails" in {
          authorizedUser()
          mockRegistrationFailure()
          val result =
            controller.submit()(postRequestEncoded(FullName("FirstName", "LastName"), formAction))

          intercept[DownstreamServiceError](status(result))
        }

        "user submits form and a registration update runtime exception occurs" in {
          authorizedUser()
          mockRegistrationException()
          val result =
            controller.submit()(postRequestEncoded(FullName("FirstName", "LastName"), formAction))

          intercept[RuntimeException](status(result))
        }
      }
    }
  }
}
