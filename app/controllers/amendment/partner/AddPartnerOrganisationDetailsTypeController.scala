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

package controllers.amendment.partner

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import config.AppConfig
import connectors.grs.{PartnershipGrsConnector, RegisteredSocietyGrsConnector, SoleTraderGrsConnector, UkCompanyGrsConnector}
import controllers.actions.{EnrolledAuthAction, JourneyAction}
import controllers.partner.PartnerTypeControllerBase
import models.registration.AmendRegistrationUpdateService
import views.html.organisation.partner_type

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddPartnerOrganisationDetailsTypeController @Inject() (
                                                              journeyAction: JourneyAction,
                                                              registrationUpdater: AmendRegistrationUpdateService,
                                                              mcc: MessagesControllerComponents,
                                                              page: partner_type,
                                                              val appConfig: AppConfig,
                                                              val soleTraderGrsConnector: SoleTraderGrsConnector,
                                                              val ukCompanyGrsConnector: UkCompanyGrsConnector,
                                                              val registeredSocietyGrsConnector: RegisteredSocietyGrsConnector,
                                                              val partnershipGrsConnector: PartnershipGrsConnector
)(implicit ec: ExecutionContext)
    extends PartnerTypeControllerBase(journeyAction = journeyAction.amend,
                                      mcc,
                                      page,
                                      registrationUpdater
    ) {

  def displayPage(): Action[AnyContent] =
    doDisplayPage(submitCall = routes.AddPartnerOrganisationDetailsTypeController.submit())

  def submit(): Action[AnyContent] =
    doSubmit(submitCall = routes.AddPartnerOrganisationDetailsTypeController.submit())

  override def grsCallbackUrl(partnerId: Option[String]): String =
    appConfig.amendPartnerGrsCallbackUrl()

  override def redirectToPartnerNamePrompt(existingPartnerId: Option[String]): Future[Result] =
    Future.successful(Redirect(routes.AddPartnerNameController.displayPage()))

}
