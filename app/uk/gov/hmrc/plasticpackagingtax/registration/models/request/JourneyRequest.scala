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

package uk.gov.hmrc.plasticpackagingtax.registration.models.request

import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration

class JourneyRequest[+A](
  val authenticatedRequest: AuthenticatedRequest[A],
  val registration: Registration,
  appConfig: AppConfig
) extends AuthenticatedRequest[A](authenticatedRequest, authenticatedRequest.user) {

  val featureFlags: Map[String, Boolean] =
    authenticatedRequest.user.features

  def isFeatureFlagEnabled(flag: String) =
    featureFlags.getOrElse(flag, appConfig.isDefaultFeatureFlagEnabled(flag))

}
