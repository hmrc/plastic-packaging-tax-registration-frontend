/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthNoEnrolmentCheckAction
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact._
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{
  AmendmentJourneyAction,
  AuthenticatedRequest,
  JourneyRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.services.CountryService
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendContactDetailsController @Inject() (
  authenticate: AuthNoEnrolmentCheckAction,
  mcc: MessagesControllerComponents,
  amendmentJourneyAction: AmendmentJourneyAction,
  contactNamePage: full_name_page,
  jobTitlePage: job_title_page,
  emailPage: email_address_page,
  phoneNumberPage: phone_number_page,
  addressPage: address_page,
  countryService: CountryService
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  def contactName(): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction) { implicit request =>
      request.registration.primaryContactDetails.name match {
        case Some(name) =>
          Ok(buildContactNamePage(FullName.form().fill(FullName(name))))
        case _ =>
          Ok(buildContactNamePage(FullName.form()))
      }
    }

  def updateContactName(): Action[AnyContent] = {

    def updateContactName(updatedName: String): Registration => Registration = {
      registration: Registration =>
        registration.copy(primaryContactDetails =
          registration.primaryContactDetails.copy(name = Some(updatedName))
        )
    }

    (authenticate andThen amendmentJourneyAction).async { implicit request =>
      FullName.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[FullName]) =>
            Future.successful(BadRequest(buildContactNamePage(formWithErrors))),
          fullName => updateRegistration(updateContactName(fullName.value))
        )
    }
  }

  private def buildContactNamePage(
    form: Form[FullName]
  )(implicit request: JourneyRequest[AnyContent]) =
    contactNamePage(form,
                    routes.AmendRegistrationController.displayPage(),
                    routes.AmendContactDetailsController.updateContactName()
    )

  def jobTitle(): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction) { implicit request =>
      request.registration.primaryContactDetails.jobTitle match {
        case Some(jobTitle) =>
          Ok(buildJobTitlePage(JobTitle.form().fill(JobTitle(jobTitle))))
        case _ =>
          Ok(buildJobTitlePage(JobTitle.form()))
      }
    }

  def updateJobTitle(): Action[AnyContent] = {

    def updateJobTitle(updatedJobTitle: String): Registration => Registration = {
      registration: Registration =>
        registration.copy(primaryContactDetails =
          registration.primaryContactDetails.copy(jobTitle = Some(updatedJobTitle))
        )
    }

    (authenticate andThen amendmentJourneyAction).async { implicit request =>
      JobTitle.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[JobTitle]) =>
            Future.successful(BadRequest(buildJobTitlePage(formWithErrors))),
          jobTitle => updateRegistration(updateJobTitle(jobTitle.value))
        )
    }
  }

  private def buildJobTitlePage(
    form: Form[JobTitle]
  )(implicit request: JourneyRequest[AnyContent]) =
    jobTitlePage(form,
                 routes.AmendRegistrationController.displayPage(),
                 routes.AmendContactDetailsController.updateJobTitle()
    )

  def email(): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction) { implicit request =>
      request.registration.primaryContactDetails.email match {
        case Some(email) =>
          Ok(buildEmailPage(EmailAddress.form().fill(EmailAddress(email))))
        case _ =>
          Ok(buildEmailPage(EmailAddress.form()))
      }
    }

  def updateEmail(): Action[AnyContent] = {

    def updateEmail(updatedEmail: String): Registration => Registration = {
      registration: Registration =>
        registration.copy(primaryContactDetails =
          registration.primaryContactDetails.copy(email = Some(updatedEmail))
        )
    }

    (authenticate andThen amendmentJourneyAction).async { implicit request =>
      EmailAddress.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[EmailAddress]) =>
            Future.successful(BadRequest(buildEmailPage(formWithErrors))),
          email => updateRegistration(updateEmail(email.value))
        )
    }
  }

  private def buildEmailPage(
    form: Form[EmailAddress]
  )(implicit request: JourneyRequest[AnyContent]) =
    emailPage(form,
              routes.AmendRegistrationController.displayPage(),
              routes.AmendContactDetailsController.updateEmail()
    )

  // TODO: we need the full email verification flow in here

  def phoneNumber(): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction) { implicit request =>
      request.registration.primaryContactDetails.phoneNumber match {
        case Some(phoneNumber) =>
          Ok(buildPhoneNumberPage(PhoneNumber.form().fill(PhoneNumber(phoneNumber))))
        case _ =>
          Ok(buildPhoneNumberPage(PhoneNumber.form()))
      }
    }

  def updatePhoneNumber(): Action[AnyContent] = {

    def updatePhoneNumber(updatedPhoneNumber: String): Registration => Registration = {
      registration: Registration =>
        registration.copy(primaryContactDetails =
          registration.primaryContactDetails.copy(phoneNumber = Some(updatedPhoneNumber))
        )
    }

    (authenticate andThen amendmentJourneyAction).async { implicit request =>
      PhoneNumber.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[PhoneNumber]) =>
            Future.successful(BadRequest(buildPhoneNumberPage(formWithErrors))),
          phoneNumber => updateRegistration(updatePhoneNumber(phoneNumber.value))
        )
    }
  }

  private def buildPhoneNumberPage(
    form: Form[PhoneNumber]
  )(implicit request: JourneyRequest[AnyContent]) =
    phoneNumberPage(form,
                    routes.AmendRegistrationController.displayPage(),
                    routes.AmendContactDetailsController.updatePhoneNumber()
    )

  def address(): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction) { implicit request =>
      request.registration.primaryContactDetails.address match {
        case Some(address) =>
          Ok(buildAddressPage(Address.form().fill(address)))
        case _ =>
          Ok(buildAddressPage(Address.form()))
      }
    }

  def updateAddress(): Action[AnyContent] = {

    def updateAddress(updatedAddress: Address): Registration => Registration = {
      registration: Registration =>
        registration.copy(primaryContactDetails =
          registration.primaryContactDetails.copy(address = Some(updatedAddress))
        )
    }

    (authenticate andThen amendmentJourneyAction).async { implicit request =>
      Address.form()
        .bindFromRequest()
        .fold((formWithErrors: Form[Address]) =>
                Future.successful(BadRequest(buildAddressPage(formWithErrors))),
              address => updateRegistration(updateAddress(address))
        )
    }
  }

  private def buildAddressPage(form: Form[Address])(implicit request: JourneyRequest[AnyContent]) =
    addressPage(form,
                countryService.getAll(),
                routes.AmendRegistrationController.displayPage(),
                routes.AmendContactDetailsController.updateAddress()
    )

  private def updateRegistration(
    registrationAmendment: Registration => Registration
  )(implicit request: AuthenticatedRequest[Any], hc: HeaderCarrier) =
    amendmentJourneyAction.updateRegistration(registrationAmendment)
      .map(_ => Redirect(routes.AmendRegistrationController.displayPage()))
      .recover { case _ => Redirect(routes.AmendRegistrationController.registrationUpdateFailed()) }

}
