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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.enrolment

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import spec.PptTestData
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.enrolment.UserEnrolmentConnector
import uk.gov.hmrc.plasticpackagingtax.registration.models.enrolment.{
  EnrolmentFailureCode,
  UserEnrolmentFailedResponse,
  UserEnrolmentSuccessResponse
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.UserEnrolmentDetails
import uk.gov.hmrc.plasticpackagingtax.registration.repositories.{
  UserDataRepository,
  UserEnrolmentDetailsRepository
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.enrolment.check_answers_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class CheckAnswersControllerSpec extends ControllerSpec with PptTestData {

  private val page                       = mock[check_answers_page]
  private val mcc                        = stubMessagesControllerComponents()
  private val mockCache                  = mock[UserDataRepository]
  private val repository                 = new UserEnrolmentDetailsRepository(mockCache)
  private val mockUserEnrolmentConnector = mock[UserEnrolmentConnector]

  private val controller =
    new CheckAnswersController(mockAuthAction, mcc, repository, mockUserEnrolmentConnector, page)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[UserEnrolmentDetails])(any(), any())).thenReturn(HtmlFormat.raw("CYA Page"))
    when(mockCache.getData[UserEnrolmentDetails](any())(any(), any())).thenReturn(
      Future.successful(Some(userEnrolmentDetails))
    )
  }

  override protected def afterEach(): Unit = {
    reset(page, mockCache)
    super.afterEach()
  }

  "Check Answers Controller" should {
    "display the check answers page" when {
      "user is authorised" in {
        when(mockCache.getData[UserEnrolmentDetails](any())(any(), any())).thenReturn(
          Future.successful(Some(userEnrolmentDetails))
        )
        authorizedUser()
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
        contentAsString(result) mustBe "CYA Page"
      }

    }
    "throw a RuntimeException" when {
      "user is not authorised" in {
        unAuthorizedUser()
        val result = controller.displayPage()(getRequest())

        intercept[RuntimeException](status(result))
      }
    }

    "redirect to next page " when {

      "answers are not complete" in {
        when(mockCache.getData[UserEnrolmentDetails](any())(any(), any())).thenReturn(
          Future.successful(Some(UserEnrolmentDetails()))
        )
        authorizedUser()
        val result = controller.displayPage()(getRequest())

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(routes.PptReferenceController.displayPage().url)
      }

      "page is submitted" when {

        "verification failed enrolment verification " in {
          authorizedUser()
          when(mockUserEnrolmentConnector.enrol(any())(any())).thenReturn(
            Future.successful(
              UserEnrolmentFailedResponse("XPPT000123456", EnrolmentFailureCode.VerificationFailed)
            )
          )
          val result =
            controller.submit()(FakeRequest("POST", ""))

          status(result) mustBe SEE_OTHER

          redirectLocation(result) mustBe Some(
            routes.NotableErrorController.enrolmentVerificationFailurePage().url
          )
        }

        "successful enrolment verification " in {
          authorizedUser()
          when(mockUserEnrolmentConnector.enrol(any())(any())).thenReturn(
            Future.successful(UserEnrolmentSuccessResponse("XPPT000123456"))
          )
          val result =
            controller.submit()(FakeRequest("POST", ""))

          status(result) mustBe SEE_OTHER

          redirectLocation(result) mustBe Some(routes.ConfirmationController.displayPage().url)
        }

      }
    }
  }
}
