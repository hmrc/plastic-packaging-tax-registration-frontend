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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.enrolment

import base.unit.ControllerSpec
import com.codahale.metrics.SharedMetricRegistries
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.forms.DateData
import uk.gov.hmrc.plasticpackagingtax.registration.forms.enrolment.RegistrationDate
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.UserEnrolmentDetails
import uk.gov.hmrc.plasticpackagingtax.registration.repositories.{
  UserDataRepository,
  UserEnrolmentDetailsRepository
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.enrolment.registration_date_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class RegistrationDateControllerSpec extends ControllerSpec {

  private val page       = mock[registration_date_page]
  private val mcc        = stubMessagesControllerComponents()
  private val mockCache  = mock[UserDataRepository]
  private val repository = new UserEnrolmentDetailsRepository(mockCache)

  private val controller =
    new RegistrationDateController(mockAuthAction, mcc, repository, page)

  private val registrationDate = RegistrationDate(DateData("1", "2", "2021"))
  private val enrolmentDetails = UserEnrolmentDetails(registrationDate = Some(registrationDate))

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[Form[RegistrationDate]], any())(any(), any())).thenReturn(
      HtmlFormat.raw("Registration Date Page")
    )
    when(mockCache.getData[UserEnrolmentDetails](any())(any(), any())).thenReturn(
      Future.successful(Some(enrolmentDetails))
    )
    SharedMetricRegistries.clear()
  }

  override protected def afterEach(): Unit = {
    reset(page, mockCache)
    super.afterEach()
  }

  "Registration Date Controller" should {
    "display the registration data page" when {
      "user is authorised" in {
        authorizedUser()
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
        contentAsString(result) mustBe "Registration Date Page"
      }

      "user is authorised and cache is empty" in {
        authorizedUser()
        when(mockCache.getData[UserEnrolmentDetails](any())(any(), any())).thenReturn(
          Future.successful(None)
        )
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
        contentAsString(result) mustBe "Registration Date Page"
      }
    }
    "throw a RuntimeException" when {
      "user is not authorised" in {
        unAuthorizedUser()
        val result = controller.displayPage()(getRequest())

        intercept[RuntimeException](status(result))
      }
    }

    "redisplay the registration date page with a BAD REQUEST status" when {
      "an invalid registration date is submitted" in {
        authorizedUser()
        val correctForm = Seq("date.day" -> registrationDate.value.day,
                              "date.month" -> registrationDate.value.month,
                              "date.year"  -> "1980"
        )
        val result = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "Registration Date Page"
      }
    }

    "redirect to check answers page and persist registration date" when {
      "a valid registration date is submitted" in {

        when(mockCache.putData[UserEnrolmentDetails](any(), any())(any(), any())).thenReturn(
          Future.successful(enrolmentDetails)
        )

        authorizedUser()

        val correctForm = Seq("date.day" -> registrationDate.value.day,
                              "date.month" -> registrationDate.value.month,
                              "date.year"  -> registrationDate.value.year
        )
        val result = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(routes.CheckAnswersController.displayPage().url)

        verify(mockCache).putData(ArgumentMatchers.eq(UserEnrolmentDetailsRepository.repositoryKey),
                                  ArgumentMatchers.eq(enrolmentDetails)
        )(any(), any())
      }
    }
  }
}
