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

package controllers.partner

import controllers.EmailVerificationActions
import forms.contact.EmailAddress
import models.genericregistration.Partner
import models.registration.{Registration, RegistrationUpdater}
import models.request.JourneyRequest
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.partner.partner_email_address_page

import scala.concurrent.{ExecutionContext, Future}

abstract class PartnerEmailAddressControllerBase(
  val journeyAction: ActionBuilder[JourneyRequest, AnyContent],
  mcc: MessagesControllerComponents,
  page: partner_email_address_page,
  val registrationUpdater: RegistrationUpdater
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with EmailVerificationActions {

  protected def doDisplay(partnerId: Option[String], backCall: Call, submitCall: Call): Action[AnyContent] =
    journeyAction { implicit request =>
      getPartner(partnerId).map { partner =>
        val isNominated: Boolean = request.registration.isNominatedPartner(partnerId)

        renderPageFor(partner, isNominated, backCall, submitCall)
      }.getOrElse(throw new IllegalStateException("Expected partner missing"))
    }

  private def renderPageFor(partner: Partner, isNominated: Boolean, backCall: Call, submitCall: Call)(implicit
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
        Ok(page(form, submitCall, contactName, isNominated))
      }.getOrElse(throw new IllegalStateException("Expected partner contact name missing"))
    }.getOrElse(throw new IllegalStateException("Expected partner contact details missing"))

  def doSubmit(
    partnerId: Option[String],
    backCall: Call,
    submitCall: Call,
    onwardsCall: Call,
    emailVerificationContinueUrl: Call,
    confirmEmailAddressCall: Call
  ): Action[AnyContent] =
    journeyAction.async { implicit request =>
      def updateAction(emailAddress: EmailAddress): Future[Registration] =
        updateEmailAddress(partnerId, emailAddress)

      getPartner(partnerId).map { partner =>
        val isNominated: Boolean = request.registration.isNominatedPartner(partnerId)

        handleSubmission(
          partner,
          isNominated,
          backCall,
          submitCall,
          onwardsCall,
          updateAction,
          emailVerificationContinueUrl,
          confirmEmailAddressCall
        )
      }.getOrElse(Future.successful(throw new IllegalStateException("Expected existing partner missing")))
    }

  private def handleSubmission(
    partner: Partner,
    isNominated: Boolean,
    backCall: Call,
    submitCall: Call,
    onwardCall: Call,
    updateAction: EmailAddress => Future[Registration],
    emailVerificationContinueUrl: Call,
    confirmEmailAddressCall: Call
  )(implicit request: JourneyRequest[AnyContent]): Future[Result] =
    EmailAddress.form()
      .bindFromRequest()
      .fold(
        (formWithErrors: Form[EmailAddress]) =>
          partner.contactDetails.flatMap(_.name).map { contactName =>
            Future.successful(BadRequest(page(formWithErrors, submitCall, contactName, isNominated)))
          }.getOrElse(Future.successful(throw new IllegalStateException("Expected partner contact name missing"))),
        emailAddress =>
          doesPartnerEmailRequireVerification(partner, emailAddress).flatMap { isEmailVerificationRequired =>
            if (!isEmailVerificationRequired)
              updateAction(emailAddress).map(_ => Redirect(onwardCall))
            else
              promptForEmailVerificationCode(
                request,
                emailAddress,
                emailVerificationContinueUrl,
                confirmEmailAddressCall
              )
          }
      )

  private def updateEmailAddress(existingPartnerId: Option[String], emailAddress: EmailAddress)(implicit
    req: JourneyRequest[AnyContent]
  ): Future[Registration] =
    registrationUpdater.updateRegistration { registration =>
      updateRegistrationWithPartnerEmail(registration, existingPartnerId, emailAddress.value)
    }

  protected def updateRegistrationWithPartnerEmail(
    registration: Registration,
    existingPartnerId: Option[String],
    emailAddress: String
  ): Registration =
    existingPartnerId.map { partnerId =>
      registration.withUpdatedPartner(
        partnerId,
        partner => partner.copy(contactDetails = partner.contactDetails.map(_.copy(emailAddress = Some(emailAddress))))
      )
    }.getOrElse {
      registration.inflightPartner.map { partner =>
        val withEmailAddress =
          partner.contactDetails.map { contactDetails =>
            val updatedContactDetailsWithEmailAddress =
              contactDetails.copy(emailAddress = Some(emailAddress))
            partner.copy(contactDetails = Some(updatedContactDetailsWithEmailAddress))
          }
        registration.withInflightPartner(withEmailAddress)
      }.getOrElse(registration)
    }

  protected def getPartner(partnerId: Option[String])(implicit request: JourneyRequest[_]): Option[Partner] =
    partnerId match {
      case Some(partnerId) => request.registration.findPartner(partnerId)
      case _               => request.registration.inflightPartner
    }

}
