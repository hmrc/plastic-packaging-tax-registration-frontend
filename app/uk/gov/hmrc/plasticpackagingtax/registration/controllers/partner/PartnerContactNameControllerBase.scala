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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.group.MemberName
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  Partner,
  PartnerContactDetails
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  Registration,
  RegistrationUpdater
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{
  AuthenticatedRequest,
  JourneyRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partner.partner_member_name_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

abstract class PartnerContactNameControllerBase(
  val authenticate: AuthActioning,
  val journeyAction: ActionRefiner[AuthenticatedRequest, JourneyRequest],
  mcc: MessagesControllerComponents,
  page: partner_member_name_page,
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
  ): Result = {
    val existingNameFields = for {
      contactDetails <- partner.contactDetails
      firstName      <- contactDetails.firstName
      lastName       <- contactDetails.lastName
    } yield (firstName, lastName)

    val form = existingNameFields match {
      case Some(data) =>
        MemberName.form().fill(MemberName(data._1, data._2))
      case None =>
        MemberName.form()
    }

    Ok(page(form, partner.name, backCall, submitCall))
  }

  protected def doSubmit(
    partnerId: Option[String],
    backCall: Call,
    submitCall: Call,
    dropoutCall: Call
  ): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      def updateAction(memberName: MemberName): Future[Registration] =
        partnerId match {
          case Some(partnerId) => updateExistingPartner(memberName, partnerId)
          case _               => updateInflightPartner(memberName)
        }

      getPartner(partnerId).map { partner =>
        val nextPage = partnerId match {
          case Some(partnerId) => onwardCallExistingPartner(partnerId)
          case _               => onwardCallNewPartner()
        }
        handleSubmission(partner, backCall, submitCall, nextPage, dropoutCall, updateAction)
      }.getOrElse(
        Future.successful(throw new IllegalStateException("Expected existing partner missing"))
      )
    }

  private def handleSubmission(
    partner: Partner,
    backCall: Call,
    submitCall: Call,
    onwardsCall: Call,
    dropoutCall: Call,
    updateAction: MemberName => Future[Registration]
  )(implicit request: JourneyRequest[AnyContent]): Future[Result] =
    MemberName.form()
      .bindFromRequest()
      .fold(
        (formWithErrors: Form[MemberName]) =>
          Future.successful(BadRequest(page(formWithErrors, partner.name, backCall, submitCall))),
        fullName =>
          updateAction(fullName).map { _ =>
            FormAction.bindFromRequest match {
              case SaveAndContinue =>
                Redirect(onwardsCall)
              case _ =>
                Redirect(dropoutCall)
            }
          }
      )

  private def updateInflightPartner(
    formData: MemberName
  )(implicit req: JourneyRequest[AnyContent]): Future[Registration] =
    registrationUpdater.updateRegistration { registration =>
      registration.inflightPartner.map { partner =>
        val updateContactDetails: PartnerContactDetails = partner.contactDetails match {
          case Some(contactDetails) =>
            contactDetails.copy(firstName = Some(formData.firstName),
                                lastName = Some(formData.lastName)
            )
          case None =>
            PartnerContactDetails(firstName = Some(formData.firstName),
                                  lastName = Some(formData.lastName)
            )
        }
        val withContactName = partner.copy(contactDetails =
          Some(updateContactDetails)
        )
        registration.withInflightPartner(Some(withContactName))
      }.getOrElse {
        registration
      }
    }

  private def updateExistingPartner(formData: MemberName, partnerId: String)(implicit
    req: JourneyRequest[AnyContent]
  ): Future[Registration] =
    registrationUpdater.updateRegistration { registration =>
      registration.withUpdatedPartner(partnerId,
                                      partner =>
                                        partner.copy(contactDetails =
                                          partner.contactDetails.map(
                                            _.copy(firstName = Some(formData.firstName),
                                                   lastName = Some(formData.lastName)
                                            )
                                          )
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

  def onwardCallNewPartner()(implicit request: JourneyRequest[AnyContent]): Call

  def onwardCallExistingPartner(partnerId: String)(implicit
    request: JourneyRequest[AnyContent]
  ): Call

}
