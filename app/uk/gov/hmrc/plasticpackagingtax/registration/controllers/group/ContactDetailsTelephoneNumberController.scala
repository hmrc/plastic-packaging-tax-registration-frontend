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
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.AddressLookupIntegration
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.group.{routes => groupRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.PhoneNumber
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
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
    extends FrontendController(mcc) with Cacheable with I18nSupport with AddressLookupIntegration {

  def displayPage(memberId: String): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      val contactDetails = request.registration.findMember(memberId).flatMap(_.contactDetails)
      val phoneNumber    = contactDetails.flatMap(_.phoneNumber)
      val memberName     = contactDetails.map(_.groupMemberName)
      val form = phoneNumber match {
        case Some(phoneNumber) => PhoneNumber.form().fill(PhoneNumber(phoneNumber))
        case _                 => PhoneNumber.form()
      }

      Ok(
        page(form,
             memberName,
             groupRoutes.ContactDetailsEmailAddressController.displayPage(memberId),
             groupRoutes.ContactDetailsTelephoneNumberController.submit(memberId)
        )
      )
    }

  def submit(memberId: String): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
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
                     groupRoutes.ContactDetailsEmailAddressController.displayPage(memberId),
                     groupRoutes.ContactDetailsTelephoneNumberController.submit(memberId)
                )
              )
            ),
          phoneNumber =>
            updateRegistration(phoneNumber, memberId).map {
              case Right(_) =>
                Redirect(groupRoutes.ContactDetailsConfirmAddressController.displayPage(memberId))
              case Left(error) => throw error
            }
        )
    }

  private def updateRegistration(phoneNumber: PhoneNumber, memberId: String)(implicit
    req: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] =
    update { registration =>
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
