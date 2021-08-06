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

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject() (config: Configuration, val servicesConfig: ServicesConfig) {

  lazy val assetsUrl: String = config.get[String]("assets.url")

  lazy val assetsPrefix: String = assetsUrl + config.get[String]("assets.version")

  lazy val serviceIdentifier = "plastic-packaging-tax"

  lazy val selfBaseUrl: String = config
    .getOptional[String]("platform.frontend.host")
    .getOrElse("http://localhost:9250")

  lazy val contactBaseUrl = servicesConfig.baseUrl("contact-frontend")

  lazy val reportAProblemPartialUrl: String =
    s"$contactBaseUrl/contact/problem_reports_ajax?service=$serviceIdentifier"

  lazy val reportAProblemNonJSUrl: String =
    s"$contactBaseUrl/contact/problem_reports_nonjs?service=$serviceIdentifier"

  lazy val loginUrl         = config.get[String]("urls.login")
  lazy val loginContinueUrl = config.get[String]("urls.loginContinue")

  lazy val incorpIdHost: String =
    servicesConfig.baseUrl("incorporated-entity-identification-frontend")

  lazy val incorpJourneyUrl                       = s"$incorpIdHost/incorporated-entity-identification/api/journey"
  def incorpDetailsUrl(journeyId: String): String = s"$incorpJourneyUrl/$journeyId"

  lazy val soleTraderHost: String =
    servicesConfig.baseUrl("sole-trader-identification-frontend")

  lazy val soleTraderJourneyUrl                       = s"$soleTraderHost/sole-trader-identification/api/journey"
  def soleTraderDetailsUrl(journeyId: String): String = s"$soleTraderJourneyUrl/$journeyId"

  lazy val partnershipHost: String =
    servicesConfig.baseUrl("partnership-identification-frontend")

  lazy val partnershipBaseUrl            = s"$partnershipHost/partnership-identification/api"
  lazy val generalPartnershipJourneyUrl  = s"$partnershipBaseUrl/general-partnership/journey"
  lazy val scottishPartnershipJourneyUrl = s"$partnershipBaseUrl/scottish-partnership/journey"
  // Define other partnership URLs here?
  def partnershipDetailsUrl(journeyId: String): String = s"$partnershipBaseUrl/journey/$journeyId"

  lazy val incorpIdJourneyCallbackUrl: String = config.get[String]("urls.incorpIdCallback")

  lazy val pptServiceHost: String =
    servicesConfig.baseUrl("plastic-packaging-tax-registration")

  lazy val emailVerificationHost: String =
    servicesConfig.baseUrl("email-verification")

  lazy val feedbackAuthenticatedLink: String = config.get[String]("urls.feedback.authenticatedLink")

  lazy val feedbackUnauthenticatedLink: String =
    config.get[String]("urls.feedback.unauthenticatedLink")

  lazy val exitSurveyUrl  = config.get[String]("urls.exitSurvey")
  lazy val hmrcPrivacyUrl = config.get[String]("urls.hmrcPrivacy")
  lazy val govUkUrl       = config.get[String]("urls.govUk")

  lazy val pptRegistrationUrl: String  = s"$pptServiceHost/registrations"
  lazy val pptSubscriptionsUrl: String = s"$pptServiceHost/subscriptions"
  lazy val emailVerificationUrl        = s"$emailVerificationHost/email-verification/verify-email"

  lazy val emailVerificationEnabled: Boolean =
    config.get[Boolean]("microservice.services.email-verification.enabled")

  lazy val pptRegistrationInfoUrl: String = config.get[String]("urls.pptRegistrationsInfoLink")

  def pptRegistrationUrl(id: String): String = s"$pptRegistrationUrl/$id"

  def pptSubscriptionStatusUrl(safeNumber: String): String =
    s"$pptSubscriptionsUrl/status/$safeNumber"

  def pptSubscriptionCreateUrl(safeNumber: String): String =
    s"$pptSubscriptionsUrl/$safeNumber"

  def getEmailVerificationStatusUrl(credId: String): String =
    s"$emailVerificationHost/email-verification/verification-status/$credId"

  def getSubmitPassscodeUrl(journeyId: String): String =
    s"$emailVerificationHost/email-verification/journey/$journeyId/passcode"

  def authenticatedFeedbackUrl(): String =
    s"$feedbackAuthenticatedLink?service=${serviceIdentifier}"

  def unauthenticatedFeedbackUrl(): String =
    s"$feedbackUnauthenticatedLink?service=${serviceIdentifier}"

  lazy val isPreLaunch: Boolean =
    config
      .getOptional[Boolean](s"features.isPreLaunch")
      .getOrElse(false)

  lazy val defaultFeatures: Map[String, Boolean] =
    config
      .getOptional[Map[String, Boolean]](s"features")
      .getOrElse(Map.empty)

}
