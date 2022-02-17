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
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.organisation.{
  routes => organisationRoutes
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum.{
  GENERAL_PARTNERSHIP,
  LIMITED_LIABILITY_PARTNERSHIP,
  OVERSEAS_COMPANY_UK_BRANCH,
  REGISTERED_SOCIETY,
  SCOTTISH_LIMITED_PARTNERSHIP,
  SCOTTISH_PARTNERSHIP,
  SOLE_TRADER,
  UK_COMPANY
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.Partner
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  Registration,
  RegistrationUpdater
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{
  AuthenticatedRequest,
  JourneyRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.services.GRSRedirections
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation.partner_type
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

abstract class PartnerTypeControllerBase(
  authenticate: AuthActioning,
  journeyAction: ActionRefiner[AuthenticatedRequest, JourneyRequest],
  mcc: MessagesControllerComponents,
  page: partner_type,
  registrationUpdater: RegistrationUpdater
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with GRSRedirections {

  protected def doDisplayPage(
    partnerId: Option[String] = None,
    submitCall: Call
  ): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      val partner = partnerId match {
        case Some(partnerId) => request.registration.findPartner(partnerId)
        case _               => request.registration.inflightPartner
      }
      partner match {
        case Some(partner) =>
          Ok(page(PartnerType.form().fill(PartnerType(partner.partnerType)), partnerId, submitCall))
        case _ => Ok(page(PartnerType.form(), partnerId, submitCall))
      }
    }

  protected def doSubmit(partnerId: Option[String] = None, submitCall: Call): Action[AnyContent] =
    (authenticate andThen journeyAction).async {
      implicit request =>
        PartnerType.form()
          .bindFromRequest()
          .fold(
            (formWithErrors: Form[PartnerType]) =>
              Future(BadRequest(page(formWithErrors, partnerId, submitCall))),
            (partnershipPartnerType: PartnerType) =>
              updatePartnerType(partnershipPartnerType, partnerId).flatMap {
                _ =>
                  FormAction.bindFromRequest match {
                    case SaveAndContinue =>
                      partnershipPartnerType.answer match {
                        case SOLE_TRADER =>
                          getSoleTraderRedirectUrl(appConfig.soleTraderJourneyUrl,
                                                   grsCallbackUrl(partnerId)
                          )
                            .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                        case UK_COMPANY | OVERSEAS_COMPANY_UK_BRANCH =>
                          getUkCompanyRedirectUrl(appConfig.incorpLimitedCompanyJourneyUrl,
                                                  grsCallbackUrl(partnerId)
                          )
                            .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                        case REGISTERED_SOCIETY =>
                          getRegisteredSocietyRedirectUrl(appConfig.incorpRegistedSocietyJourneyUrl,
                                                          grsCallbackUrl(partnerId)
                          )
                            .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                        case LIMITED_LIABILITY_PARTNERSHIP =>
                          getPartnershipRedirectUrl(appConfig.limitedLiabilityPartnershipJourneyUrl,
                                                    grsCallbackUrl(partnerId)
                          ).map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                        case SCOTTISH_LIMITED_PARTNERSHIP =>
                          getPartnershipRedirectUrl(appConfig.scottishLimitedPartnershipJourneyUrl,
                                                    grsCallbackUrl(partnerId)
                          ).map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                        case SCOTTISH_PARTNERSHIP | GENERAL_PARTNERSHIP =>
                          redirectToPartnerNamePrompt(partnerId)
                        case _ =>
                          //TODO later CHARITABLE_INCORPORATED_ORGANISATION & OVERSEAS_COMPANY_NO_UK_BRANCH will have their own not supported page
                          Future(
                            Redirect(
                              organisationRoutes.OrganisationTypeNotSupportedController.onPageLoad()
                            )
                          )
                      }
                    case _ => Future(Redirect(commonRoutes.TaskListController.displayPage()))
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
      registration.withUpdatedPartner(partnerId,
                                      partner => partner.copy(partnerType = partnerType.answer)
      )
    }

  def redirectToPartnerNamePrompt(existingParterId: Option[String]): Future[Result]

  def grsCallbackUrl(partnerId: Option[String] = None): String
}
