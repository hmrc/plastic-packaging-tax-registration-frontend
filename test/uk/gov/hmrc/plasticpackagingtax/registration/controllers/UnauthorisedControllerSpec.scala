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

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.{
  unauthorised,
  unauthorised_not_admin
}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class UnauthorisedControllerSpec extends ControllerSpec {

  private val page              = mock[unauthorised]
  private val wrongCredRolePage = mock[unauthorised_not_admin]

  when(page.apply()(any(), any())).thenReturn(Html("unauthorised"))
  when(wrongCredRolePage.apply()(any(), any())).thenReturn(Html("wrong cred role"))

  val controller =
    new UnauthorisedController(stubMessagesControllerComponents(), page, wrongCredRolePage)

  "Unauthorised controller" should {

    "return 200 (OK)" when {

      "display page method is invoked when cred role is User" in {

        val result = controller.onPageLoad()(getRequest())

        status(result) must be(OK)
        contentAsString(result) should fullyMatch regex "unauthorised"
      }

      "display page method is invoked when cred role is not User" in {

        val result = controller.onPageLoad(nonAdminCredRole = true)(getRequest())

        status(result) must be(OK)
        contentAsString(result) should fullyMatch regex "wrong cred role"
      }
    }
  }
}
