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
import play.api.mvc.Call
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject() (config: Configuration, val servicesConfig: ServicesConfig) {

  lazy val assetsUrl: String = config.get[String]("assets.url")

  lazy val assetsPrefix: String = assetsUrl + config.get[String]("assets.version")

  lazy val serviceIdentifier = "plastic-packaging-tax"

  lazy val selfBaseUrl: String = config
    .getOptional[String]("platform.frontend.host")
    .getOrElse("http://localhost:8503")

  def isRunningLocally() = !config.getOptional[String]("platform.frontend.host").isDefined

  def selfUrl(call: Call): String = s"$selfBaseUrl${call.url}"

  lazy val contactBaseUrl = config
    .getOptional[String]("platform.frontend.host")
    .getOrElse("http://localhost:9250")

  lazy val reportTechincalProblemUrl: String =
    s"$contactBaseUrl/contact/report-technical-problem?service=$serviceIdentifier"

  lazy val contactHmrcUrl: String =
    s"$contactBaseUrl/contact/contact-hmrc?service=$serviceIdentifier"

  lazy val loginUrl         = config.get[String]("urls.login")
  lazy val loginContinueUrl = config.get[String]("urls.loginContinue")

  lazy val signOutLink = {
    val signOutUrl = routes.SignOutController.signOut(
      uk.gov.hmrc.plasticpackagingtax.registration.views.model.SignOutReason.UserAction
    )
    if (isRunningLocally())
      selfUrl(signOutUrl)
    else // Use a relative link
      signOutUrl.url
  }

  lazy val incorpIdHost: String =
    servicesConfig.baseUrl("incorporated-entity-identification-frontend")

  private lazy val incorpBaseUrl           = s"$incorpIdHost/incorporated-entity-identification/api"
  lazy val incorpLimitedCompanyJourneyUrl  = s"$incorpBaseUrl/limited-company-journey"
  lazy val incorpRegistedSocietyJourneyUrl = s"$incorpBaseUrl/registered-society-journey"
  lazy val incorpDetailsUrl                = s"$incorpBaseUrl/journey"

  lazy val soleTraderHost: String =
    servicesConfig.baseUrl("sole-trader-identification-frontend")

  lazy val soleTraderJourneyInitUrl =
    s"$soleTraderHost/sole-trader-identification/api/sole-trader-journey"

  lazy val soleTraderJourneyUrl = s"$soleTraderHost/sole-trader-identification/api/journey"

  lazy val partnershipHost: String =
    servicesConfig.baseUrl("partnership-identification-frontend")

  lazy val partnershipBaseUrl            = s"$partnershipHost/partnership-identification/api"
  lazy val partnershipJourneyUrl         = s"$partnershipBaseUrl/journey"
  lazy val generalPartnershipJourneyUrl  = s"$partnershipBaseUrl/general-partnership-journey"
  lazy val scottishPartnershipJourneyUrl = s"$partnershipBaseUrl/scottish-partnership-journey"
  lazy val limitedPartnershipJourneyUrl  = s"$partnershipBaseUrl/limited-partnership-journey"

  lazy val scottishLimitedPartnershipJourneyUrl =
    s"$partnershipBaseUrl/scottish-limited-partnership-journey"

  lazy val limitedLiabilityPartnershipJourneyUrl =
    s"$partnershipBaseUrl/limited-liability-partnership-journey"

  // Define other partnership URLs here?

  lazy val grsCallbackUrl: String = config.get[String]("urls.grsCallback")

  def groupMemberGrsCallbackUrl(groupMemberId: Option[String]): String =
    groupMemberId match {
      case Some(groupMemberId) => s"${config.get[String]("urls.groupMemberGrsCallback")}/$groupMemberId"
      case None => config.get[String]("urls.groupMemberGrsCallback")
    }

  lazy val pptServiceHost: String =
    servicesConfig.baseUrl("plastic-packaging-tax-registration")

  lazy val emailVerificationHost: String =
    servicesConfig.baseUrl("email-verification")

  lazy val pptAccountHost: String =
    config.getOptional[String]("platform.frontend.host").getOrElse(
      servicesConfig.baseUrl("ppt-account-frontend")
    )

  lazy val feedbackAuthenticatedLink: String = config.get[String]("urls.feedback.authenticatedLink")

  lazy val feedbackUnauthenticatedLink: String =
    config.get[String]("urls.feedback.unauthenticatedLink")

  lazy val exitSurveyUrl  = config.get[String]("urls.exitSurvey")
  lazy val hmrcPrivacyUrl = config.get[String]("urls.hmrcPrivacy")
  lazy val govUkUrl       = config.get[String]("urls.govUk")

  lazy val pptRegistrationUrl: String  = s"$pptServiceHost/registrations"
  lazy val pptSubscriptionsUrl: String = s"$pptServiceHost/subscriptions"
  lazy val emailVerificationUrl        = s"$emailVerificationHost/email-verification/verify-email"
  lazy val pptEnrolmentUrl: String     = s"$pptServiceHost/enrolment"

  lazy val pptRegistrationInfoUrl: String = config.get[String]("urls.pptRegistrationsInfoLink")

  lazy val addressLookupHost: String =
    servicesConfig.baseUrl("address-lookup-frontend")

  lazy val addressLookupInitUrl: String =
    s"$addressLookupHost/api/init"

  lazy val addressLookupConfirmedUrl: String =
    s"$addressLookupHost/api/confirmed"

  def pptRegistrationUrl(id: String): String = s"$pptRegistrationUrl/$id"

  def pptSubscriptionStatusUrl(safeNumber: String): String =
    s"$pptSubscriptionsUrl/status/$safeNumber"

  def pptSubscriptionGetUrl(pptReference: String): String =
    s"$pptSubscriptionsUrl/$pptReference"

  def pptSubscriptionUpdateUrl(pptReference: String): String =
    s"$pptSubscriptionsUrl/$pptReference"

  def pptSubscriptionCreateUrl(safeNumber: String): String =
    s"$pptSubscriptionsUrl/$safeNumber"

  def getEmailVerificationStatusUrl(credId: String): String =
    s"$emailVerificationHost/email-verification/verification-status/$credId"

  def getTestOnlyPasscodeUrl: String =
    s"$emailVerificationHost/test-only/passcodes"

  def getSubmitPassscodeUrl(journeyId: String): String =
    s"$emailVerificationHost/email-verification/journey/$journeyId/passcode"

  def authenticatedFeedbackUrl(): String =
    s"$feedbackAuthenticatedLink?service=${serviceIdentifier}"

  def unauthenticatedFeedbackUrl(): String =
    s"$feedbackUnauthenticatedLink?service=${serviceIdentifier}"

  def isDefaultFeatureFlagEnabled(flag: String): Boolean =
    config
      .getOptional[Boolean](s"features.$flag")
      .getOrElse(false)

  lazy val defaultFeatures: Map[String, Boolean] =
    config
      .getOptional[Map[String, Boolean]](s"features")
      .getOrElse(Map.empty)

  lazy val pptAccountUrl = s"$pptAccountHost/plastic-packaging-tax/account"

  lazy val mfaUpliftUrl: String = config.get[String]("urls.mfaUplift")

  lazy val businessAccountUrl: String = config.get[String]("urls.businessAccount")

  lazy val accessibilityStatementServicePath: String =
    config.get[String]("accessibility-statement.service-path")

  lazy val grsAccessibilityStatementPath: String =
    "/accessibility-statement" + accessibilityStatementServicePath

}
