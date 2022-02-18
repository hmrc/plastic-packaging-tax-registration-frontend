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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.partner

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs.{
  PartnershipGrsConnector,
  RegisteredSocietyGrsConnector,
  SoleTraderGrsConnector,
  UkCompanyGrsConnector
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthNoEnrolmentCheckAction
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner.PartnerNameControllerBase
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.AmendRegistrationUpdateService
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partner.partner_name_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AddPartnerNameController @Inject() (
  authenticate: AuthNoEnrolmentCheckAction,
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
