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

package controllers.amendment.group

import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import controllers.actions.EnrolledAuthAction
import controllers.group.ContactDetailsNameControllerBase
import models.registration.AmendRegistrationUpdateService
import models.request.AmendmentJourneyAction
import views.html.group.member_name_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AddGroupMemberContactDetailsNameController @Inject() (
                                                             authenticate: EnrolledAuthAction,
                                                             journeyAction: AmendmentJourneyAction,
                                                             mcc: MessagesControllerComponents,
                                                             page: member_name_page,
                                                             registrationUpdater: AmendRegistrationUpdateService
)(implicit ec: ExecutionContext)
    extends ContactDetailsNameControllerBase(authenticate, journeyAction, mcc, page, registrationUpdater) {

  def displayPage(memberId: String): Action[AnyContent] =
    doDisplayPage(memberId, getSubmitCall)

  def submit(memberId: String): Action[AnyContent] =
    doSubmit(memberId, getSubmitCall, getSuccessfulRedirect)

  protected def getSubmitCall(memberId: String): Call =
    routes.AddGroupMemberContactDetailsNameController.submit(memberId)

  protected def getSuccessfulRedirect(memberId: String): Call =
    routes.AddGroupMemberContactDetailsEmailAddressController.displayPage(memberId)

}
