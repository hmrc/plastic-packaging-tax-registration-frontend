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

package controllers.amendment.partner

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.AppConfig
import connectors.grs.{
  PartnershipGrsConnector,
  RegisteredSocietyGrsConnector,
  SoleTraderGrsConnector,
  UkCompanyGrsConnector
}
import controllers.actions.EnrolledAuthAction
import controllers.partner.PartnerNameControllerBase
import models.registration.AmendRegistrationUpdateService
import models.request.AmendmentJourneyAction
import views.html.partner.partner_name_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AddPartnerNameController @Inject() (
                                           authenticate: EnrolledAuthAction,
                                           journeyAction: AmendmentJourneyAction,
                                           override val appConfig: AppConfig,
                                           ukCompanyGrsConnector: UkCompanyGrsConnector,
                                           soleTraderGrsConnector: SoleTraderGrsConnector,
                                           partnershipGrsConnector: PartnershipGrsConnector,
                                           registeredSocietyGrsConnector: RegisteredSocietyGrsConnector,
                                           registrationUpdater: AmendRegistrationUpdateService,
                                           mcc: MessagesControllerComponents,
                                           page: partner_name_page
)(implicit val executionContext: ExecutionContext)
    extends PartnerNameControllerBase(authenticate = authenticate,
                                      journeyAction = journeyAction,
                                      ukCompanyGrsConnector = ukCompanyGrsConnector,
                                      soleTraderGrsConnector = soleTraderGrsConnector,
                                      partnershipGrsConnector = partnershipGrsConnector,
                                      registeredSocietyGrsConnector = registeredSocietyGrsConnector,
                                      registrationUpdater = registrationUpdater,
                                      mcc = mcc,
                                      appConfig = appConfig,
                                      page = page
    ) {

  def displayPage(): Action[AnyContent] =
    doDisplay(None,
              routes.AddPartnerOrganisationDetailsTypeController.displayPage(),
              routes.AddPartnerNameController.submit()
    )

  def submit(): Action[AnyContent] =
    doSubmit(None,
             routes.AddPartnerOrganisationDetailsTypeController.displayPage(),
             routes.AddPartnerNameController.submit(),
             routes.PartnersListController.displayPage()
    )

  override def grsCallbackUrl(partnerId: Option[String]): String =
    appConfig.amendPartnerGrsCallbackUrl()

}
