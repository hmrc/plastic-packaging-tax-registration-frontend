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

package uk.gov.hmrc.plasticpackagingtax.registration.config

import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject() (config: Configuration, val servicesConfig: ServicesConfig) {

  lazy val assetsUrl: String = config.get[String]("assets.url")

  lazy val assetsPrefix: String = assetsUrl + config.get[String]("assets.version")

  lazy val serviceIdentifier = "plastic-packaging-tax"

  lazy val contactBaseUrl = servicesConfig.baseUrl("contact-frontend")

  lazy val reportAProblemPartialUrl: String =
    s"$contactBaseUrl/contact/problem_reports_ajax?service=$serviceIdentifier"

  lazy val reportAProblemNonJSUrl: String =
    s"$contactBaseUrl/contact/problem_reports_nonjs?service=$serviceIdentifier"

  lazy val loginUrl         = config.get[String]("urls.login")
  lazy val loginContinueUrl = config.get[String]("urls.loginContinue")

  lazy val incorpIdHost: String =
    servicesConfig.baseUrl("incorporated-entity-identification-frontend")

  lazy val pptServiceHost: String =
    servicesConfig.baseUrl("plastic-packaging-tax-registration")

  lazy val incorpIdJourneyCallbackUrl: String = config.get[String]("urls.incorpIdCallback")

  lazy val incorpJourneyUrl = s"$incorpIdHost/incorporated-entity-identification/api/journey"

  lazy val pptRegistrationUrl: String = s"$pptServiceHost/registrations"

  def incorpDetailsUrl(journeyId: String): String = s"$incorpJourneyUrl/$journeyId"

  def pptRegistrationUrl(id: String): String = s"$pptRegistrationUrl/$id"

  lazy val exitSurveyBaseUrl = servicesConfig.baseUrl("feedback-frontend")

  lazy val exitSurveyUrl = s"$exitSurveyBaseUrl/feedback/plastic-packaging-tax-registration"

  lazy val govUkUrl = config.get[String]("urls.govUk")
}
