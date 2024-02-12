/*
 * Copyright 2024 HM Revenue & Customs
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

import controllers.actions.JourneyAction
import models.registration.NewRegistrationUpdateService
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import views.html.group.member_phone_number_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ContactDetailsTelephoneNumberController @Inject() (
                                                          journeyAction: JourneyAction,
                                                          mcc: MessagesControllerComponents,
                                                          page: member_phone_number_page,
                                                          registrationUpdater: NewRegistrationUpdateService
)(implicit ec: ExecutionContext)
    extends ContactDetailsTelephoneNumberControllerBase(journeyAction.register,
                                                        mcc,
                                                        page,
                                                        registrationUpdater
    ) {

  def displayPage(memberId: String): Action[AnyContent] = doDisplayPage(memberId)

  def submit(memberId: String): Action[AnyContent] = doSubmit(memberId)

  override protected def getSubmitCall(memberId: String): Call =
    routes.ContactDetailsTelephoneNumberController.submit(memberId)

  override protected def getSuccessfulRedirect(memberId: String): Call =
    routes.ContactDetailsConfirmAddressController.displayPage(memberId)

}
