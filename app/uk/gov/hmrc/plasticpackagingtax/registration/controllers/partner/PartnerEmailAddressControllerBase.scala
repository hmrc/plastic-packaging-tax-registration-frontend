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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{
  AuthActioning,
  FormAction,
  SaveAndContinue
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.EmailAddress
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.Partner
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  Registration,
  RegistrationUpdater
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{
  AuthenticatedRequest,
  JourneyRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partner.partner_email_address_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

abstract class PartnerEmailAddressControllerBase(
  val authenticate: AuthActioning,
  val journeyAction: ActionRefiner[AuthenticatedRequest, JourneyRequest],
  mcc: MessagesControllerComponents,
  page: partner_email_address_page,
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
        renderPageFor(partner, backCall, submitCall)
      }.getOrElse(throw new IllegalStateException("Expected partner missing"))
    }

  private def renderPageFor(partner: Partner, backCall: Call, submitCall: Call)(implicit
    request: JourneyRequest[AnyContent]
  ): Result =
    partner.contactDetails.map { contactDetails =>
      contactDetails.name.map { contactName =>
        val form = contactDetails.emailAddress match {
          case Some(data) =>
            EmailAddress.form().fill(EmailAddress(data))
          case _ =>
            EmailAddress.form()
        }
        Ok(page(form, backCall, submitCall, contactName))
      }.getOrElse(throw new IllegalStateException("Expected partner contact name missing"))
    }.getOrElse(throw new IllegalStateException("Expected partner contact details missing"))

  def doSubmit(
    partnerId: Option[String],
    backCall: Call,
    submitCall: Call,
    onwardsCall: Call,
    dropoutCall: Call
  ): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      def updateAction(emailAddress: EmailAddress): Future[Registration] =
        partnerId match {
          case Some(partnerId) => updateExistingPartner(emailAddress, partnerId)
          case _               => updateInflightPartner(emailAddress)
        }
      getPartner(partnerId).map { partner =>
        handleSubmission(partner, backCall, submitCall, onwardsCall, dropoutCall, updateAction)
      }.getOrElse(
        Future.successful(throw new IllegalStateException("Expected existing partner missing"))
      )
    }

  private def handleSubmission(
    partner: Partner,
    backCall: Call,
    submitCall: Call,
    onwardCall: Call,
    dropoutCall: Call,
    updateAction: EmailAddress => Future[Registration]
  )(implicit request: JourneyRequest[AnyContent]): Future[Result] =
    EmailAddress.form()
      .bindFromRequest()
      .fold(
        (formWithErrors: Form[EmailAddress]) =>
          partner.contactDetails.flatMap(_.name).map {
            contactName =>
              Future.successful(BadRequest(page(formWithErrors, backCall, submitCall, contactName)))
          }.getOrElse(
            Future.successful(
              throw new IllegalStateException("Expected partner contact name missing")
            )
          ),
        emailAddress =>
          updateAction(emailAddress).map { _ =>
            FormAction.bindFromRequest match {
              case SaveAndContinue =>
                Redirect(onwardCall)
              case _ =>
                Redirect(dropoutCall)
            }
          }
      )

  private def updateInflightPartner(
    formData: EmailAddress
  )(implicit req: JourneyRequest[AnyContent]): Future[Registration] =
    registrationUpdater.updateRegistration { registration =>
      registration.inflightPartner.map { partner =>
        val withEmailAddress =
          partner.contactDetails.map { contactDetails =>
            val updatedContactDetailsWithEmailAddress =
              contactDetails.copy(emailAddress = Some(formData.value))
            partner.copy(contactDetails = Some(updatedContactDetailsWithEmailAddress))
          }
        registration.withInflightPartner(withEmailAddress)
      }.getOrElse {
        registration
      }
    }

  private def updateExistingPartner(formData: EmailAddress, partnerId: String)(implicit
    req: JourneyRequest[AnyContent]
  ): Future[Registration] =
    registrationUpdater.updateRegistration { registration =>
      registration.withUpdatedPartner(
        partnerId,
        partner =>
          partner.copy(contactDetails =
            partner.contactDetails.map(_.copy(emailAddress = Some(formData.value)))
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