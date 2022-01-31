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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner

import play.api.Logger
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{RegistrationConnector, ServiceError}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.forms.partner.RemovePartner
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partner.remove_partner_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemovePartnerController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: remove_partner_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  private val logger = Logger(this.getClass)

  def displayPage(partnerId: String): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request: JourneyRequest[AnyContent] =>
      getPartnerName(partnerId) match {
        case Some(partnerName) => Ok(page(RemovePartner.form(), partnerName, partnerId))
        case _ =>
          Redirect(routes.PartnerListController.displayPage())
      }

    }

  def submit(partnerId: String): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request: JourneyRequest[AnyContent] =>
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
                      case Right(_) =>
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
