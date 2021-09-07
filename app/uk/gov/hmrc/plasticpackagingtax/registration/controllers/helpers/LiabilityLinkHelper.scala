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

import play.api.mvc.{AnyContent, Call}
import uk.gov.hmrc.plasticpackagingtax.registration.config.{AppConfig, Features}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyRequest

import javax.inject.{Inject, Singleton}

@Singleton
case class LiabilityLinkHelper @Inject() (appConfig: AppConfig) {

  private val deMinimis: Int = 10000

  def backLink()(implicit request: JourneyRequest[AnyContent]): Call =
    request.registration.liabilityDetails.weight match {
      case Some(weight) =>
        weight.totalKg match {
          case Some(totalKg) =>
            if (totalKg < deMinimis)
              routes.LiabilityExpectToExceedThresholdWeightController.displayPage()
            else routes.LiabilityWeightController.displayPage()
          case None => routes.RegistrationController.displayPage()
        }
      case None => routes.RegistrationController.displayPage()
    }

  def nextPage()(implicit req: JourneyRequest[AnyContent]): Call =
    if (req.isFeatureFlagEnabled(Features.isPreLaunch))
      routes.LiabilityLiableDateController.displayPage()
    else routes.LiabilityStartDateController.displayPage()

}
