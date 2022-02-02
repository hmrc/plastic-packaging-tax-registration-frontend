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

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.RegistrationConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthNoEnrolmentCheckAction
import uk.gov.hmrc.plasticpackagingtax.registration.forms.group.RemoveMember
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Cacheable
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.amendment.group.confirm_remove_member_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}

@Singleton
class ConfirmRemoveMemberController @Inject() (
  authenticate: AuthNoEnrolmentCheckAction,
  amendmentJourneyAction: AmendmentJourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: confirm_remove_member_page
) extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(memberId: String): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction) { implicit request =>
      request.registration.findMember(memberId).map { member =>
        Ok(page(member, RemoveMember.form()))
      }.getOrElse {
        throw new IllegalStateException("Could not find member")
      }
    }

  def submit(memberId: String): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction) { implicit request =>
      // TODO: handle form submission and remove member
      request.registration.findMember(memberId).map { member =>
        Ok(page(member, RemoveMember.form()))
      }.getOrElse {
        throw new IllegalStateException("Could not find member")
      }
    }

}
