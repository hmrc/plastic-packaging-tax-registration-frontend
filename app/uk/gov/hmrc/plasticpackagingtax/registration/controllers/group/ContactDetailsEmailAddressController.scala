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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.EmailAddress
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.GroupMemberContactDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  Cacheable,
  GroupDetail,
  Registration
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.group.member_email_address_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContactDetailsEmailAddressController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: member_email_address_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      request.registration.lastMember.map(_.contactDetails match {
        case Some(details) =>
          Ok(
            page(EmailAddress.form().fill(EmailAddress(details.email.getOrElse(""))),
                 Some(details.groupMemberName),
                 groupRoutes.ContactDetailsNameController.displayPage(),
                 groupRoutes.ContactDetailsEmailAddressController.submit()
            )
          )
        case _ =>
          Ok(
            page(EmailAddress.form(),
                 None,
                 groupRoutes.ContactDetailsNameController.displayPage(),
                 groupRoutes.ContactDetailsEmailAddressController.submit()
            )
          )
      }).get
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      EmailAddress.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[EmailAddress]) =>
            Future.successful(
              BadRequest(
                page(formWithErrors,
                     request.registration.lastMember.flatMap(
                       _.contactDetails.map(_.groupMemberName)
                     ),
                     groupRoutes.ContactDetailsNameController.displayPage(),
                     groupRoutes.ContactDetailsEmailAddressController.submit()
                )
              )
            ),
          emailAddress =>
            updateRegistration(emailAddress).map {
              case Right(_) =>
                Redirect(groupRoutes.ContactDetailsTelephoneNumberController.displayPage())
              case Left(error) => throw error
            }
        )
    }

  private def updateRegistration(
    formData: EmailAddress
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val contactDetails: Option[GroupMemberContactDetails] =
        registration.lastMember.map(_.withGroupMemberEmail(email = formData.value))
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
