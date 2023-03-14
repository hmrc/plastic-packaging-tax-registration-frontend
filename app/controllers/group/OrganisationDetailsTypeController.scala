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

package controllers.group

import play.api.mvc._
import config.AppConfig
import connectors.grs.{PartnershipGrsConnector, RegisteredSocietyGrsConnector, SoleTraderGrsConnector, UkCompanyGrsConnector}
import controllers.actions.{JourneyAction, NotEnrolledAuthAction}
import controllers.actions.getRegistration.GetRegistrationAction
import models.registration.NewRegistrationUpdateService
import views.html.group.organisation_type

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class OrganisationDetailsTypeController @Inject() (
                                                    journeyAction: JourneyAction,
                                                    mcc: MessagesControllerComponents,
                                                    appConfig: AppConfig,
                                                    page: organisation_type,
                                                    registrationUpdater: NewRegistrationUpdateService,
                                                    val soleTraderGrsConnector: SoleTraderGrsConnector,
                                                    val ukCompanyGrsConnector: UkCompanyGrsConnector,
                                                    val partnershipGrsConnector: PartnershipGrsConnector,
                                                    val registeredSocietyGrsConnector: RegisteredSocietyGrsConnector
)(implicit ec: ExecutionContext)
    extends OrganisationDetailsTypeControllerBase(
                                                  journeyAction.register,
                                                  appConfig,
                                                  page,
                                                  registrationUpdater,
                                                  mcc
    ) {

  def displayPageNewMember() =
    doDisplayPage(None, routes.OrganisationDetailsTypeController.submitNewMember())

  def displayPageAmendMember(memberId: String) =
    doDisplayPage(Some(memberId),
                  routes.OrganisationDetailsTypeController.submitAmendMember(memberId)
    )

  def submitNewMember() = doSubmit(None, routes.OrganisationDetailsTypeController.submitNewMember())

  def submitAmendMember(memberId: String) =
    doSubmit(Some(memberId), routes.OrganisationDetailsTypeController.submitAmendMember(memberId))

  override def grsCallbackUrl(organisationId: Option[String]): String =
    appConfig.groupMemberGrsCallbackUrl(organisationId)

}
