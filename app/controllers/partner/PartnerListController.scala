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

package controllers.partner

import com.google.inject.{Inject, Singleton}
import connectors.RegistrationConnector
import controllers.actions.JourneyAction
import controllers.{routes => commonRoutes}
import forms.partner.AddPartner
import models.registration.Cacheable
import models.request.JourneyRequest
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.partner.partner_list_page

@Singleton
class PartnerListController @Inject() (
                                        journeyAction: JourneyAction,
                                        override val registrationConnector: RegistrationConnector,
                                        mcc: MessagesControllerComponents,
                                        page: partner_list_page
) extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    journeyAction.register { implicit request =>
      Ok(page(AddPartner.form(), getNominatedPartner(request), getOtherPartners(request)))
    }

  def submit(): Action[AnyContent] =
    journeyAction.register { implicit request =>
      AddPartner.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[AddPartner]) =>
            BadRequest(
              page(formWithErrors, getNominatedPartner(request), getOtherPartners(request))
            ),
          addPartner =>
            addPartner.answer match {
              case Some(true) =>
                Redirect(
                  routes.PartnerTypeController.displayNewPartner()
                )
              case _ => Redirect(commonRoutes.TaskListController.displayPage())
            }
        )
    }

  private def getNominatedPartner(request: JourneyRequest[AnyContent]) =
    request.registration.nominatedPartner.getOrElse(
      throw new IllegalStateException("Nominated partner absent")
    )

  private def getOtherPartners(request: JourneyRequest[AnyContent]) =
    request.registration.otherPartners

}
