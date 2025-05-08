/*
 * Copyright 2025 HM Revenue & Customs
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

import config.AppConfig
import connectors.grs.{PartnershipGrsConnector, RegisteredSocietyGrsConnector, SoleTraderGrsConnector, UkCompanyGrsConnector}
import controllers.actions.JourneyAction
import controllers.partner.{routes => partnerRoutes}
import controllers.{routes => commonRoutes}
import models.registration.NewRegistrationUpdateService
import play.api.mvc._
import views.html.partner.partner_name_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class PartnerNameController @Inject() (
  journeyAction: JourneyAction,
  registrationUpdater: NewRegistrationUpdateService,
  override val appConfig: AppConfig,
  override val soleTraderGrsConnector: SoleTraderGrsConnector,
  override val ukCompanyGrsConnector: UkCompanyGrsConnector,
  override val partnershipGrsConnector: PartnershipGrsConnector,
  override val registeredSocietyGrsConnector: RegisteredSocietyGrsConnector,
  mcc: MessagesControllerComponents,
  page: partner_name_page
)(implicit ec: ExecutionContext)
    extends PartnerNameControllerBase(
      journeyAction = journeyAction.register,
      ukCompanyGrsConnector = ukCompanyGrsConnector,
      soleTraderGrsConnector = soleTraderGrsConnector,
      partnershipGrsConnector = partnershipGrsConnector,
      registeredSocietyGrsConnector = registeredSocietyGrsConnector,
      registrationUpdater = registrationUpdater,
      mcc = mcc,
      page = page,
      appConfig = appConfig
    ) {

  def displayNewPartner(): Action[AnyContent] =
    doDisplay(
      None,
      partnerRoutes.PartnerTypeController.displayNewPartner(),
      partnerRoutes.PartnerNameController.submitNewPartner()
    )

  def displayExistingPartner(partnerId: String): Action[AnyContent] =
    doDisplay(
      Some(partnerId),
      partnerRoutes.PartnerCheckAnswersController.displayExistingPartner(partnerId),
      partnerRoutes.PartnerNameController.submitExistingPartner(partnerId)
    )

  def submitNewPartner(): Action[AnyContent] =
    doSubmit(
      None,
      partnerRoutes.PartnerTypeController.displayNewPartner(),
      partnerRoutes.PartnerNameController.submitNewPartner(),
      commonRoutes.TaskListController.displayPage()
    )

  def submitExistingPartner(partnerId: String): Action[AnyContent] =
    doSubmit(
      Some(partnerId),
      partnerRoutes.PartnerTypeController.displayExistingPartner(partnerId),
      partnerRoutes.PartnerNameController.submitExistingPartner(partnerId),
      commonRoutes.TaskListController.displayPage()
    )

  override def grsCallbackUrl(partnerId: Option[String]): String =
    appConfig.partnerGrsCallbackUrl(partnerId)

}
