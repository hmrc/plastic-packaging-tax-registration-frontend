/*
 * Copyright 2025 HM Revenue & Customs
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

import controllers.organisation.{routes => organisationRoutes}
import forms.organisation.PartnerType
import forms.organisation.PartnerType.FormMode
import forms.organisation.PartnerTypeEnum._
import models.genericregistration.Partner
import models.registration.{Registration, RegistrationUpdater}
import models.request.JourneyRequest
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.GRSRedirections
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.organisation.partner_type

import scala.concurrent.{ExecutionContext, Future}

abstract class PartnerTypeControllerBase(
  journeyAction: ActionBuilder[JourneyRequest, AnyContent],
  mcc: MessagesControllerComponents,
  page: partner_type,
  registrationUpdater: RegistrationUpdater
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with GRSRedirections {

  private def form(nominated: Boolean): Form[PartnerType] =
    PartnerType.form(if (nominated) FormMode.NominatedPartnerType else FormMode.OtherPartnerType)

  protected def doDisplayPage(partnerId: Option[String] = None, submitCall: Call): Action[AnyContent] =
    journeyAction { implicit request =>
      val partner: Option[Partner] =
        partnerId.fold(request.registration.inflightPartner)(request.registration.findPartner)
      val nominated = request.registration.isNominatedPartner(partnerId)

      partner match {
        case Some(partner) =>
          Ok(page(form(nominated).fill(PartnerType(partner.partnerType)), partnerId, submitCall))
        case _ => Ok(page(form(nominated), partnerId, submitCall))
      }
    }

  protected def doSubmit(partnerId: Option[String] = None, submitCall: Call): Action[AnyContent] =
    journeyAction.async { implicit request =>
      val nominated = request.registration.isNominatedPartner(partnerId)
      form(nominated)
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[PartnerType]) => Future(BadRequest(page(formWithErrors, partnerId, submitCall))),
          (partnershipPartnerType: PartnerType) =>
            updatePartnerType(partnershipPartnerType, partnerId).flatMap { _ =>
              partnershipPartnerType.answer match {
                case SOLE_TRADER =>
                  getSoleTraderRedirectUrl(appConfig.soleTraderJourneyInitUrl, grsCallbackUrl(partnerId))
                    .map(journeyStartUrl => SeeOther(journeyStartUrl))
                case UK_COMPANY | OVERSEAS_COMPANY_UK_BRANCH =>
                  getUkCompanyRedirectUrl(appConfig.incorpLimitedCompanyJourneyUrl, grsCallbackUrl(partnerId))
                    .map(journeyStartUrl => SeeOther(journeyStartUrl))
                case REGISTERED_SOCIETY =>
                  getRegisteredSocietyRedirectUrl(appConfig.incorpRegistedSocietyJourneyUrl, grsCallbackUrl(partnerId))
                    .map(journeyStartUrl => SeeOther(journeyStartUrl))
                case LIMITED_LIABILITY_PARTNERSHIP =>
                  getPartnershipRedirectUrl(
                    appConfig.limitedLiabilityPartnershipJourneyUrl,
                    grsCallbackUrl(partnerId),
                    businessVerification = false
                  ).map(journeyStartUrl => SeeOther(journeyStartUrl))
                case SCOTTISH_LIMITED_PARTNERSHIP =>
                  getPartnershipRedirectUrl(
                    appConfig.scottishLimitedPartnershipJourneyUrl,
                    grsCallbackUrl(partnerId),
                    businessVerification = false
                  ).map(journeyStartUrl => SeeOther(journeyStartUrl))
                case SCOTTISH_PARTNERSHIP | GENERAL_PARTNERSHIP =>
                  redirectToPartnerNamePrompt(partnerId)
                case _ =>
                  Future(Redirect(organisationRoutes.RegisterAsOtherOrganisationController.onPageLoad()))
              }
            }
        )
    }

  private def updatePartnerType(partnerType: PartnerType, partnerId: Option[String])(implicit
    request: JourneyRequest[AnyContent]
  ): Future[Registration] =
    partnerId match {
      case Some(partnerId) => updateExistingPartner(partnerType, partnerId)
      case _               => updateInflightPartner(partnerType)
    }

  private def updateInflightPartner(
    partnerType: PartnerType
  )(implicit req: JourneyRequest[AnyContent]): Future[Registration] =
    registrationUpdater.updateRegistration { registration =>
      val result = registration.inflightPartner match {
        case Some(partner) => partner.copy(partnerType = partnerType.answer)
        case _             => Partner(partnerType = partnerType.answer)
      }
      registration.withInflightPartner(Some(result))
    }

  private def updateExistingPartner(partnerType: PartnerType, partnerId: String)(implicit
    req: JourneyRequest[AnyContent]
  ): Future[Registration] =
    registrationUpdater.updateRegistration { registration =>
      registration.withUpdatedPartner(partnerId, partner => partner.copy(partnerType = partnerType.answer))
    }

  def redirectToPartnerNamePrompt(existingParterId: Option[String]): Future[Result]

  def grsCallbackUrl(partnerId: Option[String] = None): String
}
