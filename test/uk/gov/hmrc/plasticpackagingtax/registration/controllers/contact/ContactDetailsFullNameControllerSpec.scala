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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.contact

import base.unit.ControllerSpec
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{await, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => pptRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.FullName
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.PrimaryContactDetails
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact.full_name_page
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
    when(page.apply(any[Form[FullName]], any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
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
            withPrimaryContactDetails(PrimaryContactDetails(name = Some("FirstName LastName")))
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
          mockRegistrationUpdate()

          val result =
            controller.submit()(postRequestEncoded(FullName("FirstName -'. LastName"), formAction))

          status(result) mustBe SEE_OTHER

          modifiedRegistration.primaryContactDetails.name mustBe Some("FirstName -'. LastName")

          formAction._1 match {
            case "SaveAndContinue" =>
              redirectLocation(result) mustBe Some(
                routes.ContactDetailsJobTitleController.displayPage().url
              )
            case "SaveAndComeBackLater" =>
              redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)
          }
        }
      }

      "return 400 (BAD_REQUEST) for " + formAction._1 when {
        "user does not enter name" in {

          authorizedUser()
          val result = controller.submit()(postRequestEncoded(FullName(""), formAction))

          status(result) mustBe BAD_REQUEST
        }

        "user enters a long name" in {
          authorizedUser()
          val result = controller.submit()(postRequestEncoded(FullName("abced" * 40), formAction))

          status(result) mustBe BAD_REQUEST
        }

        "user enters non-alphabetic characters" in {
          authorizedUser()
          val result =
            controller.submit()(
              postRequestEncoded(FullName("FirstNam807980234£$ LastName"), formAction)
            )

          status(result) mustBe BAD_REQUEST
        }

        "user enters non-alphabetic no digits" in {
          authorizedUser()
          val result =
            controller.submit()(postRequestEncoded(FullName("()/,& LastName"), formAction))

          status(result) mustBe BAD_REQUEST
        }

        "user enters accented characters" in {
          authorizedUser()
          val result =
            controller.submit()(postRequestEncoded(FullName("Chlöe Anne-Marie"), formAction))

          status(result) mustBe BAD_REQUEST
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
          mockRegistrationUpdateFailure()
          val result =
            controller.submit()(postRequestEncoded(FullName("FirstName LastName"), formAction))

          intercept[DownstreamServiceError](status(result))
        }

        "user submits form and a registration update runtime exception occurs" in {
          authorizedUser()
          mockRegistrationException()
          val result =
            controller.submit()(postRequestEncoded(FullName("FirstName LastName"), formAction))

          intercept[RuntimeException](status(result))
        }
      }
    }

    "display page for a group organisation" in {
      authorizedUser()
      mockRegistrationFind(
        aRegistration(
          withPrimaryContactDetails(PrimaryContactDetails(name = Some("FirstName LastName"))),
          withGroupDetail(Some(groupDetailsWithMembers))
        )
      )

      await(controller.displayPage()(getRequest()))

      val captor = ArgumentCaptor.forClass(classOf[Boolean])
      verify(page).apply(any(), any(), captor.capture())(any(), any())
      captor.getValue mustBe true
    }
  }
}
