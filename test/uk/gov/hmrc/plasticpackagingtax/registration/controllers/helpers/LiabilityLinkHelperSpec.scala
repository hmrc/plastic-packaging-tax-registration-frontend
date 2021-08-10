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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.helpers

import base.PptTestData
import base.unit.ControllerSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.mvc.Headers
import uk.gov.hmrc.http.HeaderNames
import uk.gov.hmrc.plasticpackagingtax.registration.config.Features
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.LiabilityWeight
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.LiabilityDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyRequest

class LiabilityLinkHelperSpec extends ControllerSpec {

  private val helper =
    LiabilityLinkHelper(appConfig = config)

  "The Liability Link Helper " should {
    "provide a suitable back link " when {
      "weight is more than minimum weight" in {
        val headers = Headers().add(HeaderNames.xRequestId -> "req1")
        val journeyRequest =
          new JourneyRequest(
            authenticatedRequest =
              authRequest(headers, user = PptTestData.newUser("123")),
            aRegistration(
              withLiabilityDetails(LiabilityDetails(weight = Some(LiabilityWeight(Some(10001)))))
            ),
            appConfig = appConfig
          )
        val result = helper.backLink()(journeyRequest)
        result mustBe
          routes.LiabilityWeightController.displayPage()
      }

      "weight is less than minimum weight" in {
        val headers = Headers().add(HeaderNames.xRequestId -> "req1")
        val journeyRequest =
          new JourneyRequest(
            authenticatedRequest =
              authRequest(headers, user = PptTestData.newUser("123")),
            aRegistration(
              withLiabilityDetails(LiabilityDetails(weight = Some(LiabilityWeight(Some(9000)))))
            ),
            appConfig = appConfig
          )
        val result = helper.backLink()(journeyRequest)
        result mustBe
          routes.LiabilityExpectToExceedThresholdWeightController.displayPage()
      }

      "weight is None" in {
        val headers = Headers().add(HeaderNames.xRequestId -> "req1")
        val journeyRequest =
          new JourneyRequest(authenticatedRequest =
                               authRequest(headers, user = PptTestData.newUser("123")),
                             aRegistration(withLiabilityDetails(LiabilityDetails(weight = None))),
                             appConfig = appConfig
          )
        val result = helper.backLink()(journeyRequest)
        result mustBe
          routes.RegistrationController.displayPage()
      }

      "weight has the totalKg as None" in {
        val headers = Headers().add(HeaderNames.xRequestId -> "req1")
        val journeyRequest =
          new JourneyRequest(
            authenticatedRequest =
              authRequest(headers, user = PptTestData.newUser("123")),
            aRegistration(
              withLiabilityDetails(LiabilityDetails(weight = Some(LiabilityWeight(None))))
            ),
            appConfig = appConfig
          )
        val result = helper.backLink()(journeyRequest)
        result mustBe
          routes.RegistrationController.displayPage()
      }
    }
  }

  "The Liability Link Helper " should {
    "provide a suitable next page link " when {
      "preLaunch is enabled" in {
        val result = helper.nextPage()(generateRequest(Map(Features.isPreLaunch -> true)))
        result mustBe
          routes.LiabilityLiableDateController.displayPage()
      }

      "preLaunch is disabled" in {
        val result = helper.nextPage()(generateRequest(Map(Features.isPreLaunch -> false)))
        result mustBe
          routes.LiabilityStartDateController.displayPage()
      }
    }
  }
}
