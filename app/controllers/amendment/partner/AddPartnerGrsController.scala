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

import play.api.mvc.MessagesControllerComponents
import connectors.SubscriptionsConnector
import connectors.grs.{PartnershipGrsConnector, RegisteredSocietyGrsConnector, SoleTraderGrsConnector, UkCompanyGrsConnector}
import controllers.actions.JourneyAction
import controllers.partner.PartnerGrsControllerBase
import models.registration.AmendRegistrationUpdateService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AddPartnerGrsController @Inject() (
                                          journeyAction: JourneyAction,
                                          ukCompanyGrsConnector: UkCompanyGrsConnector,
                                          soleTraderGrsConnector: SoleTraderGrsConnector,
                                          partnershipGrsConnector: PartnershipGrsConnector,
                                          registeredSocietyGrsConnector: RegisteredSocietyGrsConnector,
                                          registrationUpdater: AmendRegistrationUpdateService,
                                          subscriptionsConnector: SubscriptionsConnector,
                                          mcc: MessagesControllerComponents
)(implicit val executionContext: ExecutionContext)
    extends PartnerGrsControllerBase(journeyAction = journeyAction.amend,
                                     ukCompanyGrsConnector = ukCompanyGrsConnector,
                                     soleTraderGrsConnector = soleTraderGrsConnector,
                                     partnershipGrsConnector = partnershipGrsConnector,
                                     registeredSocietyGrsConnector = registeredSocietyGrsConnector,
                                     subscriptionsConnector = subscriptionsConnector,
                                     registrationUpdater = registrationUpdater,
                                     mcc = mcc
    ) {

  def grsCallbackNewPartner(journeyId: String) =
    grsCallback(journeyId, None, routes.AddPartnerContactDetailsNameController.displayPage)

}
