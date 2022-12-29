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

package controllers.partner

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import controllers.actions.{
  AuthActioning,
  FormAction,
  SaveAndContinue
}
import forms.contact.PhoneNumber
import models.genericregistration.Partner
import models.registration.{
  Registration,
  RegistrationUpdater
}
import models.request.{
  AuthenticatedRequest,
  JourneyRequest
}
import views.html.partner.partner_phone_number_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

abstract class PartnerPhoneNumberControllerBase(
  val authenticate: AuthActioning,
  val journeyAction: ActionRefiner[AuthenticatedRequest, JourneyRequest],
  mcc: MessagesControllerComponents,
  page: partner_phone_number_page,
  registrationUpdater: RegistrationUpdater
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  protected def doDisplay(
    partnerId: Option[String],
    backCall: Call,
    submitCall: Call
  ): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      getPartner(partnerId).map { partner =>
        val sectionHeading = request.registration.isNominatedPartner(partnerId)
        renderPageFor(partner, backCall, submitCall, sectionHeading)
      }.getOrElse(throw new IllegalStateException("Expected partner missing"))
    }

  private def renderPageFor(partner: Partner, backCall: Call, submitCall: Call, sectionHeading: Boolean)(implicit
    request: JourneyRequest[AnyContent]
  ): Result =
    partner.contactDetails.map {
      contactDetails =>
        contactDetails.name.map { contactName =>
          val form = contactDetails.phoneNumber match {
            case Some(data) =>
              PhoneNumber.form().fill(PhoneNumber(data))
            case _ =>
              PhoneNumber.form()
          }
          Ok(page(form, submitCall, contactName, sectionHeading))
        }.getOrElse(throw new IllegalStateException("Expected partner contact details missing"))
    }.getOrElse(throw new IllegalStateException("Expected partner contact name missing"))

  def doSubmit(
    partnerId: Option[String],
    backCall: Call,
    submitCall: Call,
    onwardsCall: Call,
    dropoutCall: Call
  ): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      def updateAction(phoneNumber: PhoneNumber): Future[Registration] =
        partnerId match {
          case Some(partnerId) => updateExistingPartner(phoneNumber, partnerId)
          case _               => updateInflightPartner(phoneNumber)
        }
      getPartner(partnerId).map { partner =>
        val sectionHeading = request.registration.isNominatedPartner(partnerId)
        handleSubmission(partner, backCall, submitCall, onwardsCall, dropoutCall, sectionHeading,updateAction)
      }.getOrElse(
        Future.failed(throw new IllegalStateException("Expected existing partner missing"))
      )
    }

  private def handleSubmission(
    partner: Partner,
    backCall: Call,
    submitCall: Call,
    onwardsCall: Call,
    dropoutCall: Call,
    sectionHeading: Boolean,
    updateAction: PhoneNumber => Future[Registration]
  )(implicit request: JourneyRequest[AnyContent]): Future[Result] =
    partner.contactDetails.flatMap(_.name).map { contactName =>
      PhoneNumber.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[PhoneNumber]) =>
            Future.successful(BadRequest(page(formWithErrors, submitCall, contactName,sectionHeading))),
          phoneNumber =>
            updateAction(phoneNumber).map { _ =>
              FormAction.bindFromRequest match {
                case SaveAndContinue =>
                  Redirect(onwardsCall)
                case _ =>
                  Redirect(dropoutCall)
              }
            }
        )
    }.getOrElse(Future.successful(throw new IllegalStateException("Expected partner name missing")))

  private def updateInflightPartner(
    formData: PhoneNumber
  )(implicit req: JourneyRequest[AnyContent]): Future[Registration] =
    registrationUpdater.updateRegistration { registration =>
      registration.inflightPartner.map { partner =>
        val withPhoneNumber =
          partner.contactDetails.map { contactDetails =>
            val updatedContactDetailsWithPhoneNumber =
              contactDetails.copy(phoneNumber = Some(formData.value))
            partner.copy(contactDetails = Some(updatedContactDetailsWithPhoneNumber))
          }
        registration.withInflightPartner(withPhoneNumber)
      }.getOrElse {
        registration
      }
    }

  private def updateExistingPartner(formData: PhoneNumber, partnerId: String)(implicit
    req: JourneyRequest[AnyContent]
  ): Future[Registration] =
    registrationUpdater.updateRegistration { registration =>
      registration.withUpdatedPartner(
        partnerId,
        partner =>
          partner.copy(contactDetails =
            partner.contactDetails.map(_.copy(phoneNumber = Some(formData.value)))
          )
      )
    }

  private def getPartner(
    partnerId: Option[String]
  )(implicit request: JourneyRequest[_]): Option[Partner] =
    partnerId match {
      case Some(partnerId) => request.registration.findPartner(partnerId)
      case _               => request.registration.inflightPartner
    }

}
