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

package controllers.enrolment

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import forms.enrolment.{IsUkAddress, Postcode, PptReference}
import models.registration.UserEnrolmentDetails
import play.api.test.FakeRequest
import repositories.{UserDataRepository, UserEnrolmentDetailsRepository}
import views.html.enrolment.postcode_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class PostcodeControllerSpec extends ControllerSpec {

  private val page       = mock[postcode_page]
  private val mcc        = stubMessagesControllerComponents()
  private val mockCache  = mock[UserDataRepository]
  private val repository = new UserEnrolmentDetailsRepository(mockCache)

  private val controller = new PostcodeController(FakeRegistrationAuthAction, mcc, repository, page)

  private val pptReference = PptReference("XAPPT000123456")
  private val isUkAddress  = IsUkAddress(Some(true))

  private val initialEnrolmentDetails =
    UserEnrolmentDetails(pptReference = Some(pptReference), isUkAddress = Some(isUkAddress))

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[Form[Postcode]])(any(), any())).thenReturn(HtmlFormat.raw("Postcode Page"))
    when(mockCache.getData[UserEnrolmentDetails](any())(any(), any())).thenReturn(
      Future.successful(Some(initialEnrolmentDetails))
    )
  }

  override protected def afterEach(): Unit = {
    reset(page, mockCache)
    super.afterEach()
  }

  "Postcode Controller" should {
    "display the postcode page" when {
      "user is authorised" in {
        when(mockCache.getData[UserEnrolmentDetails](any())(any(), any())).thenReturn(
          Future.successful(
            Some(initialEnrolmentDetails.copy(postcode = Some(Postcode("LS1 1AA"))))
          )
        )

        val result = controller.displayPage()(FakeRequest())

        status(result) mustBe OK
        contentAsString(result) mustBe "Postcode Page"
      }

      "user is authorised and cache is empty" in {

        when(mockCache.getData[UserEnrolmentDetails](any())(any(), any())).thenReturn(
          Future.successful(None)
        )
        val result = controller.displayPage()(FakeRequest())

        status(result) mustBe OK
        contentAsString(result) mustBe "Postcode Page"
      }
    }

    "redisplay the Postcode Page with a BAD REQUEST status" when {
      "an invalid postcode is submitted" in {

        val result = controller.submit()(postRequestEncoded(Postcode("XXX")))

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "Postcode Page"
      }
    }

    "redirect to next page and persist postcode" when {
      "a valid postcode is submitted" in {
        val expectedEnrolmentDetails =
          initialEnrolmentDetails.copy(postcode = Some(Postcode("LS1 1AA")))
        when(mockCache.putData[UserEnrolmentDetails](any(), any())(any(), any())).thenReturn(
          Future.successful(expectedEnrolmentDetails)
        )


        val result = controller.submit()(postRequestEncoded(Postcode("LS1 1AA")))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.RegistrationDateController.displayPage().url)

        verify(mockCache).putData(ArgumentMatchers.eq(UserEnrolmentDetailsRepository.repositoryKey),
                                  ArgumentMatchers.eq(expectedEnrolmentDetails)
        )(any(), any())
      }
    }
  }
}
