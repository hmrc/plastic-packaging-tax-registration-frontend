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

package controllers.partner

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import connectors._
import connectors.grs._
import controllers.actions.{JourneyAction, NotEnrolledAuthAction}
import controllers.actions.getRegistration.GetRegistrationAction
import models.registration.NewRegistrationUpdateService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class PartnerGrsController @Inject() (
                                       journeyAction: JourneyAction,
                                       ukCompanyGrsConnector: UkCompanyGrsConnector,
                                       soleTraderGrsConnector: SoleTraderGrsConnector,
                                       partnershipGrsConnector: PartnershipGrsConnector,
                                       registeredSocietyGrsConnector: RegisteredSocietyGrsConnector,
                                       registrationUpdater: NewRegistrationUpdateService,
                                       subscriptionsConnector: SubscriptionsConnector,
                                       mcc: MessagesControllerComponents
)(implicit val executionContext: ExecutionContext)
    extends PartnerGrsControllerBase(journeyAction = journeyAction.register,
                                     ukCompanyGrsConnector = ukCompanyGrsConnector,
                                     soleTraderGrsConnector = soleTraderGrsConnector,
                                     partnershipGrsConnector = partnershipGrsConnector,
                                     registeredSocietyGrsConnector = registeredSocietyGrsConnector,
                                     subscriptionsConnector = subscriptionsConnector,
                                     registrationUpdater = registrationUpdater,
                                     mcc = mcc
    ) {

  def grsCallbackNewPartner(journeyId: String): Action[AnyContent] =
    grsCallback(journeyId, None, routes.PartnerContactNameController.displayNewPartner)

  def grsCallbackExistingPartner(journeyId: String, partnerId: String): Action[AnyContent] =
    grsCallback(journeyId,
                Some(partnerId),
                routes.PartnerContactNameController.displayExistingPartner(partnerId)
    )

}
