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
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.GroupMemberContactDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  Cacheable,
  GroupDetail,
  Registration
}
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

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      request.registration.lastMember.map(_.contactDetails match {
        case Some(details) =>
          Ok(
            page(MemberName.form().fill(MemberName(details.firstName, details.lastName)),
                 request.registration.lastMember.map(_.businessName).getOrElse("your organisation"),
                 groupRoutes.OrganisationListController.displayPage(),
                 groupRoutes.ContactDetailsNameController.submit()
            )
          )
        case _ =>
          Ok(
            page(MemberName.form(),
                 request.registration.lastMember.map(_.businessName).getOrElse("your organisation"),
                 groupRoutes.OrganisationListController.displayPage(),
                 groupRoutes.ContactDetailsNameController.submit()
            )
          )
      }).get
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      MemberName.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[MemberName]) =>
            Future.successful(
              BadRequest(
                page(formWithErrors,
                     request.registration.lastMember.map(_.businessName).getOrElse(
                       "your organisation"
                     ),
                     groupRoutes.OrganisationListController.displayPage(),
                     groupRoutes.ContactDetailsNameController.submit()
                )
              )
            ),
          memberName =>
            updateRegistration(memberName).map {
              case Right(_) =>
                Redirect(groupRoutes.ContactDetailsEmailAddressController.displayPage())
              case Left(error) => throw error
            }
        )
    }

  private def updateRegistration(
    formData: MemberName
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val contactDetails: Option[GroupMemberContactDetails] =
        registration.lastMember.map(
          _.withGroupMemberName(firstName = formData.firstName, lastName = formData.lastName)
        )
      val detail: GroupDetail =
        registration.groupDetail.getOrElse(throw new IllegalStateException("No group detail"))
      registration.copy(groupDetail =
        Some(
          detail.copy(members =
            detail.updateMember(
              registration.lastMember.map(_.copy(contactDetails = contactDetails)).get
            )
          )
        )
      )
    }

}
