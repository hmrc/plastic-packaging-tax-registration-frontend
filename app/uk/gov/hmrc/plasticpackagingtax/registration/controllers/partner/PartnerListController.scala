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

import com.google.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.RegistrationConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.partner.AddPartner
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Cacheable
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partner.partner_list_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

@Singleton
class PartnerListController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: partner_list_page
) extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      Ok(page(AddPartner.form(), getNominatedPartner(request), getOtherPartners(request)))
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
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
                  commonRoutes.TaskListController.displayPage()
                ) // TODO: redirect to the select new partner type page when available
              case _ => Redirect(commonRoutes.TaskListController.displayPage())
            }
        )
    }

  private def getNominatedPartner(request: JourneyRequest[AnyContent]) =
    request.registration.organisationDetails.partnershipDetails.flatMap(
      _.nominatedPartner
    ).getOrElse(throw new IllegalStateException("Nominated partner absent"))

  private def getOtherPartners(request: JourneyRequest[AnyContent]) =
    request.registration.organisationDetails.partnershipDetails.flatMap(_.otherPartners).getOrElse(
      throw new IllegalStateException("Other partners absent")
    )

}
