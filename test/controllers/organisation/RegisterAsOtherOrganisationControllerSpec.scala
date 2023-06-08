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

package controllers.organisation

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import views.html.organisation.register_as_other_organisation
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class RegisterAsOtherOrganisationControllerSpec extends ControllerSpec {

  private val page = mock[register_as_other_organisation]

  val controller =
    new RegisterAsOtherOrganisationController(stubMessagesControllerComponents(),
                                              page,
                                              authenticate = FakeRegistrationAuthAction
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply()(any(), any())).thenReturn(HtmlFormat.empty)

  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "OrganisationTypeNotSupported controller" should {

    "return 200 (OK)" when {

      "on page load method is invoked" in {

        val result = controller.onPageLoad()(FakeRequest())

        status(result) must be(OK)
      }
    }
  }
}
