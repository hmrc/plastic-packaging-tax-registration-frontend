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
import forms.enrolment.{IsUkAddress, PptReference}
import models.registration.UserEnrolmentDetails
import play.api.test.FakeRequest
import repositories.{UserDataRepository, UserEnrolmentDetailsRepository}
import views.html.enrolment.is_uk_address_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class IsUkAddressControllerSpec extends ControllerSpec {

  private val page       = mock[is_uk_address_page]
  private val mcc        = stubMessagesControllerComponents()
  private val mockCache  = mock[UserDataRepository]
  private val repository = new UserEnrolmentDetailsRepository(mockCache)

  private val controller = new IsUkAddressController(FakeRegistrationAuthAction, mcc, repository, page)

  private val pptReference            = PptReference("XAPPT000123456")
  private val initialEnrolmentDetails = UserEnrolmentDetails(pptReference = Some(pptReference))

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[Form[IsUkAddress]])(any(), any())).thenReturn(
      HtmlFormat.raw("Is UK Address Page")
    )
    when(mockCache.getData[UserEnrolmentDetails](any())(any(), any())).thenReturn(
      Future.successful(Some(initialEnrolmentDetails))
    )
  }

  override protected def afterEach(): Unit = {
    reset(page, mockCache)
    super.afterEach()
  }

  "Is UK Address Controller" should {
    "display the is uk address page" when {
      "user is authorised" in {
        when(mockCache.getData[UserEnrolmentDetails](any())(any(), any())).thenReturn(
          Future.successful(
            Some(initialEnrolmentDetails.copy(isUkAddress = Some(IsUkAddress(Some(true)))))
          )
        )

        val result = controller.displayPage()(FakeRequest())

        status(result) mustBe OK
        contentAsString(result) mustBe "Is UK Address Page"
      }
      "user is authorised and cache is empty" in {
        when(mockCache.getData[UserEnrolmentDetails](any())(any(), any())).thenReturn(
          Future.successful(None)
        )

        val result = controller.displayPage()(FakeRequest())

        status(result) mustBe OK
        contentAsString(result) mustBe "Is UK Address Page"
      }
    }


    "redisplay the is uk address page with a BAD REQUEST status" when {
      "no selection is made" in {

        val result = controller.submit()(postRequestEncoded(IsUkAddress(None)))

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "Is UK Address Page"
      }
    }

    "redirect to the postcode page and persist is uk address selection" when {
      "is uk address is true" in {
        val expectedEnrolmentDetails =
          initialEnrolmentDetails.copy(isUkAddress = Some(IsUkAddress(Some(true))))
        when(mockCache.putData[UserEnrolmentDetails](any(), any())(any(), any())).thenReturn(
          Future.successful(expectedEnrolmentDetails)
        )


        val result = controller.submit()(postJsonRequestEncoded(("value", "yes")))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.PostcodeController.displayPage().url)

        verify(mockCache).putData(ArgumentMatchers.eq(UserEnrolmentDetailsRepository.repositoryKey),
                                  ArgumentMatchers.eq(expectedEnrolmentDetails)
        )(any(), any())
      }
    }

    "redirect to the registration date page and persist is uk address selection" when {
      "is uk address is false" in {
        val expectedEnrolmentDetails =
          initialEnrolmentDetails.copy(isUkAddress = Some(IsUkAddress(Some(false))))
        when(mockCache.putData[UserEnrolmentDetails](any(), any())(any(), any())).thenReturn(
          Future.successful(expectedEnrolmentDetails)
        )


        val result = controller.submit()(postJsonRequestEncoded(("value", "no")))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.RegistrationDateController.displayPage().url)

        verify(mockCache).putData(ArgumentMatchers.eq(UserEnrolmentDetailsRepository.repositoryKey),
                                  ArgumentMatchers.eq(expectedEnrolmentDetails)
        )(any(), any())
      }
    }
  }
}
