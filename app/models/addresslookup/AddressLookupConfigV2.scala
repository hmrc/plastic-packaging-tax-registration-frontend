/*
 * Copyright 2023 HM Revenue & Customs
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

package models.addresslookup

import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Call
import config.AppConfig
import controllers.{routes => commonRoutes}
import views.components.Styles.gdsFieldsetPageHeading

case class AddressLookupConfigV2(version: Int = 2, options: JourneyOptions, labels: JourneyLabels)

object AddressLookupConfigV2 {
  implicit val format: OFormat[AddressLookupConfigV2] = Json.format[AddressLookupConfigV2]

  def apply(
    continue: Call,
    appConfig: AppConfig,
    messagesPrefix: String,
    entityName: Option[String]
  )(implicit messagesApi: MessagesApi): AddressLookupConfigV2 = {
    val en: Messages = MessagesImpl(Lang("en"), messagesApi)

    def getEntityName = entityName.getOrElse(en("missing.organisationName"))

    new AddressLookupConfigV2(options = JourneyOptions(continueUrl = appConfig.selfUrl(continue),
                                                       signOutHref = appConfig.signOutLink,
                                                       serviceHref = appConfig.selfUrl(
                                                         commonRoutes.StartRegistrationController.startRegistration()
                                                       )
                              ),
                              labels = JourneyLabels(en =
                                LanguageLabels(appLevelLabels =
                                                 AppLevelLabels(navTitle = en("service.name")),
                                               selectPageLabels = SelectPageLabels(
                                                 title = en(s"${messagesPrefix}.select.title",
                                                            getEntityName
                                                 ),
                                                 heading = en(s"${messagesPrefix}.select.heading",
                                                              getEntityName
                                                 )
                                               ),
                                               lookupPageLabels = LookupPageLabels(
                                                 title = en(s"${messagesPrefix}.lookup.title",
                                                            getEntityName
                                                 ),
                                                 heading = en(s"${messagesPrefix}.lookup.heading",
                                                              getEntityName
                                                 )
                                               ),
                                               confirmPageLabels = ConfirmPageLabels(
                                                 title = en(s"${messagesPrefix}.confirm.title",
                                                            getEntityName
                                                 ),
                                                 heading = en(s"${messagesPrefix}.confirm.heading",
                                                              getEntityName
                                                 )
                                               ),
                                               editPageLabels = EditPageLabels(
                                                 title = en(s"${messagesPrefix}.edit.title",
                                                            getEntityName
                                                 ),
                                                 heading = en(s"${messagesPrefix}.edit.heading",
                                                              getEntityName
                                                 )
                                               )
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

case class LanguageLabels(
  appLevelLabels: AppLevelLabels,
  selectPageLabels: SelectPageLabels,
  lookupPageLabels: LookupPageLabels,
  confirmPageLabels: ConfirmPageLabels,
  editPageLabels: EditPageLabels
)

object LanguageLabels {
  implicit val format: OFormat[LanguageLabels] = Json.format[LanguageLabels]
}

case class AppLevelLabels(navTitle: String)

object AppLevelLabels {
  implicit val format: OFormat[AppLevelLabels] = Json.format[AppLevelLabels]
}

case class SelectPageLabels(title: String, heading: String)

object SelectPageLabels {
  implicit val format: OFormat[SelectPageLabels] = Json.format[SelectPageLabels]
}

case class LookupPageLabels(title: String, heading: String)

object LookupPageLabels {
  implicit val format: OFormat[LookupPageLabels] = Json.format[LookupPageLabels]
}

case class ConfirmPageLabels(title: String, heading: String)

object ConfirmPageLabels {
  implicit val format: OFormat[ConfirmPageLabels] = Json.format[ConfirmPageLabels]
}

case class EditPageLabels(title: String, heading: String)

object EditPageLabels {
  implicit val format: OFormat[EditPageLabels] = Json.format[EditPageLabels]
}
