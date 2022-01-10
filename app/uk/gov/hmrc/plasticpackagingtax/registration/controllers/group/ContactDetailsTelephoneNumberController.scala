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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.PhoneNumber
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.GroupMemberContactDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  Cacheable,
  GroupDetail,
  Registration
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.group.member_phone_number_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContactDetailsTelephoneNumberController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: member_phone_number_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      request.registration.lastMember.map(_.contactDetails match {
        case Some(details) =>
          Ok(
            page(PhoneNumber.form().fill(PhoneNumber(details.phoneNumber.getOrElse(""))),
                 Some(details.groupMemberName),
                 groupRoutes.ContactDetailsEmailAddressController.displayPage(),
                 groupRoutes.ContactDetailsTelephoneNumberController.submit()
            )
          )
        case _ =>
          Ok(
            page(PhoneNumber.form(),
                 None,
                 groupRoutes.ContactDetailsEmailAddressController.displayPage(),
                 groupRoutes.ContactDetailsTelephoneNumberController.submit()
            )
          )
      }).get
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      PhoneNumber.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[PhoneNumber]) =>
            Future.successful(
              BadRequest(
                page(formWithErrors,
                     request.registration.lastMember.flatMap(
                       _.contactDetails.map(_.groupMemberName)
                     ),
                     groupRoutes.ContactDetailsEmailAddressController.displayPage(),
                     groupRoutes.ContactDetailsTelephoneNumberController.submit()
                )
              )
            ),
          phoneNumber =>
            updateRegistration(phoneNumber).map {
              case Right(_) =>
                Redirect(groupRoutes.ContactDetailsCheckAnswersController.displayPage())
              case Left(error) => throw error
            }
        )
    }

  private def updateRegistration(
    formData: PhoneNumber
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val contactDetails: Option[GroupMemberContactDetails] =
        registration.lastMember.map(_.withGroupMemberPhoneNumber(phoneNumber = formData.value))
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
