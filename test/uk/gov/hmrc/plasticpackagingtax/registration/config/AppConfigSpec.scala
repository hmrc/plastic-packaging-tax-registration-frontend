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
        |microservice.services.plastic-packaging-tax-registration.host=localhost
        |microservice.services.plastic-packaging-tax-registration.port=8502
      """.stripMargin
    )

  private val validServicesConfiguration = Configuration(validConfig)
  private val validAppConfig: AppConfig  = appConfig(validServicesConfiguration)

  private def appConfig(conf: Configuration) =
    new AppConfig(conf, servicesConfig(conf))

  private def servicesConfig(conf: Configuration) = new ServicesConfig(conf)

  "The config" should {

    "have 'incorpJourneyUrl' defined" in {
      validAppConfig.incorpJourneyUrl must be(
        "http://localhost:9718/incorporated-entity-identification/api/journey"
      )
    }

    "have 'pptRegistrationUrl' defined" in {
      validAppConfig.pptRegistrationUrl must be("http://localhost:8502/registrations")
    }

    "have 'incorpDetailsUrl' defined" in {
      validAppConfig.incorpDetailsUrl("someId") must be(
        "http://localhost:9718/incorporated-entity-identification/api/journey/someId"
      )
    }

    "have 'pptRegistrationUrl(...)' defined" in {
      validAppConfig.pptRegistrationUrl("someId") must be(
        "http://localhost:8502/registrations/someId"
      )
    }
  }
}
