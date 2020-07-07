/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.plasticpackagingtaxregistrationfrontend.config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig) {
  private val contactBaseUrl = servicesConfig.baseUrl("contact-frontend")

  private val assetsUrl         = config.get[String]("assets.url")
  private val serviceIdentifier = "MyService"

  val assetsPrefix: String   = assetsUrl + config.get[String]("assets.version")
  val analyticsToken: String = config.get[String](s"google-analytics.token")
  val analyticsHost: String  = config.get[String](s"google-analytics.host")

  val reportAProblemPartialUrl: String = s"$contactBaseUrl/contact/problem_reports_ajax?service=$serviceIdentifier"
  val reportAProblemNonJSUrl: String   = s"$contactBaseUrl/contact/problem_reports_nonjs?service=$serviceIdentifier"

}
