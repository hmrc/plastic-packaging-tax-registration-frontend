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

import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.SubscriptionsConnector
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs.{
  PartnershipGrsConnector,
  RegisteredSocietyGrsConnector,
  SoleTraderGrsConnector,
  UkCompanyGrsConnector
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthNoEnrolmentCheckAction
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner.PartnerGrsControllerBase
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.AmendRegistrationUpdateService
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AddPartnerGrsController @Inject() (
  authenticate: AuthNoEnrolmentCheckAction,
  journeyAction: AmendmentJourneyAction,
  ukCompanyGrsConnector: UkCompanyGrsConnector,
  soleTraderGrsConnector: SoleTraderGrsConnector,
  partnershipGrsConnector: PartnershipGrsConnector,
  registeredSocietyGrsConnector: RegisteredSocietyGrsConnector,
  registrationUpdater: AmendRegistrationUpdateService,
  subscriptionsConnector: SubscriptionsConnector,
  mcc: MessagesControllerComponents
)(implicit val executionContext: ExecutionContext)
    extends PartnerGrsControllerBase(authenticate = authenticate,
                                     journeyAction = journeyAction,
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
