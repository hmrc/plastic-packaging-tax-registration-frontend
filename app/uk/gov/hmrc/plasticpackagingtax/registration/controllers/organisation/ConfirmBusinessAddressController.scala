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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.organisation

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.RegistrationConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType.{
  OVERSEAS_COMPANY_UK_BRANCH,
  PARTNERSHIP,
  REGISTERED_SOCIETY,
  UK_COMPANY
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Cacheable
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation.confirm_business_address
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmBusinessAddressController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: confirm_business_address
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      val businessAddress = getBusinessAddress(request)
      businessAddress match {
        case Some(value) =>
          Future(
            Ok(
              page(value,
                   request.registration.organisationDetails.businessName.getOrElse(
                     "your organisation"
                   ),
                   commonRoutes.TaskListController.displayPage().url
              )
            )
          )
        case _ => Future.successful(Redirect(commonRoutes.TaskListController.displayPage()))
      }

    }

  private def getBusinessAddress(implicit request: JourneyRequest[AnyContent]): Option[Address] =
    request.registration.organisationDetails.organisationType match {
      case Some(UK_COMPANY) | Some(REGISTERED_SOCIETY) | Some(OVERSEAS_COMPANY_UK_BRANCH) =>
        request.registration.organisationDetails.incorporationDetails.map(
          _.companyAddress.toPptAddress
        )
      case Some(PARTNERSHIP) =>
        request.registration.organisationDetails.partnershipDetails.flatMap(
          _.partnershipOrCompanyAddress
        )
      case _ => None
    }

}

case class RegistrationException(message: String) extends Exception
