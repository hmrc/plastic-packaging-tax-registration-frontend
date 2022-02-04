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

import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthNoEnrolmentCheckAction
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.group.ContactDetailsTelephoneNumberControllerBase
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.AmendRegistrationUpdateService
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.group.member_phone_number_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AddGroupMemberContactDetailsTelephoneNumberController @Inject() (
  authenticate: AuthNoEnrolmentCheckAction,
  journeyAction: AmendmentJourneyAction,
  mcc: MessagesControllerComponents,
  page: member_phone_number_page,
  registrationUpdater: AmendRegistrationUpdateService
)(implicit ec: ExecutionContext)
    extends ContactDetailsTelephoneNumberControllerBase(authenticate,
                                                        journeyAction,
                                                        mcc,
                                                        page,
                                                        registrationUpdater
    ) {

  def displayPage(memberId: String): Action[AnyContent] = doDisplayPage(memberId)

  def submit(memberId: String): Action[AnyContent] = doSubmit(memberId)

  override protected def getBackLink(memberId: String): Call =
    routes.AddGroupMemberContactDetailsEmailAddressController.displayPage(memberId)

  override protected def getSubmitCall(memberId: String): Call =
    routes.AddGroupMemberContactDetailsTelephoneNumberController.submit(memberId)

  override protected def getSuccessfulRedirect(memberId: String): Call =
    routes.AddGroupMemberContactDetailsConfirmAddressController.displayPage(memberId)

}
