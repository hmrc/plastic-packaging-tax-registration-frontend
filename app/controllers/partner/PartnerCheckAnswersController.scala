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

package controllers.partner

import com.google.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import connectors.RegistrationConnector
import controllers.actions.auth.RegistrationAuthAction
import controllers.actions.getRegistration.GetRegistrationAction
import models.registration.Cacheable
import views.html.partner.partner_check_answers_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

@Singleton
class PartnerCheckAnswersController @Inject() (
                                                authenticate: RegistrationAuthAction,
                                                journeyAction: GetRegistrationAction,
                                                override val registrationConnector: RegistrationConnector,
                                                mcc: MessagesControllerComponents,
                                                page: partner_check_answers_page
) extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayNewPartner(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      Ok(
        page(
          request.registration.newPartner.getOrElse(
            throw new IllegalStateException("New partner absent")
          )
        )
      )
    }

  def displayExistingPartner(partnerId: String): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      val partner = request.registration.organisationDetails.partnershipDetails.flatMap(
        _.findPartner(partnerId)
      ).getOrElse(throw new IllegalStateException(s"Partner with id [$partnerId] absent"))
      Ok(page(partner))
    }

  def continue(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      request.registration.organisationDetails.partnershipDetails.map(_.partners.size) match {
        case Some(1) =>
          Redirect(
            routes.PartnerListController.displayPage()
          )
        case _ => Redirect(routes.PartnerListController.displayPage())
      }
    }

}
