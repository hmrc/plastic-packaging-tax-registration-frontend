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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.group

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthActioning
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.EmailAddress
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  Registration,
  RegistrationUpdater
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{
  AuthenticatedRequest,
  JourneyRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.group.member_email_address_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

abstract class ContactDetailsEmailAddressControllerBase(
  val authenticate: AuthActioning,
  val journeyAction: ActionRefiner[AuthenticatedRequest, JourneyRequest],
  mcc: MessagesControllerComponents,
  page: member_email_address_page,
  registrationUpdater: RegistrationUpdater
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  protected def doDisplayPage(memberId: String): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      val contactDetails = request.registration.findMember(memberId).flatMap(_.contactDetails)
      val emailAddress   = contactDetails.flatMap(_.email)
      val memberName     = contactDetails.map(_.groupMemberName)
      val form = emailAddress match {
        case Some(emailAddress) => EmailAddress.form().fill(EmailAddress(emailAddress))
        case _                  => EmailAddress.form()
      }

      Ok(page(form, memberName, getBackLink(memberId), getSubmitCall(memberId)))
    }

  protected def doSubmit(memberId: String): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      EmailAddress.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[EmailAddress]) =>
            Future.successful(
              BadRequest(
                page(formWithErrors,
                     request.registration.findMember(memberId).flatMap(
                       _.contactDetails.map(_.groupMemberName)
                     ),
                     getBackLink(memberId),
                     getSubmitCall(memberId)
                )
              )
            ),
          emailAddress =>
            updateRegistration(emailAddress, memberId).map { _ =>
              Redirect(getSuccessfulRedirect(memberId))
            }
        )
    }

  protected def getBackLink(memberId: String): Call
  protected def getSubmitCall(memberId: String): Call
  protected def getSuccessfulRedirect(memberId: String): Call

  private def updateRegistration(emailAddress: EmailAddress, memberId: String)(implicit
    req: JourneyRequest[AnyContent]
  ): Future[Registration] =
    registrationUpdater.updateRegistration { registration =>
      registration.copy(groupDetail =
        registration.groupDetail.map(
          _.withUpdatedOrNewMember(
            registration.findMember(memberId).map(
              _.withUpdatedGroupMemberEmail(emailAddress.value)
            ).getOrElse(throw new IllegalStateException("Expected group member absent"))
          )
        )
      )
    }

}
