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

package controllers.amendment

import play.api.i18n.I18nSupport
import play.api.mvc.{Call, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import models.registration.Registration
import models.request.JourneyRequest
import models.subscriptions.{SubscriptionCreateOrUpdateResponseFailure, SubscriptionCreateOrUpdateResponseSuccess}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import controllers.amendment.group.{routes => amendGroupRoutes}
import services.AmendRegistrationService

import scala.concurrent.ExecutionContext

abstract class AmendmentController(
  mcc: MessagesControllerComponents,
  service: AmendRegistrationService
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  protected def updateRegistration(
    registrationAmendment: Registration => Registration,
    successfulRedirect: Call = routes.AmendRegistrationController.displayPage()
  )(implicit request: JourneyRequest[_], hc: HeaderCarrier) =
    service.updateSubscriptionWithRegistration(registrationAmendment).map {
      case _: SubscriptionCreateOrUpdateResponseSuccess =>
        Redirect(successfulRedirect)
      case _: SubscriptionCreateOrUpdateResponseFailure =>
        Redirect(routes.AmendRegistrationController.registrationUpdateFailed())
    }

  protected def updateGroupMemberRegistration(
    registrationAmendment: Registration => Registration,
    memberId: String
  )(implicit request: JourneyRequest[_], hc: HeaderCarrier) =
    service.updateSubscriptionWithRegistration(registrationAmendment)
      .map(
        _ => Redirect(amendGroupRoutes.ContactDetailsCheckAnswersController.displayPage(memberId))
      )
      .recover {
        case _ => Redirect(routes.AmendRegistrationController.registrationUpdateFailed())
      }

}
