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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.partner

import play.api.data.Form
import play.api.mvc._
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthNoEnrolmentCheckAction
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.{
  AmendmentController,
  routes => amendmentRoutes
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.{
  EmailAddress,
  JobTitle,
  PhoneNumber
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.group.MemberName
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.Partner
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{
  AmendmentJourneyAction,
  JourneyRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.services.{
  AddressCaptureConfig,
  AddressCaptureService
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partner.{
  partner_email_address_page,
  partner_job_title_page,
  partner_member_name_page,
  partner_phone_number_page
}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendPartnerContactDetailsController @Inject() (
  authenticate: AuthNoEnrolmentCheckAction,
  mcc: MessagesControllerComponents,
  amendmentJourneyAction: AmendmentJourneyAction,
  contactNamePage: partner_member_name_page,
  contactEmailPage: partner_email_address_page,
  contactPhoneNumberPage: partner_phone_number_page,
  jobTitlePage: partner_job_title_page,
  addressCaptureService: AddressCaptureService
)(implicit ec: ExecutionContext)
    extends AmendmentController(mcc, amendmentJourneyAction) {

  def contactName(partnerId: String): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction) { implicit request =>
      val partner = getPartner(partnerId)
      val form = MemberName.form().fill(
        MemberName(
          firstName = partner.contactDetails.flatMap(_.firstName).getOrElse(
            throw new IllegalStateException("Partner first name absent")
          ),
          lastName = partner.contactDetails.flatMap(_.lastName).getOrElse(
            throw new IllegalStateException("Partner last name absent")
          )
        )
      )

      Ok(buildContactNamePage(form, partner, isNominated(partnerId)))
    }

  def updateContactName(partnerId: String): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction).async { implicit request =>
      MemberName.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[MemberName]) =>
            Future.successful(
              BadRequest(
                buildContactNamePage(formWithErrors, getPartner(partnerId), isNominated(partnerId))
              )
            ),
          partnerName =>
            updateRegistration(
              { registration: Registration =>
                registration.withUpdatedPartner(partnerId,
                                                partner =>
                                                  partner.copy(contactDetails =
                                                    partner.contactDetails.map(
                                                      _.copy(firstName =
                                                               Some(partnerName.firstName),
                                                             lastName = Some(partnerName.lastName)
                                                      )
                                                    )
                                                  )
                )
              },
              successfulRedirect(partnerId)
            )
        )
    }

  private def buildContactNamePage(form: Form[MemberName], partner: Partner, isNominated: Boolean)(
    implicit request: Request[_]
  ) =
    contactNamePage(form = form,
                    partnershipName = partner.name,
                    backLink =
                      if (isNominated) amendmentRoutes.AmendRegistrationController.displayPage()
                      else
                        routes.PartnerContactDetailsCheckAnswersController.displayPage(partner.id),
                    updateCall =
                      routes.AmendPartnerContactDetailsController.updateContactName(partner.id)
    )

  def emailAddress(partnerId: String): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction) { implicit request =>
      val form = EmailAddress.form().fill(
        EmailAddress(
          getPartner(partnerId).contactDetails.flatMap(_.emailAddress).getOrElse(
            throw new IllegalStateException("Partner email address absent")
          )
        )
      )
      Ok(buildContactEmailPage(form, getPartner(partnerId), isNominated(partnerId)))
    }

  def updateEmailAddress(partnerId: String): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction).async { implicit request =>
      EmailAddress.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[EmailAddress]) =>
            Future.successful(
              BadRequest(
                buildContactEmailPage(formWithErrors, getPartner(partnerId), isNominated(partnerId))
              )
            ),
          emailAddress =>
            updateRegistration(
              { registration: Registration =>
                registration.withUpdatedPartner(
                  partnerId,
                  partner =>
                    partner.copy(contactDetails =
                      partner.contactDetails.map(_.copy(emailAddress = Some(emailAddress.value)))
                    )
                )
              },
              successfulRedirect(partnerId)
            )
        )
    }

  private def buildContactEmailPage(
    form: Form[EmailAddress],
    partner: Partner,
    isNominated: Boolean
  )(implicit request: JourneyRequest[_]) =
    contactEmailPage(form = form,
                     backLink =
                       if (isNominated)
                         amendmentRoutes.AmendRegistrationController.displayPage()
                       else
                         routes.PartnerContactDetailsCheckAnswersController.displayPage(partner.id),
                     updateCall =
                       routes.AmendPartnerContactDetailsController.updateEmailAddress(partner.id),
                     contactName = partner.name
    )

  def phoneNumber(partnerId: String): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction) { implicit request =>
      val form = PhoneNumber.form().fill(
        PhoneNumber(
          getPartner(partnerId).contactDetails.flatMap(_.phoneNumber).getOrElse(
            throw new IllegalStateException("Partner phone number absent")
          )
        )
      )
      Ok(buildContactPhoneNumberPage(form, getPartner(partnerId), isNominated(partnerId)))
    }

  def updatePhoneNumber(partnerId: String): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction).async { implicit request =>
      PhoneNumber.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[PhoneNumber]) =>
            Future.successful(
              BadRequest(
                buildContactPhoneNumberPage(formWithErrors,
                                            getPartner(partnerId),
                                            isNominated(partnerId)
                )
              )
            ),
          phoneNumber =>
            updateRegistration(
              { registration: Registration =>
                registration.withUpdatedPartner(
                  partnerId,
                  partner =>
                    partner.copy(contactDetails =
                      partner.contactDetails.map(_.copy(phoneNumber = Some(phoneNumber.value)))
                    )
                )
              },
              successfulRedirect(partnerId)
            )
        )
    }

  private def buildContactPhoneNumberPage(
    form: Form[PhoneNumber],
    partner: Partner,
    isNominated: Boolean
  )(implicit request: JourneyRequest[_]) =
    contactPhoneNumberPage(form = form,
                           backLink =
                             if (isNominated)
                               amendmentRoutes.AmendRegistrationController.displayPage()
                             else
                               routes.PartnerContactDetailsCheckAnswersController.displayPage(
                                 partner.id
                               ),
                           updateCall =
                             routes.AmendPartnerContactDetailsController.updatePhoneNumber(
                               partner.id
                             ),
                           contactName = partner.name
    )

  def address(partnerId: String): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction).async { implicit request =>
      addressCaptureService.initAddressCapture(
        AddressCaptureConfig(
          backLink = routes.PartnerContactDetailsCheckAnswersController.displayPage(partnerId).url,
          successLink =
            routes.AmendPartnerContactDetailsController.updateAddress(partnerId).url,
          alfHeadingsPrefix = "addressLookup.partner",
          entityName = Some(getPartner(partnerId).name),
          pptHeadingKey = "addressCapture.contact.heading",
          pptHintKey = None
        )
      ).map(redirect => Redirect(redirect))
    }

  def updateAddress(partnerId: String): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction).async { implicit request =>
      addressCaptureService.getCapturedAddress().flatMap {
        capturedAddress =>
          updateRegistration(
            registration =>
              registration.withUpdatedPartner(
                partnerId,
                partner =>
                  partner.copy(contactDetails =
                    partner.contactDetails.map(_.copy(address = capturedAddress))
                  )
              ),
            successfulRedirect(partnerId)
          )
      }
    }

  def jobTitle(partnerId: String): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction) { implicit request =>
      val partner = getPartner(partnerId)
      val form = JobTitle.form().fill(
        JobTitle(value =
          partner.contactDetails.flatMap(_.jobTitle).getOrElse(
            throw new IllegalStateException("Nominated Partner job title absent")
          )
        )
      )

      Ok(buildJobTitlePage(form, partner, isNominated(partnerId)))
    }

  def updateJobTitle(partnerId: String): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction).async { implicit request =>
      JobTitle.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[JobTitle]) =>
            Future.successful(
              BadRequest(
                buildJobTitlePage(formWithErrors, getPartner(partnerId), isNominated(partnerId))
              )
            ),
          jobTitle =>
            updateRegistration(
              { registration: Registration =>
                registration.withUpdatedPartner(partnerId,
                                                partner =>
                                                  partner.copy(contactDetails =
                                                    partner.contactDetails.map(
                                                      _.copy(jobTitle =
                                                        Some(jobTitle.value)
                                                      )
                                                    )
                                                  )
                )
              },
              successfulRedirect(partnerId)
            )
        )
    }

  private def buildJobTitlePage(form: Form[JobTitle], partner: Partner, isNominated: Boolean)(
    implicit request: JourneyRequest[_]
  ) =
    jobTitlePage(form = form,
                 contactName = partner.name,
                 backLink =
                   if (isNominated) amendmentRoutes.AmendRegistrationController.displayPage()
                   else
                     routes.PartnerContactDetailsCheckAnswersController.displayPage(partner.id),
                 updateCall =
                   routes.AmendPartnerContactDetailsController.updateJobTitle(partner.id)
    )

  private def getPartner(partnerId: String)(implicit request: JourneyRequest[_]) =
    request.registration.findPartner(partnerId).getOrElse(
      throw new IllegalStateException("Partner not found")
    )

  private def isNominated(partnerId: String)(implicit request: JourneyRequest[_]) =
    request.registration.isNominatedPartner(Some(partnerId)).getOrElse(false)

  private def successfulRedirect(partnerId: String)(implicit request: JourneyRequest[_]) =
    if (isNominated(partnerId)) amendmentRoutes.AmendRegistrationController.displayPage()
    else routes.PartnerContactDetailsCheckAnswersController.displayPage(partnerId)

}
