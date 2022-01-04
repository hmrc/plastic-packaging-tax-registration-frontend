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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.liability

import play.api.mvc.{AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.config.Features
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.LiabilityDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyRequest
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

abstract class LiabilityController(mcc: MessagesControllerComponents)
    extends FrontendController(mcc) {

  protected val deMinimisKg: Long = LiabilityDetails.minimumLiabilityWeightKg

  protected def isPreLaunch(implicit request: JourneyRequest[AnyContent]) =
    request.isFeatureFlagEnabled(Features.isPreLaunch)

  protected def isGroupRegistrationEnabled(implicit request: JourneyRequest[AnyContent]) =
    request.isFeatureFlagEnabled(Features.isGroupRegistrationEnabled)

}
