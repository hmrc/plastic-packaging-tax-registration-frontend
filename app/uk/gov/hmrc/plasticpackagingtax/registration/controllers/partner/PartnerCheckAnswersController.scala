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
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.RegistrationConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Cacheable
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyAction
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partner.partner_check_answers_page

import scala.concurrent.ExecutionContext

@Singleton
class PartnerCheckAnswersController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: partner_check_answers_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def nominatedPartner(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      val nominatedPartner = request.registration.organisationDetails.partnershipDetails.flatMap(
        _.nominatedPartner
      ).getOrElse(throw new IllegalStateException("Nominated partner absent"))
      Ok(page(nominatedPartner))
    }

  def partner(partnerId: String): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      val otherPartner = request.registration.organisationDetails.partnershipDetails.flatMap(
        _.findPartner(partnerId)
      ).getOrElse(throw new IllegalStateException(s"Partner with id [$partnerId] absent"))
      Ok(page(otherPartner))
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      Redirect(
        commonRoutes.TaskListController.displayPage()
      ) // TODO: direct to partner list page when created
    }

}
