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
import controllers.Assets.SEE_OTHER
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, status}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class IncorpIdControllerSpec extends ControllerSpec {

  private val fakeRequest = FakeRequest("GET", "/")
  private val mcc         = stubMessagesControllerComponents()

  private val controller =
    new IncorpIdController(authenticate = mockAuthAction, mcc)(config, ec)

  "incorpIdCallback" should {
    "redirect to the registration page" in {
      authorizedUser()
      val result = controller.incorpIdCallback("uuid-id")(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.RegistrationController.displayPage().url)
    }
  }
}
