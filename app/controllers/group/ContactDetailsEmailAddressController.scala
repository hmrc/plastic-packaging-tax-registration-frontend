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

package controllers.group

import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import controllers.actions.JourneyAction
import controllers.actions.getRegistration.GetRegistrationAction
import models.registration.NewRegistrationUpdateService
import views.html.group.member_email_address_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ContactDetailsEmailAddressController @Inject() (
                                                       journeyAction: JourneyAction,
                                                       mcc: MessagesControllerComponents,
                                                       page: member_email_address_page,
                                                       registrationUpdater: NewRegistrationUpdateService
)(implicit ec: ExecutionContext)
    extends ContactDetailsEmailAddressControllerBase(
                                                     journeyAction.register,
                                                     mcc,
                                                     page,
                                                     registrationUpdater
    ) {

  def displayPage(memberId: String): Action[AnyContent] = doDisplayPage(memberId)

  def submit(memberId: String): Action[AnyContent] = doSubmit(memberId)

  override protected def getSubmitCall(memberId: String): Call =
    routes.ContactDetailsEmailAddressController.submit(memberId)

  override protected def getSuccessfulRedirect(memberId: String): Call =
    routes.ContactDetailsTelephoneNumberController.displayPage(memberId)

}
