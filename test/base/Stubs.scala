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

package base

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

import akka.stream.testkit.NoMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import play.api.Configuration
import play.api.http.{DefaultFileMimeTypes, FileMimeTypes, FileMimeTypesConfiguration}
import play.api.i18n.{Langs, MessagesApi}
import play.api.mvc._
import play.api.test.Helpers._
import uk.gov.hmrc.govukfrontend.views.html.components
import uk.gov.hmrc.govukfrontend.views.html.components.{GovukHeader, Footer => _, _}
import uk.gov.hmrc.hmrcfrontend.config.{AccessibilityStatementConfig, TrackingConsentConfig}
import uk.gov.hmrc.hmrcfrontend.views.html.components.{
  HmrcFooter,
  HmrcHeader,
  HmrcReportTechnicalIssue
}
import uk.gov.hmrc.hmrcfrontend.views.html.helpers.{
  hmrcStandardFooter,
  HmrcFooterItems,
  HmrcTrackingConsentSnippet
}
import uk.gov.hmrc.plasticpackagingtax.registration.config.{AppConfig, TimeoutDialogConfig}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.main_template
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partials.{phaseBanner, siteHeader}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext

trait Stubs {

  val gdsGovukLayout = new GovukLayout(new components.GovukTemplate(govukHeader = new GovukHeader(),
                                                                    govukFooter = new GovukFooter(),
                                                                    new GovukSkipLink()
                                       ),
                                       new GovukHeader(),
                                       new GovukFooter(),
                                       new GovukBackLink()
  )

  val hmrcFooter = new hmrcStandardFooter(
    new HmrcFooter(),
    new HmrcFooterItems(new AccessibilityStatementConfig(Configuration(minimalConfig)))
  )

  val hmrcTrackingConsentSnippet = new HmrcTrackingConsentSnippet(
    new TrackingConsentConfig(Configuration(minimalConfig))
  )

  val appConfig =
    new AppConfig(Configuration(minimalConfig), servicesConfig(Configuration(minimalConfig)))

  val hmrcReportTechnicalIssue = new HmrcReportTechnicalIssue()
  val govukHeader              = new GovukHeader()
  val sHeader                  = new siteHeader(HmrcHeader)
  val govPBanner               = new GovukPhaseBanner(new govukTag())
  val pBanner                  = new phaseBanner(govPBanner, appConfig)
  val timeoutDialogConfig      = new TimeoutDialogConfig(servicesConfig(Configuration(minimalConfig)))

  val gdsMainTemplate = new main_template(govukHeader = govukHeader,
                                          govukLayout = gdsGovukLayout,
                                          govukBackLink = new components.GovukBackLink(),
                                          siteHeader = sHeader,
                                          timeoutDialogConfig = timeoutDialogConfig,
                                          govukPhaseBanner = govPBanner,
                                          hmrcFooter = hmrcFooter,
                                          phaseBanner = pBanner,
                                          hmrcTrackingConsentSnippet = hmrcTrackingConsentSnippet,
                                          hmrcReportTechnicalIssue = hmrcReportTechnicalIssue
  )

  private val minimalConfig: Config =
    ConfigFactory.parseString("""
                                                                  |timeoutDialog.timeout=13min
                                                                  |timeoutDialog.countdown=
                                                                  |tracking-consent-frontend.gtm.container=b
    """.stripMargin)

  def stubMessagesControllerComponents(
    bodyParser: BodyParser[AnyContent] = stubBodyParser(AnyContentAsEmpty),
    playBodyParsers: PlayBodyParsers = stubPlayBodyParsers(NoMaterializer),
    messagesApi: MessagesApi = stubMessagesApi(),
    langs: Langs = stubLangs(),
    fileMimeTypes: FileMimeTypes = new DefaultFileMimeTypes(FileMimeTypesConfiguration()),
    executionContext: ExecutionContext = ExecutionContext.global
  ): MessagesControllerComponents =
    DefaultMessagesControllerComponents(
      new DefaultMessagesActionBuilderImpl(bodyParser, messagesApi)(executionContext),
      DefaultActionBuilder(bodyParser)(executionContext),
      playBodyParsers,
      messagesApi,
      langs,
      fileMimeTypes,
      executionContext
    )

  def servicesConfig(conf: Configuration) = new ServicesConfig(conf)

}
