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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers

import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.SubscriptionsConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.registration_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  mcc: MessagesControllerComponents,
  registration_page: registration_page,
  subscriptionsConnector: SubscriptionsConnector
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      request.registration.organisationDetails.businessPartnerId() match {
        case Some(safeNumber) =>
          for {
            subscriptionStatus <- subscriptionsConnector.getSubscriptionStatus(safeNumber)
          } yield subscriptionStatus.subscriptionStatus match {
            case _ => Ok(registration_page(request.registration))
          }
        case None => Future.successful(Ok(registration_page(request.registration)))
      }
    }

}
