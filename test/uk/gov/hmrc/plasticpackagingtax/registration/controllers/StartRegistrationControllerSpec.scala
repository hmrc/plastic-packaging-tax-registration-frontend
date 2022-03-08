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
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.SEE_OTHER
import play.api.test.Helpers.{redirectLocation, status}
import uk.gov.hmrc.plasticpackagingtax.registration.config.Features
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.liability.{
  routes => liabilityRoutes
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.liability.prelaunch.{
  routes => preLaunchLiabilityRoutes
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.{Date, OldDate}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.{
  LiabilityExpectedWeight,
  LiabilityWeight
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  LiabilityDetails,
  Registration
}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import java.time.LocalDate

class StartRegistrationControllerSpec extends ControllerSpec {

  private val mcc = stubMessagesControllerComponents()

  private val emptyRegistration = Registration("123")

  private val partialRegistration = Registration(id = "123",
                                                 liabilityDetails = LiabilityDetails(
                                                   expectedWeight = Some(
                                                     LiabilityExpectedWeight(Some(true),
                                                                             Some(12000)
                                                     )
                                                   ),
                                                   exceededThresholdWeight = Some(true),
                                                   dateExceededThresholdWeight =
                                                     Some(Date(LocalDate.parse("2022-03-05"))),
                                                   expectedWeightNext12m =
                                                     Some(LiabilityWeight(Some(12000))),
                                                   startDate =
                                                     Some(OldDate(Some(1), Some(4), Some(2022)))
                                                 )
  )

  private val controller =
    new StartRegistrationController(mockAuthAction, mockJourneyAction, mcc)

  "StartRegistrationController" should {
    "redirect to first liability check page" when {
      "no existing registration" when {
        "preLaunch" in {
          authorizedUser(features = Map(Features.isPreLaunch -> true))
          verifyRedirect(
            preLaunchLiabilityRoutes.LiabilityWeightExpectedController.displayPage().url
          )
        }
        "postLaunch" in {
          authorizedUser(features = Map(Features.isPreLaunch -> false))
          verifyRedirect(liabilityRoutes.ExceededThresholdWeightController.displayPage().url)
        }

        def verifyRedirect(pageUrl: String): Unit = {
          mockRegistrationFind(emptyRegistration)

          val result = controller.startRegistration()(getRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(pageUrl)
        }
      }
    }
    "redirect to task list page" when {
      "partial registration exists" in {
        authorizedUser()
        mockRegistrationFind(partialRegistration)

        val result = controller.startRegistration()(getRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.TaskListController.displayPage().url)
      }
    }
  }

}
