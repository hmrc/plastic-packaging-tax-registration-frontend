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
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.mvc.Headers
import uk.gov.hmrc.http.HeaderNames
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.LiabilityWeight
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.LiabilityDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyRequest

class LiabilityLinkHelperSpec extends ControllerSpec {

  private val helper =
    LiabilityLinkHelper(appConfig = config)

  "Backlink for liability weight page" should {

    "when liability weight is more than minimum weight" in {
      val headers = Headers().add(HeaderNames.xRequestId -> "req1")
      val journeyRequest =
        new JourneyRequest(
          authenticatedRequest =
            authRequest(headers, user = PptTestData.newUser("123")),
          aRegistration(
            withLiabilityDetails(LiabilityDetails(weight = Some(LiabilityWeight(Some(10001)))))
          )
        )
      when(config.minimumWeight).thenReturn(10000)
      val result = helper.backLinkForLiabilityDatePages()(journeyRequest)
      result mustBe
        routes.LiabilityWeightController.displayPage()
    }

    "when liability weight is less than minimum weight" in {
      val headers = Headers().add(HeaderNames.xRequestId -> "req1")
      val journeyRequest =
        new JourneyRequest(
          authenticatedRequest =
            authRequest(headers, user = PptTestData.newUser("123")),
          aRegistration(
            withLiabilityDetails(LiabilityDetails(weight = Some(LiabilityWeight(Some(9000)))))
          )
        )
      when(config.minimumWeight).thenReturn(10000)
      val result = helper.backLinkForLiabilityDatePages()(journeyRequest)
      result mustBe
        routes.LiabilityProcessMoreWeightController.displayPage()
    }

    "when liability weight is None" in {
      val headers = Headers().add(HeaderNames.xRequestId -> "req1")
      val journeyRequest =
        new JourneyRequest(authenticatedRequest =
                             authRequest(headers, user = PptTestData.newUser("123")),
                           aRegistration(withLiabilityDetails(LiabilityDetails(weight = None)))
        )
      val result = helper.backLinkForLiabilityDatePages()(journeyRequest)
      result mustBe
        routes.RegistrationController.displayPage()
    }

    "when liability weight has the totalKg as None" in {
      val headers = Headers().add(HeaderNames.xRequestId -> "req1")
      val journeyRequest =
        new JourneyRequest(
          authenticatedRequest =
            authRequest(headers, user = PptTestData.newUser("123")),
          aRegistration(
            withLiabilityDetails(LiabilityDetails(weight = Some(LiabilityWeight(None))))
          )
        )
      val result = helper.backLinkForLiabilityDatePages()(journeyRequest)
      result mustBe
        routes.RegistrationController.displayPage()
    }
  }

  "Date page link" should {

    "when preLaunch is enabled" in {
      when(config.isPreLaunch).thenReturn(true)
      val result = helper.datePageLink()
      result mustBe
        routes.LiabilityLiableDateController.displayPage()
    }

    "when preLaunch is disabled" in {
      when(config.isPreLaunch).thenReturn(false)
      val result = helper.datePageLink()
      result mustBe
        routes.LiabilityStartDateController.displayPage()
    }
  }
}
