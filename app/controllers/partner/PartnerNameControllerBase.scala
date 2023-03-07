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

package controllers.partner

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import config.AppConfig
import connectors.grs.{
  PartnershipGrsConnector,
  RegisteredSocietyGrsConnector,
  SoleTraderGrsConnector,
  UkCompanyGrsConnector
}
import controllers.actions.AuthActioning
import controllers.organisation.{
  routes => organisationRoutes
}
import forms.organisation.PartnerTypeEnum.{
  GENERAL_PARTNERSHIP,
  SCOTTISH_PARTNERSHIP
}
import forms.partner.PartnerName
import models.genericregistration.{
  Partner,
  PartnerPartnershipDetails
}
import models.registration.{
  Registration,
  RegistrationUpdater
}
import models.request.{
  AuthenticatedRequest,
  JourneyRequest
}
import services.GRSRedirections
import views.html.partner.partner_name_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

abstract class PartnerNameControllerBase(
  val authenticate: AuthActioning,
  val journeyAction: ActionRefiner[AuthenticatedRequest, JourneyRequest],
  val ukCompanyGrsConnector: UkCompanyGrsConnector,
  val soleTraderGrsConnector: SoleTraderGrsConnector,
  val partnershipGrsConnector: PartnershipGrsConnector,
  val registeredSocietyGrsConnector: RegisteredSocietyGrsConnector,
  val registrationUpdater: RegistrationUpdater,
  mcc: MessagesControllerComponents,
  page: partner_name_page,
  appConfig: AppConfig
)(implicit executionContext: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with GRSRedirections {

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

  protected def doSubmit(
    partnerId: Option[String],
    backCall: Call,
    submitCall: Call,
    dropoutCall: Call
  ): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      getPartner(partnerId).map { partner =>
        def updateAction(partnerName: PartnerName): Future[Registration] =
          partnerId match {
            case Some(partnerId) => updateExistingPartner(partnerName, partnerId)
            case _               => updateInflightPartner(partnerName)
          }
        handleSubmission(partner, partnerId, backCall, submitCall, dropoutCall, updateAction)
      }.getOrElse {
        Future.successful(throw new IllegalStateException("Expected partner missing"))
      }
    }

  private def renderPageFor(partner: Partner, backCall: Call, submitCall: Call)(implicit
    request: JourneyRequest[AnyContent]
  ): Result =
    if (partner.canEditName) {
      val form = partner.partnerPartnershipDetails.flatMap(_.partnershipName) match {
        case Some(data) =>
          PartnerName.form().fill(PartnerName(data))
        case None =>
          PartnerName.form()
      }
      Ok(page(form, backCall, submitCall))
    } else
      throw new IllegalStateException("Partner type does not permit user supplied names")

  private def handleSubmission(
    partner: Partner,
    existingPartnerId: Option[String],
    backCall: Call,
    submitCall: Call,
    dropoutCall: Call,
    updateAction: PartnerName => Future[Registration]
  )(implicit request: JourneyRequest[AnyContent]): Future[Result] =
    if (partner.canEditName)
      PartnerName.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[PartnerName]) =>
            Future.successful(BadRequest(page(formWithErrors, backCall, submitCall))),
          partnerName =>
            updateAction(partnerName).flatMap { _ =>
              // Select GRS journey type based on selected partner type
              partner.partnerType match {
                case SCOTTISH_PARTNERSHIP =>
                  getPartnershipRedirectUrl(appConfig.scottishPartnershipJourneyUrl,
                    grsCallbackUrl(existingPartnerId)
                  ).map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                case GENERAL_PARTNERSHIP =>
                  getPartnershipRedirectUrl(appConfig.generalPartnershipJourneyUrl,
                    grsCallbackUrl(existingPartnerId)
                  ).map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                case _ =>
                  Future(
                    Redirect(
                      organisationRoutes.RegisterAsOtherOrganisationController.onPageLoad()
                    )
                  )
              }
            }
        )
    else
      throw new IllegalStateException("Partner type does not permit user supplied names")

  private def updateInflightPartner(
    formData: PartnerName
  )(implicit req: JourneyRequest[AnyContent]): Future[Registration] =
    registrationUpdater.updateRegistration { registration =>
      registration.inflightPartner.map { partner: Partner =>
        registration.withInflightPartner(Some(setPartnershipNameFor(partner, formData)))
      }.getOrElse {
        registration
      }
    }

  private def updateExistingPartner(formData: PartnerName, partnerId: String)(implicit
    req: JourneyRequest[AnyContent]
  ): Future[Registration] =
    registrationUpdater.updateRegistration { registration =>
      registration.withUpdatedPartner(partnerId,
                                      partner => setPartnershipNameFor(partner, formData)
      )
    }

  private def setPartnershipNameFor(partner: Partner, formData: PartnerName): Partner = {
    val partnershipDetailsWithPartnershipName = partner.partnerPartnershipDetails.map(
      _.copy(partnershipName = Some(formData.value))
    ).getOrElse {
      // Partnership details have not been created yet; we need to create a minimal one to carry the user supplied name
      // until the GRS callback can fully populate it
      PartnerPartnershipDetails(partnershipName = Some(formData.value))
    }
    partner.copy(partnerPartnershipDetails = Some(partnershipDetailsWithPartnershipName))
  }

  private def getPartner(
    partnerId: Option[String]
  )(implicit request: JourneyRequest[_]): Option[Partner] =
    partnerId match {
      case Some(partnerId) => request.registration.findPartner(partnerId)
      case _               => request.registration.inflightPartner
    }

  def grsCallbackUrl(partnerId: Option[String] = None): String

}
