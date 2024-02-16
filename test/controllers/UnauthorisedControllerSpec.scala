/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.Helpers._
import play.twirl.api.Html
import controllers.unauthorised.UnauthorisedController
import play.api.test.FakeRequest
import views.html.unauthorised._
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class UnauthorisedControllerSpec extends ControllerSpec {

  private val page                  = mock[unauthorised]
  private val wrongCredRolePage     = mock[unauthorised_not_admin]
  private val unauthorisedAgentPage = mock[unauthorised_agent]

  when(page.apply()(any(), any())).thenReturn(Html("unauthorised"))
  when(wrongCredRolePage.apply()(any(), any())).thenReturn(Html("wrong cred role"))
  when(unauthorisedAgentPage.apply()(any(), any())).thenReturn(Html("unauthorised agent"))

  val controller =
    new UnauthorisedController(stubMessagesControllerComponents(), page, wrongCredRolePage, unauthorisedAgentPage)

  ".onPageLoad" should {

    "return 200 (OK)" when {

      "onPageLoad can be invoked" in {

        val result = controller.showGenericUnauthorised()(FakeRequest())

        status(result) must be(OK)
        contentAsString(result) should fullyMatch regex "unauthorised"
      }

      "showAgentUnauthorised can be invoked" in {

        val result = controller.showAgentUnauthorised()(FakeRequest())

        status(result) must be(OK)
        contentAsString(result) should fullyMatch regex "unauthorised agent"
      }

      "showAssistantUnauthorised can be invoked" in {

        val result = controller.showAssistantUnauthorised()(FakeRequest())

        status(result) must be(OK)
        contentAsString(result) should fullyMatch regex "wrong cred role"
      }
    }
  }
}
