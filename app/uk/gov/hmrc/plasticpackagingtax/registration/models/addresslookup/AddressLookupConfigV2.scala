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

package uk.gov.hmrc.plasticpackagingtax.registration.models.addresslookup

import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Call
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.views.components.Styles.gdsFieldsetPageHeading

case class AddressLookupConfigV2(version: Int = 2, options: JourneyOptions, labels: JourneyLabels)

object AddressLookupConfigV2 {
  implicit val format: OFormat[AddressLookupConfigV2] = Json.format[AddressLookupConfigV2]

  def apply(continue: Call, appConfig: AppConfig)(implicit
    messagesApi: MessagesApi
  ): AddressLookupConfigV2 = {
    val en: Messages = MessagesImpl(Lang("en"), messagesApi)

    new AddressLookupConfigV2(options = JourneyOptions(continueUrl = appConfig.selfUrl(continue),
                                                       signOutHref = appConfig.externalSignOutLink,
                                                       serviceHref = appConfig.selfUrl(
                                                         commonRoutes.StartController.displayStartPage
                                                       )
                              ),
                              labels = JourneyLabels(en =
                                LanguageLabels(appLevelLabels =
                                  AppLevelLabels(navTitle = en("service.name"))
                                )
                              )
    )
  }

}

case class JourneyOptions(
  continueUrl: String,
  signOutHref: String,
  serviceHref: String,
  showPhaseBanner: Boolean = true,
  pageHeadingStyle: String = gdsFieldsetPageHeading,
  ukMode: Boolean = true,
  disableTranslations: Boolean = true,
  includeHMRCBranding: Boolean = false
)

object JourneyOptions {
  implicit val format: OFormat[JourneyOptions] = Json.format[JourneyOptions]
}

case class JourneyLabels(en: LanguageLabels)

object JourneyLabels {
  implicit val format: OFormat[JourneyLabels] = Json.format[JourneyLabels]
}

case class LanguageLabels(appLevelLabels: AppLevelLabels)

object LanguageLabels {
  implicit val format: OFormat[LanguageLabels] = Json.format[LanguageLabels]
}

case class AppLevelLabels(navTitle: String)

object AppLevelLabels {
  implicit val format: OFormat[AppLevelLabels] = Json.format[AppLevelLabels]
}
