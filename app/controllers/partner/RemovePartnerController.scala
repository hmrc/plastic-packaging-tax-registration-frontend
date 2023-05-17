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

import connectors.{RegistrationConnector, ServiceError}
import controllers.actions.JourneyAction
import forms.partner.RemovePartner
import models.registration.{Cacheable, Registration}
import models.request.JourneyRequest
import play.api.Logger
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.partner.remove_partner_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemovePartnerController @Inject() (
                                          journeyAction: JourneyAction,
                                          override val registrationConnector: RegistrationConnector,
                                          mcc: MessagesControllerComponents,
                                          page: remove_partner_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  private val logger = Logger(this.getClass)

  def displayPage(partnerId: String): Action[AnyContent] =
    journeyAction.register { implicit request: JourneyRequest[AnyContent] =>
      getPartnerName(partnerId) match {
        case Some(partnerName) => Ok(page(RemovePartner.form(), partnerName, partnerId))
        case _ =>
          Redirect(routes.PartnerListController.displayPage())
      }

    }

  def submit(partnerId: String): Action[AnyContent] =
    journeyAction.register.async { implicit request: JourneyRequest[AnyContent] =>
      getPartnerName(partnerId) match {
        case Some(partnerName) =>
          RemovePartner.form()
            .bindFromRequest()
            .fold(
              (formWithErrors: Form[RemovePartner]) =>
                Future.successful(BadRequest(page(formWithErrors, partnerName, partnerId))),
              partner =>
                partner.value match {
                  case Some(true) =>
                    removePartner(partnerId).map {
                      case Right(registration) =>
                        if (registration.otherPartners.isEmpty)
                          Redirect(routes.PartnerTypeController.displayNewPartner())
                        else
                          Redirect(routes.PartnerListController.displayPage())
                      case Left(error) =>
                        logger.warn(
                          s"Failed to remove partner [$partnerName] with id [$partnerId] - ${error.getMessage}",
                          error
                        )
                        Redirect(routes.PartnerListController.displayPage())
                    }
                  case _ =>
                    Future.successful(Redirect(routes.PartnerListController.displayPage()))
                }
            )
        case _ => Future.successful(Redirect(routes.PartnerListController.displayPage()))
      }
    }

  private def getPartnerName(
    partnerId: String
  )(implicit request: JourneyRequest[AnyContent]): Option[String] =
    request.registration.findPartner(partnerId).map(_.name)

  private def removePartner(
    partnerId: String
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      registration.copy(organisationDetails =
        registration.organisationDetails.copy(partnershipDetails =
          registration.organisationDetails.partnershipDetails.map(
            partnershipDetails =>
              partnershipDetails.copy(partners =
                partnershipDetails.partners.filter(_.id != partnerId)
              )
          )
        )
      )
    }

}
