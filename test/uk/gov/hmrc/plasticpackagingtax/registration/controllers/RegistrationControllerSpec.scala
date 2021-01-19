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

import akka.http.scaladsl.model.StatusCodes.OK
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import play.twirl.api.HtmlFormat
import spec.ControllerSpec
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.registration_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class RegistrationControllerSpec extends ControllerSpec {

  private val fakeRequest      = FakeRequest("GET", "/")
  private val mcc              = stubMessagesControllerComponents()
  private val registrationPage = mock[registration_page]

  private val controller =
    new RegistrationController(authenticate = mockAuthAction,
                               mcc = mcc,
                               registration_page = registrationPage
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(registrationPage.apply(any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(registrationPage)
    super.afterEach()
  }

  "Registration Controller" should {

    "return 200" when {

      "user is authorised and display page method is invoked" in {
        authorizedUser()
        val result = controller.displayPage()(fakeRequest)

        status(result) mustBe OK.intValue
      }
    }
  }

  "Registration Controller" should {

    "return error" when {

      "user is not authorised" in {
        unAuthorizedUser()
        val result = controller.displayPage()(fakeRequest)

        intercept[RuntimeException](status(result))
      }
    }
  }
}
