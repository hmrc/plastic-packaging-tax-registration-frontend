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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.liability

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.StartRegistrationController
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.RegType
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.check_liability_details_answers_page
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class CheckLiabilityDetailsAnswersController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  mcc: MessagesControllerComponents,
  startRegistrationController: StartRegistrationController,
  page: check_liability_details_answers_page
) extends LiabilityController(mcc) with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      Ok(page(request.registration, backLink, startRegistrationController.startLink))
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { _ =>
      Future.successful(Redirect(commonRoutes.RegistrationController.displayPage()))
    }

  private def backLink()(implicit request: JourneyRequest[AnyContent]): Call =
    if (isGroupRegistrationEnabled)
      request.registration.registrationType match {
        case Some(regType) if regType == RegType.GROUP =>
          routes.MembersUnderGroupControlController.displayPage()
        case _ => routes.RegistrationTypeController.displayPage()
      }
    else
      routes.LiabilityStartDateController.displayPage()

}
