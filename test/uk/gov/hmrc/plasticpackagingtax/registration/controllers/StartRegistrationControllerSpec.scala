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
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.SEE_OTHER
import play.api.test.Helpers.{redirectLocation, status}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.LiabilityWeight
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  LiabilityDetails,
  Registration
}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class StartRegistrationControllerSpec extends ControllerSpec {

  private val mcc = stubMessagesControllerComponents()

  private val emptyRegistration = Registration("123")

  private val partialRegistration = Registration(
    id = "123",
    liabilityDetails = LiabilityDetails(weight = Some(LiabilityWeight(Some(12000))))
  )

  private val controller =
    new StartRegistrationController(mockAuthAction, mockJourneyAction, mcc)

  "StartRegistrationController" should {
    "redirect to liability weight capture page" when {
      "no existing registration" in {
        authorizedUser()
        mockRegistrationFind(emptyRegistration)

        val result = controller.startRegistration()(getRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          routes.LiabilityWeightExpectedController.displayPage().url
        )
      }
    }
    "redirect to task list page" when {
      "partial registration exists" in {
        authorizedUser()
        mockRegistrationFind(partialRegistration)

        val result = controller.startRegistration()(getRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.RegistrationController.displayPage().url)
      }
    }
  }

}
