/*
 * Copyright 2024 HM Revenue & Customs
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

package base.it

import builders.RegistrationBuilder
import com.codahale.metrics.{MetricFilter, SharedMetricRegistries, Timer}
import uk.gov.hmrc.play.bootstrap.metrics.Metrics
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.DefaultAwaitTimeout
import spec.PptTestData
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.ExecutionContext

class ConnectorISpec
    extends WiremockTestServer
    with GuiceOneAppPerSuite
    with DefaultAwaitTimeout
    with PptTestData
    with RegistrationBuilder {

  def overrideConfig: Map[String, Any] =
    Map(
      "microservice.services.incorporated-entity-identification-frontend.host" -> wireHost,
      "microservice.services.incorporated-entity-identification-frontend.port" -> wirePort,
      "microservice.services.plastic-packaging-tax-registration.host"          -> wireHost,
      "microservice.services.plastic-packaging-tax-registration.port"          -> wirePort,
      "microservice.services.sole-trader-identification-frontend.host"         -> wireHost,
      "microservice.services.sole-trader-identification-frontend.port"         -> wirePort,
      "microservice.services.partnership-identification-frontend.host"         -> wireHost,
      "microservice.services.partnership-identification-frontend.port"         -> wirePort,
      "microservice.services.email-verification.host"                          -> wireHost,
      "microservice.services.email-verification.port"                          -> wirePort,
      "microservice.services.address-lookup-frontend.host"                     -> wireHost,
      "microservice.services.address-lookup-frontend.port"                     -> wirePort,
      "microservice.services.ppt-account-frontend.host"                        -> wireHost,
      "microservice.services.ppt-account-frontend.port"                        -> wirePort
    )

  override def fakeApplication(): Application = {
    SharedMetricRegistries.clear()
    new GuiceApplicationBuilder().configure(overrideConfig).build()
  }

  protected implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  protected implicit val hc: HeaderCarrier    = HeaderCarrier()
  protected val httpClient: DefaultHttpClient = app.injector.instanceOf[DefaultHttpClient]
  protected val metrics: Metrics              = app.injector.instanceOf[Metrics]

  def getTimer(name: String): Timer =
    SharedMetricRegistries
      .getOrCreate("plastic-packaging-tax-registration-frontend")
      .getTimers(MetricFilter.startsWith(name))
      .get(name)

}
