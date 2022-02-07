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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.group

import play.api.data.Form
import play.api.mvc._
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthNoEnrolmentCheckAction
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.AmendmentController
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact._
import uk.gov.hmrc.plasticpackagingtax.registration.forms.group.MemberName
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{
  AmendmentJourneyAction,
  JourneyRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.services.CountryService
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact._
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.group.{
  member_email_address_page,
  member_name_page,
  member_phone_number_page
}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendMemberContactDetailsController @Inject() (
  authenticate: AuthNoEnrolmentCheckAction,
  mcc: MessagesControllerComponents,
  amendmentJourneyAction: AmendmentJourneyAction,
  contactNamePage: member_name_page,
  phoneNumberPage: member_phone_number_page,
  emailAddressPage: member_email_address_page,
  addressPage: address_page,
  countryService: CountryService
)(implicit ec: ExecutionContext)
    extends AmendmentController(mcc, amendmentJourneyAction) {

  def contactName(memberId: String): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction) { implicit request =>
      request.registration.findMember(memberId).flatMap(_.contactDetails) match {
        case Some(contactDetails) =>
          Ok(
            buildContactNamePage(
              MemberName.form().fill(MemberName(contactDetails.firstName, contactDetails.lastName)),
              memberId
            )
          )
        case _ =>
          Ok(buildContactNamePage(MemberName.form(), memberId))
      }
    }

  def updateContactName(memberId: String): Action[AnyContent] = {

    def updateContactName(firstName: String, lastName: String): Registration => Registration = {
      registration: Registration =>
        registration.copy(groupDetail =
          registration.groupDetail.map(
            _.withUpdatedOrNewMember(
              registration.findMember(memberId).map(
                _.withUpdatedGroupMemberName(firstName, lastName)
              ).getOrElse(throw new IllegalStateException("Expected group member absent"))
            )
          )
        )
    }

    (authenticate andThen amendmentJourneyAction).async { implicit request =>
      MemberName.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[MemberName]) =>
            Future.successful(BadRequest(buildContactNamePage(formWithErrors, memberId))),
          memberName =>
            updateGroupMemberRegistration(
              updateContactName(memberName.firstName, memberName.lastName),
              memberId
            )
        )
    }
  }

  private def buildContactNamePage(form: Form[MemberName], memberId: String)(implicit
    request: JourneyRequest[AnyContent]
  ) =
    contactNamePage(form,
                    request.registration.findMember(memberId).map(_.businessName).getOrElse(
                      "your organisation"
                    ),
                    routes.ContactDetailsCheckAnswersController.displayPage(memberId),
                    routes.AmendMemberContactDetailsController.updateContactName(memberId),
                    memberId
    )

  def phoneNumber(memberId: String): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction) { implicit request =>
      val contactDetails = request.registration.findMember(memberId).flatMap(_.contactDetails)
      val memberName     = contactDetails.map(_.groupMemberName)
      contactDetails.flatMap(_.phoneNumber) match {
        case Some(phoneNumber) =>
          Ok(
            buildPhoneNumberPage(PhoneNumber.form().fill(PhoneNumber(phoneNumber)),
                                 memberName,
                                 memberId
            )
          )
        case _ =>
          Ok(buildPhoneNumberPage(PhoneNumber.form(), memberName, memberId))
      }
    }

  def updatePhoneNumber(memberId: String): Action[AnyContent] = {

    def updatePhoneNumber(updatedPhoneNumber: String): Registration => Registration = {
      registration: Registration =>
        registration.copy(groupDetail =
          registration.groupDetail.map(
            _.withUpdatedOrNewMember(
              registration.findMember(memberId).map(
                _.withUpdatedGroupMemberPhoneNumber(updatedPhoneNumber)
              ).getOrElse(throw new IllegalStateException("Expected group member absent"))
            )
          )
        )
    }

    (authenticate andThen amendmentJourneyAction).async { implicit request =>
      val memberName =
        request.registration.findMember(memberId).flatMap(_.contactDetails).map(_.groupMemberName)
      PhoneNumber.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[PhoneNumber]) =>
            Future.successful(
              BadRequest(buildPhoneNumberPage(formWithErrors, memberName, memberId))
            ),
          phoneNumber =>
            updateGroupMemberRegistration(updatePhoneNumber(phoneNumber.value), memberId)
        )
    }
  }

  private def buildPhoneNumberPage(
    form: Form[PhoneNumber],
    memberName: Option[String],
    memberId: String
  )(implicit request: JourneyRequest[AnyContent]) =
    phoneNumberPage(form,
                    memberName,
                    routes.ContactDetailsCheckAnswersController.displayPage(memberId),
                    routes.AmendMemberContactDetailsController.updatePhoneNumber(memberId)
    )

  def email(memberId: String): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction) { implicit request =>
      val contactDetails = request.registration.findMember(memberId).flatMap(_.contactDetails)
      val memberName     = contactDetails.map(_.groupMemberName)
      contactDetails.flatMap(_.email) match {
        case Some(emailAddress) =>
          Ok(
            buildEmailAddressPage(EmailAddress.form().fill(EmailAddress(emailAddress)),
                                  memberName,
                                  memberId
            )
          )
        case _ =>
          Ok(buildEmailAddressPage(EmailAddress.form(), memberName, memberId))
      }
    }

  def updateEmail(memberId: String): Action[AnyContent] = {

    def updateEmailAddress(updatedEmailAddress: String): Registration => Registration = {
      registration: Registration =>
        registration.copy(groupDetail =
          registration.groupDetail.map(
            _.withUpdatedOrNewMember(
              registration.findMember(memberId).map(
                _.withUpdatedGroupMemberEmail(updatedEmailAddress)
              ).getOrElse(throw new IllegalStateException("Expected group member absent"))
            )
          )
        )
    }

    (authenticate andThen amendmentJourneyAction).async { implicit request =>
      val memberName =
        request.registration.findMember(memberId).flatMap(_.contactDetails).map(_.groupMemberName)
      EmailAddress.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[EmailAddress]) =>
            Future.successful(
              BadRequest(buildEmailAddressPage(formWithErrors, memberName, memberId))
            ),
          emailAddress =>
            updateGroupMemberRegistration(updateEmailAddress(emailAddress.value), memberId)
        )
    }
  }

  private def buildEmailAddressPage(
    form: Form[EmailAddress],
    memberName: Option[String],
    memberId: String
  )(implicit request: JourneyRequest[AnyContent]) =
    emailAddressPage(form,
                     memberName,
                     routes.ContactDetailsCheckAnswersController.displayPage(memberId),
                     routes.AmendMemberContactDetailsController.updateEmail(memberId)
    )

  def address(memberId: String): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction) { implicit request =>
      request.registration.findMember(memberId).flatMap(_.contactDetails).flatMap(_.address) match {
        case Some(address) =>
          Ok(buildAddressPage(Address.form().fill(address), memberId))
        case _ =>
          Ok(buildAddressPage(Address.form(), memberId))
      }
    }

  def updateAddress(memberId: String): Action[AnyContent] = {

    def updateAddress(updatedAddress: Address): Registration => Registration = {
      registration: Registration =>
        registration.copy(groupDetail =
          registration.groupDetail.map(
            _.withUpdatedOrNewMember(
              registration.findMember(memberId).map(
                _.withUpdatedGroupMemberAddress(updatedAddress)
              ).getOrElse(throw new IllegalStateException("Expected group member absent"))
            )
          )
        )
    }

    (authenticate andThen amendmentJourneyAction).async { implicit request =>
      Address.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[Address]) =>
            Future.successful(BadRequest(buildAddressPage(formWithErrors, memberId))),
          address => updateGroupMemberRegistration(updateAddress(address), memberId)
        )
    }
  }

  private def buildAddressPage(form: Form[Address], memberId: String)(implicit
    request: JourneyRequest[AnyContent]
  ) =
    addressPage(form,
                countryService.getAll(),
                routes.ContactDetailsCheckAnswersController.displayPage(memberId),
                routes.AmendMemberContactDetailsController.updateAddress(memberId)
    )

}
