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
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.`given`
import org.mockito.Mockito.reset
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import views.html.enrolment.verify_organisation_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class VerifyOrganisationControllerSpec extends ControllerSpec {
  private val page = mock[verify_organisation_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new VerifyOrganisationController(authenticate = mockAuthAction, mcc = mcc, page = page)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    given(page.apply()(any(), any())).willReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Verify organisation controller" should {

    "return 200" when {

      "user is authorised and display page method is invoked" in {
        authorizedUser()

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

    "redirect to IsUkAddress page " when {

      "user is authorised and submit " in {
        val result = controller.submit()(getRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IsUkAddressController.displayPage().url)
      }

    }

  }

}
