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

import forms.group.MemberName
import models.registration.OrganisationName.getMissingOrgMessage
import models.registration.{Registration, RegistrationUpdater}
import models.request.JourneyRequest
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.group.member_name_page

import scala.concurrent.{ExecutionContext, Future}

abstract class ContactDetailsNameControllerBase(
  val journeyAction: ActionBuilder[JourneyRequest, AnyContent],
  mcc: MessagesControllerComponents,
  page: member_name_page,
  registrationUpdater: RegistrationUpdater
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with ContactDetailsControllerBase with I18nSupport {

  protected def doDisplayPage(memberId: String, getSubmitCall: String => Call): Action[AnyContent] =
    journeyAction { implicit request =>
      val contactDetails = request.registration.findMember(memberId).flatMap(_.contactDetails)
      val form = contactDetails match {
        case Some(contactDetails) =>
          MemberName.form().fill(MemberName(contactDetails.firstName, contactDetails.lastName))
        case _ => MemberName.form()
      }

      Ok(page(form, request.registration.findMember(memberId).map(_.businessName).getOrElse(getMissingOrgMessage), getSubmitCall(memberId), memberId))
    }

  protected def doSubmit(memberId: String, getSubmitCall: String => Call, getSuccessfulRedirect: String => Call): Action[AnyContent] =
    journeyAction.async { implicit request =>
      MemberName.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[MemberName]) =>
            Future.successful(
              BadRequest(page(formWithErrors, request.registration.findMember(memberId).map(_.businessName).getOrElse(getMissingOrgMessage), getSubmitCall(memberId), memberId))
            ),
          memberName =>
            updateRegistration(memberName, memberId).map { _ =>
              Redirect(getSuccessfulRedirect(memberId))
            }
        )
    }

  private def updateRegistration(formData: MemberName, memberId: String)(implicit req: JourneyRequest[AnyContent]): Future[Registration] =
    registrationUpdater.updateRegistration { registration =>
      registration.copy(groupDetail =
        registration.groupDetail.map(
          _.withUpdatedOrNewMember(
            registration.findMember(memberId).map(_.withUpdatedGroupMemberName(formData.firstName, formData.lastName)).getOrElse(
              throw new IllegalStateException("Expected group member absent")
            )
          )
        )
      )
    }

}
