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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.connectors._
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs.{PartnershipGrsConnector, RegisteredSocietyGrsConnector, SoleTraderGrsConnector, UkCompanyGrsConnector}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{AuthAction, FormAction, SaveAndContinue}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.organisation.{routes => organisationRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner.{routes => partnerRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum.{GENERAL_PARTNERSHIP, LIMITED_LIABILITY_PARTNERSHIP, OVERSEAS_COMPANY_UK_BRANCH, REGISTERED_SOCIETY, SCOTTISH_LIMITED_PARTNERSHIP, SCOTTISH_PARTNERSHIP, SOLE_TRADER, UK_COMPANY}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.Partner
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.services.GRSRedirections
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation.partner_type
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PartnerTypeController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  val appConfig: AppConfig,
  val soleTraderGrsConnector: SoleTraderGrsConnector,
  val ukCompanyGrsConnector: UkCompanyGrsConnector,
  val partnershipGrsConnector: PartnershipGrsConnector,
  val registeredSocietyGrsConnector: RegisteredSocietyGrsConnector,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: partner_type
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport with GRSRedirections {

  def displayNewPartner(): Action[AnyContent] = displayPage()

  def displayExistingPartner(partnerId: String): Action[AnyContent] =
    displayPage(partnerId = Some(partnerId))

  private def displayPage(partnerId: Option[String] = None): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      val partner = partnerId match {
        case Some(partnerId) => request.registration.findPartner(partnerId)
        case _               => None
      }
      partner match {
        case Some(partner) =>
          Ok(page(PartnerType.form().fill(PartnerType(partner.partnerType)), partnerId))
        case _ => Ok(page(PartnerType.form(), partnerId))
      }
    }

  def submitNewPartner(): Action[AnyContent] = submit()

  def submitExistingPartner(partnerId: String): Action[AnyContent] = submit(Some(partnerId))

  private def submit(partnerId: Option[String] = None): Action[AnyContent] =
    (authenticate andThen journeyAction).async {
      implicit request =>
        PartnerType.form()
          .bindFromRequest()
          .fold(
            (formWithErrors: Form[PartnerType]) =>
              Future(BadRequest(page(formWithErrors, partnerId))),
            (partnershipPartnerType: PartnerType) =>
              updatePartnerType(partnershipPartnerType, partnerId).flatMap {
                _ =>
                  FormAction.bindFromRequest match {
                    case SaveAndContinue =>
                      partnershipPartnerType.answer match {
                        case SOLE_TRADER =>
                          getSoleTraderRedirectUrl(appConfig.soleTraderJourneyUrl,
                                                   appConfig.partnerGrsCallbackUrl(partnerId)
                          )
                            .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                        case UK_COMPANY | OVERSEAS_COMPANY_UK_BRANCH =>
                          getUkCompanyRedirectUrl(appConfig.incorpLimitedCompanyJourneyUrl,
                                                  appConfig.partnerGrsCallbackUrl(partnerId)
                          )
                            .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                        case REGISTERED_SOCIETY =>
                          getRegisteredSocietyRedirectUrl(appConfig.incorpRegistedSocietyJourneyUrl,
                            appConfig.partnerGrsCallbackUrl(partnerId))
                            .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                        case LIMITED_LIABILITY_PARTNERSHIP =>
                          getPartnershipRedirectUrl(appConfig.limitedLiabilityPartnershipJourneyUrl,
                                                    appConfig.partnerGrsCallbackUrl(partnerId)
                          ).map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                        case SCOTTISH_LIMITED_PARTNERSHIP =>
                          getPartnershipRedirectUrl(appConfig.scottishLimitedPartnershipJourneyUrl,
                                                    appConfig.partnerGrsCallbackUrl(partnerId)
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
  ): Future[Either[ServiceError, Registration]] =
    partnerId match {
      case Some(partnerId) => updateExistingPartner(partnerType, partnerId)
      case _               => updateInflightPartner(partnerType)
    }

  private def updateInflightPartner(
    partnerType: PartnerType
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val result = registration.inflightPartner match {
        case Some(partner) => partner.copy(partnerType = partnerType.answer)
        case _             => Partner(partnerType = partnerType.answer)
      }
      registration.withInflightPartner(Some(result))
    }

  private def updateExistingPartner(partnerType: PartnerType, partnerId: String)(implicit
    req: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] =
    update { registration =>
      registration.withUpdatedPartner(partnerId,
                                      partner => partner.copy(partnerType = partnerType.answer)
      )
    }

  private def redirectToPartnerNamePrompt(existingParterId: Option[String]): Future[Result] =
    Future {
      Redirect(existingParterId.map { partnerId =>
        partnerRoutes.PartnerNameController.displayExistingPartner(partnerId)
      }.getOrElse(partnerRoutes.PartnerNameController.displayNewPartner()))
    }

}
