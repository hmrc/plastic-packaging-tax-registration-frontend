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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.contact

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact.email_address_passcode_confirmation_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import play.api.i18n.Messages

import javax.inject.Inject

class ContactDetailsEmailAddressPasscodeConfirmationController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  mcc: MessagesControllerComponents,
  page: email_address_passcode_confirmation_page
) extends FrontendController(mcc) with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      Ok(
        page(routes.ContactDetailsEmailAddressPasscodeController.displayPage(),
             routes.ContactDetailsEmailAddressPasscodeConfirmationController.submit(),
             Some(sectionName())
        )
      )
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction) { _ =>
      // TODO does this do anything?
      Redirect(routes.ContactDetailsTelephoneNumberController.displayPage())
    }

  private def sectionName()(implicit messages: Messages): String =
    messages("primaryContactDetails.sectionHeader")

}
