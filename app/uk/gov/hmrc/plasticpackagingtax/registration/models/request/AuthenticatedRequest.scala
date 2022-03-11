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

import play.api.mvc.{Request, WrappedRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.models.SignedInUser
import uk.gov.hmrc.plasticpackagingtax.registration.models.enrolment.PptEnrolment

class AuthenticatedRequest[+A](
  request: Request[A],
  val user: SignedInUser,
  val appConfig: AppConfig
) extends WrappedRequest[A](request) {

  def pptReference: Option[String] =
    user.enrolments.getEnrolment(PptEnrolment.Identifier).flatMap(
      enrolment => enrolment.getIdentifier(PptEnrolment.Key)
    ).map(id => id.value)

  val featureFlags: Map[String, Boolean] =
    user.features

  def isFeatureFlagEnabled(flag: String) =
    featureFlags.getOrElse(flag, appConfig.isDefaultFeatureFlagEnabled(flag))

}
