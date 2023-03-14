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

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import controllers.actions.AuthActioning
import forms.contact.PhoneNumber
import models.registration.{
  Registration,
  RegistrationUpdater
}
import models.request.{
  AuthenticatedRequest,
  JourneyRequest
}
import views.html.group.member_phone_number_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

abstract class ContactDetailsTelephoneNumberControllerBase(
  val journeyAction: ActionBuilder[JourneyRequest, AnyContent],
  mcc: MessagesControllerComponents,
  page: member_phone_number_page,
  registrationUpdater: RegistrationUpdater
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with ContactDetailsControllerBase with I18nSupport {

  protected def doDisplayPage(memberId: String): Action[AnyContent] =
    journeyAction { implicit request =>
      val contactDetails = request.registration.findMember(memberId).flatMap(_.contactDetails)
      val phoneNumber    = contactDetails.flatMap(_.phoneNumber)
      val memberName     = contactDetails.map(_.groupMemberName)
      val form = phoneNumber match {
        case Some(phoneNumber) => PhoneNumber.form().fill(PhoneNumber(phoneNumber))
        case _                 => PhoneNumber.form()
      }

      Ok(page(form, memberName, getSubmitCall(memberId)))
    }

  protected def doSubmit(memberId: String): Action[AnyContent] =
    journeyAction.async { implicit request =>
      PhoneNumber.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[PhoneNumber]) =>
            Future.successful(
              BadRequest(
                page(formWithErrors,
                     request.registration.findMember(memberId).flatMap(
                       _.contactDetails.map(_.groupMemberName)
                     ),
                     getSubmitCall(memberId)
                )
              )
            ),
          phoneNumber =>
            updateRegistration(phoneNumber, memberId).map { _ =>
              Redirect(getSuccessfulRedirect(memberId))
            }
        )
    }

  private def updateRegistration(phoneNumber: PhoneNumber, memberId: String)(implicit
    req: JourneyRequest[AnyContent]
  ): Future[Registration] =
    registrationUpdater.updateRegistration { registration =>
      registration.copy(groupDetail =
        registration.groupDetail.map(
          _.withUpdatedOrNewMember(
            registration.findMember(memberId).map(
              _.withUpdatedGroupMemberPhoneNumber(phoneNumber.value)
            ).getOrElse(throw new IllegalStateException("Expected group member absent"))
          )
        )
      )
    }

}
