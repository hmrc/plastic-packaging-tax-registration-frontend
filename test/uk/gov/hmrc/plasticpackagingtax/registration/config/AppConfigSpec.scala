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

package uk.gov.hmrc.plasticpackagingtax.registration.config

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class AppConfigSpec extends AnyWordSpec with Matchers with MockitoSugar {

  private val validConfig: Config =
    ConfigFactory.parseString(
      """
        |microservice.services.incorporated-entity-identification-frontend.host=localhost
        |microservice.services.incorporated-entity-identification-frontend.port=9718
        |microservice.services.sole-trader-identification-frontend.host=localhost
        |microservice.services.sole-trader-identification-frontend.port=9717
        |microservice.services.partnership-identification-frontend.host=localhost
        |microservice.services.partnership-identification-frontend.port=9722
        |microservice.services.plastic-packaging-tax-registration.host=localhost
        |microservice.services.plastic-packaging-tax-registration.port=8502
        |microservice.services.contact-frontend.host=localhost
        |microservice.services.contact-frontend.port=9250
        |microservice.services.ppt-account-frontend.host=localhost
        |microservice.services.ppt-account-frontend.port=8505
        |features.isPreLaunch=false
        |urls.feedback.authenticatedLink="http://localhost:9250/contact/beta-feedback"
        |urls.feedback.unauthenticatedLink="http://localhost:9250/contact/beta-feedback-unauthenticated"
        |urls.mfaUplift="http://localhost:9553/bas-gateway/uplift-mfa"
        |urls.businessAccount="http://localhost:9020/business-account"
      """.stripMargin
    )

  private val emptyConfig: Config = ConfigFactory.parseString("")

  private def appConfig(conf: Configuration) =
    new AppConfig(conf, servicesConfig(conf))

  private def servicesConfig(conf: Configuration) = new ServicesConfig(conf)

  "The config" should {

    val validAppConfig: AppConfig = appConfig(Configuration(validConfig))

    "have 'incorpJourneyUrl' defined" in {
      validAppConfig.incorpDetailsUrl must be(
        "http://localhost:9718/incorporated-entity-identification/api/journey"
      )
    }

    "have 'soleTraderJourneyUrl' defined" in {
      validAppConfig.soleTraderJourneyInitUrl must be(
        "http://localhost:9717/sole-trader-identification/api/sole-trader-journey"
      )
    }

    "have 'generalPartnershipJourneyUrl' defined" in {
      validAppConfig.generalPartnershipJourneyUrl must be(
        "http://localhost:9722/partnership-identification/api/general-partnership-journey"
      )
    }

    "have 'pptRegistrationUrl' defined" in {
      validAppConfig.pptRegistrationUrl must be("http://localhost:8502/registrations")
    }

    "have 'pptRegistrationUrl(...)' defined" in {
      validAppConfig.pptRegistrationUrl("someId") must be(
        "http://localhost:8502/registrations/someId"
      )
    }

    "have 'pptSubscriptionsStatusUrl(...)' defined" in {
      validAppConfig.pptSubscriptionStatusUrl("someId") must be(
        "http://localhost:8502/subscriptions/status/someId"
      )
    }

    "have 'pptSubscriptionsCreateUrl(...)' defined" in {
      validAppConfig.pptSubscriptionCreateUrl("someId") must be(
        "http://localhost:8502/subscriptions/someId"
      )
    }

    "have 'authenticatedFeedbackUrl' defined" in {
      validAppConfig.authenticatedFeedbackUrl() must be(
        "http://localhost:9250/contact/beta-feedback?service=plastic-packaging-tax"
      )
    }

    "have 'unauthenticatedFeedbackUrl' defined" in {
      validAppConfig.unauthenticatedFeedbackUrl() must be(
        "http://localhost:9250/contact/beta-feedback-unauthenticated?service=plastic-packaging-tax"
      )
    }

    "have 'pptAccountUrl' defined" in {
      validAppConfig.pptAccountUrl must be("http://localhost:8505/plastic-packaging-tax/account")
    }

    "have 'mfaUpliftUrl' defined" in {
      validAppConfig.mfaUpliftUrl must be("http://localhost:9553/bas-gateway/uplift-mfa")
    }

    "have 'businessAccountUrl' defined" in {
      validAppConfig.businessAccountUrl must be("http://localhost:9020/business-account")
    }

    "inspect feature flags" when {
      "and check that 'isPreLaunch' is false" in {
        validAppConfig.isDefaultFeatureFlagEnabled(Features.isPreLaunch) mustBe false
      }
    }
  }

  "with an empty config" should {

    val emptyAppConfig: AppConfig = appConfig(Configuration(emptyConfig))

    "inspect feature flags" when {
      "and check that 'isPreLaunch' default value is 'true'" in {
        emptyAppConfig.isDefaultFeatureFlagEnabled(Features.isPreLaunch) mustBe false
      }
    }
  }
}
