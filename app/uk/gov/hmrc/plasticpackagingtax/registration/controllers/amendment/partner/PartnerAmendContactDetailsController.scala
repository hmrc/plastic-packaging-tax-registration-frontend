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

import play.api.data.Form
import play.api.mvc._
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthNoEnrolmentCheckAction
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.AmendmentController
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact._
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{
  AmendmentJourneyAction,
  JourneyRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact._

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class PartnerAmendContactDetailsController @Inject() (
  authenticate: AuthNoEnrolmentCheckAction,
  mcc: MessagesControllerComponents,
  amendmentJourneyAction: AmendmentJourneyAction,
  contactNamePage: full_name_page
)(implicit ec: ExecutionContext)
    extends AmendmentController(mcc, amendmentJourneyAction) {

  def contactName(partnerId: String): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction) { implicit request =>
      request.registration.findPartner(partnerId) match {
        case Some(partner) =>
          Ok(buildContactNamePage(FullName.form().fill(FullName(partner.name)), partnerId))
        case _ =>
          Ok(buildContactNamePage(FullName.form(), partnerId))
      }
    }

  def updateContactName(partnerId: String): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction) { implicit request =>
      // TODO: update the partner contact name on subscription
      Redirect(routes.PartnerContactDetailsCheckAnswersController.displayPage(partnerId))
    }

  private def buildContactNamePage(form: Form[FullName], partnerId: String)(implicit
    request: JourneyRequest[AnyContent]
  ) =
    contactNamePage(form,
                    routes.PartnerContactDetailsCheckAnswersController.displayPage(partnerId),
                    routes.PartnerAmendContactDetailsController.updateContactName(partnerId)
    )

  // TODO: support other contact details
}
