/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.amendment

import play.api.data.Form
import play.api.mvc._
import controllers.actions.EnrolledAuthAction
import forms.contact._
import models.registration.Registration
import models.request.{
  AmendmentJourneyAction,
  JourneyRequest
}
import services.{
  AddressCaptureConfig,
  AddressCaptureService
}
import views.html.contact._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendContactDetailsController @Inject() (
                                                authenticate: EnrolledAuthAction,
                                                mcc: MessagesControllerComponents,
                                                amendmentJourneyAction: AmendmentJourneyAction,
                                                contactNamePage: full_name_page,
                                                jobTitlePage: job_title_page,
                                                phoneNumberPage: phone_number_page,
                                                addressCaptureService: AddressCaptureService
)(implicit ec: ExecutionContext)
    extends AmendmentController(mcc, amendmentJourneyAction) {

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
                 routes.AmendContactDetailsController.updateJobTitle()
    )

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
                    routes.AmendContactDetailsController.updatePhoneNumber()
    )

  def address(): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction).async { implicit request =>
      addressCaptureService.initAddressCapture(
        AddressCaptureConfig(backLink = routes.AmendRegistrationController.displayPage().url,
                             successLink = routes.AmendContactDetailsController.updateAddress().url,
                             alfHeadingsPrefix = "addressLookup.contact",
                             entityName = request.registration.organisationDetails.businessName,
                             pptHeadingKey = "addressCapture.contact.heading",
                             pptHintKey = None,
                             forceUkAddress = false
        )
      ).map(redirect => Redirect(redirect))
    }

  def updateAddress(): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction).async { implicit request =>
      addressCaptureService.getCapturedAddress().flatMap {
        capturedAddress =>
          updateRegistration { registration =>
            registration.copy(primaryContactDetails =
              registration.primaryContactDetails.copy(address = capturedAddress)
            )
          }
      }
    }

}