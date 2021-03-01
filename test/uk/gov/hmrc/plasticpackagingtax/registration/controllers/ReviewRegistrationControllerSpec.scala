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
import controllers.Assets.{OK, SEE_OTHER}
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.BDDMockito.`given`
import org.mockito.Mockito.{reset, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.libs.json.JsObject
import play.api.test.Helpers.status
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.IncorpIdConnector
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.IncorporationDetails
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.review_registration_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class ReviewRegistrationControllerSpec extends ControllerSpec {
  private val page                  = mock[review_registration_page]
  private val mcc                   = stubMessagesControllerComponents()
  private val mockIncorpIdConnector = mock[IncorpIdConnector]

  private val incorporationDetails =
    IncorporationDetails("123456789", "Example Limited", "0123456789")

  private val controller =
    new ReviewRegistrationController(authenticate = mockAuthAction,
                                     mockJourneyAction,
                                     mcc = mcc,
                                     incorpIdConnector = mockIncorpIdConnector,
                                     page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    val registration = aRegistration()

    mockRegistrationFind(registration)
    given(page.apply(refEq(registration), refEq(incorporationDetails))(any(), any())).willReturn(
      HtmlFormat.empty
    )
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Review registration controller" should {

    "return 200" when {

      "user is authorised and display page method is invoked" in {
        authorizedUser()
        when(mockIncorpIdConnector.getDetails(any())(any()))
          .thenReturn(Future(incorporationDetails))
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }
    }

    "return 303" when {

      "Journey ID is invalid or does not exist" in {
        authorizedUser()

        val registration = aRegistration(withIncorpJourneyId(None))
        mockRegistrationFind(registration)
        given(
          page.apply(refEq(registration), refEq(incorporationDetails))(any(), any())
        ).willReturn(HtmlFormat.empty)

        val result = controller.displayPage()(getRequest())

        status(result) mustBe SEE_OTHER
      }

      "when form is submitted" in {
        authorizedUser()

        val result = controller.submit()(postRequest(JsObject.empty))

        status(result) mustBe SEE_OTHER
      }
    }

    "return an error" when {

      "user is not authorised" in {
        unAuthorizedUser()
        val result = controller.displayPage()(getRequest())

        intercept[RuntimeException](status(result))
      }
    }
  }

}
