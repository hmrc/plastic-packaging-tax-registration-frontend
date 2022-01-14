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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{RegistrationConnector, ServiceError}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.group.{routes => groupRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.group.MemberName
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.group.member_name_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContactDetailsNameController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: member_name_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(memberId: String): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      val contactDetails = request.registration.findMember(memberId).flatMap(_.contactDetails)
      val form = contactDetails match {
        case Some(contactDetails) =>
          MemberName.form().fill(MemberName(contactDetails.firstName, contactDetails.lastName))
        case _ => MemberName.form()
      }

      Ok(
        page(form,
             request.registration.findMember(memberId).map(_.businessName).getOrElse(
               "your organisation"
             ),
             groupRoutes.OrganisationListController.displayPage(),
             groupRoutes.ContactDetailsNameController.submit(memberId),
             memberId
        )
      )
    }

  def submit(memberId: String): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      MemberName.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[MemberName]) =>
            Future.successful(
              BadRequest(
                page(formWithErrors,
                     request.registration.findMember(memberId).map(_.businessName).getOrElse(
                       "your organisation"
                     ),
                     groupRoutes.OrganisationListController.displayPage(),
                     groupRoutes.ContactDetailsNameController.submit(memberId),
                     memberId
                )
              )
            ),
          memberName =>
            updateRegistration(memberName, memberId).map {
              case Right(_) =>
                Redirect(groupRoutes.ContactDetailsEmailAddressController.displayPage(memberId))
              case Left(error) => throw error
            }
        )
    }

  private def updateRegistration(formData: MemberName, memberId: String)(implicit
    req: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] =
    update { registration =>
      registration.copy(groupDetail =
        registration.groupDetail.map(
          _.withUpdatedOrNewMember(
            registration.findMember(memberId).map(
              _.withUpdatedGroupMemberName(formData.firstName, formData.lastName)
            ).getOrElse(throw new IllegalStateException("Expected group member absent"))
          )
        )
      )
    }

}
