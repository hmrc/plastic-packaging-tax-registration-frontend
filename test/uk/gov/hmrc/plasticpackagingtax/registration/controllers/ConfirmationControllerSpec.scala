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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers

import base.PptTestData
import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.`given`
import org.mockito.Mockito.reset
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.OK
import play.api.test.Helpers.status
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.confirmation_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class ConfirmationControllerSpec extends ControllerSpec {
  private val page = mock[confirmation_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new ConfirmationController(authenticate = mockAuthAction, mcc = mcc, page = page)

  private val registration = aRegistration()

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    mockRegistrationFind(registration)
    given(page.apply()(any(), any(), any())).willReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Confirmation controller" should {

    "return 200" when {

      "user is authorised and display page method is invoked" in {
        authorizedUser()

        mockRegistrationUpdate()
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

      "user is already enrolled and display page method is invoked" in {
        val user =
          PptTestData.newUser().copy(enrolments = Enrolments(Set(Enrolment("HMRC-PPT-ORG"))))
        authorizedUser(user)

        mockRegistrationUpdate()
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
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
