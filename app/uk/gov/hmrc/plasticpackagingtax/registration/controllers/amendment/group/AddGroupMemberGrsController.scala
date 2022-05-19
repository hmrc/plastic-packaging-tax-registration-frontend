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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.group

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors._
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs._
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.EnrolledAuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.group.GroupMemberGrsControllerBase
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.AmendRegistrationUpdateService
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.utils.AddressConversionUtils

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AddGroupMemberGrsController @Inject() (
                                              authenticate: EnrolledAuthAction,
                                              journeyAction: AmendmentJourneyAction,
                                              ukCompanyGrsConnector: UkCompanyGrsConnector,
                                              subscriptionsConnector: SubscriptionsConnector,
                                              partnershipGrsConnector: PartnershipGrsConnector,
                                              registrationUpdater: AmendRegistrationUpdateService,
                                              addressConversionUtils: AddressConversionUtils,
                                              mcc: MessagesControllerComponents
)(implicit val executionContext: ExecutionContext)
    extends GroupMemberGrsControllerBase(
      authenticate,
      journeyAction,
      ukCompanyGrsConnector,
      subscriptionsConnector,
      partnershipGrsConnector,
      registrationUpdater,
      addressConversionUtils,
      mcc
    ) {

  def grsCallbackNewMember(journeyId: String): Action[AnyContent] =
    grsCallback(journeyId, None, nameCaptureRedirect)

  private def nameCaptureRedirect(memberId: String) =
    routes.AddGroupMemberContactDetailsNameController.displayPage(memberId)

}
