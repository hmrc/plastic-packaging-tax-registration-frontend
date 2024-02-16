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

package controllers.amendment.group

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.AppConfig
import connectors._
import connectors.grs.{PartnershipGrsConnector, RegisteredSocietyGrsConnector, SoleTraderGrsConnector, UkCompanyGrsConnector}
import controllers.actions.JourneyAction
import controllers.group.OrganisationDetailsTypeControllerBase
import models.registration.AmendRegistrationUpdateService
import views.html.group.organisation_type

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AddGroupMemberOrganisationDetailsTypeController @Inject() (
  journeyAction: JourneyAction,
  mcc: MessagesControllerComponents,
  appConfig: AppConfig,
  page: organisation_type,
  registrationUpdater: AmendRegistrationUpdateService,
  val soleTraderGrsConnector: SoleTraderGrsConnector,
  val ukCompanyGrsConnector: UkCompanyGrsConnector,
  val partnershipGrsConnector: PartnershipGrsConnector,
  val registeredSocietyGrsConnector: RegisteredSocietyGrsConnector,
  val registrationConnector: RegistrationConnector
)(implicit ec: ExecutionContext)
    extends OrganisationDetailsTypeControllerBase(journeyAction.amend, appConfig, page, registrationUpdater, mcc) {

  def displayPage(): Action[AnyContent] =
    doDisplayPage(None, routes.AddGroupMemberOrganisationDetailsTypeController.submit())

  def submit() = doSubmit(None, routes.AddGroupMemberOrganisationDetailsTypeController.submit())

  override def grsCallbackUrl(organisationId: Option[String]): String =
    appConfig.amendGroupMemberGrsCallbackUrl()

}
