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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.liability

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.RegistrationConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.{RegType, RegistrationType}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Cacheable
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.registration_type_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationTypeController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: registration_type_page
)(implicit ec: ExecutionContext)
    extends LiabilityController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      request.registration.registrationType match {
        case Some(regType) =>
          Ok(page(RegistrationType.form().fill(RegistrationType(Some(regType))), backLink))
        case _ => Ok(page(RegistrationType.form(), backLink))
      }
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      RegistrationType.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[RegistrationType]) =>
            Future.successful(BadRequest(page(formWithErrors, backLink))),
          registrationType =>
            update(
              registration => registration.copy(registrationType = registrationType.value)
            ).map {
              case Right(registration) =>
                registration.registrationType match {
                  case Some(regType) if regType == RegType.GROUP =>
                    Redirect(routes.MembersUnderGroupControlController.displayPage().url)
                  case _ =>
                    Redirect(routes.CheckLiabilityDetailsAnswersController.displayPage().url)
                }
              case Left(error) => throw error
            }
        )
    }

  private def backLink()(implicit request: JourneyRequest[AnyContent]): Call =
    if (isPreLaunch)
      routes.LiabilityWeightExpectedController.displayPage()
    else
      routes.LiabilityStartDateController.displayPage()

}
