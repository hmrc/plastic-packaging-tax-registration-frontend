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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.connectors._
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs.{
  PartnershipGrsConnector,
  SoleTraderGrsConnector,
  UkCompanyGrsConnector
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{
  AuthAction,
  FormAction,
  SaveAndContinue
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.organisation.{
  routes => organisationRoutes
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum.{
  LIMITED_LIABILITY_PARTNERSHIP,
  OVERSEAS_COMPANY_UK_BRANCH,
  SCOTTISH_LIMITED_PARTNERSHIP,
  SCOTTISH_PARTNERSHIP,
  SOLE_TRADER,
  UK_COMPANY
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  IncorpEntityGrsCreateRequest,
  Partner,
  PartnershipGrsCreateRequest,
  SoleTraderGrsCreateRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation.partner_type
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PartnerTypeController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  appConfig: AppConfig,
  soleTraderGrsConnector: SoleTraderGrsConnector,
  ukCompanyGrsConnector: UkCompanyGrsConnector,
  partnershipGrsConnector: PartnershipGrsConnector,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: partner_type
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

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
    (authenticate andThen journeyAction).async { implicit request =>
      PartnerType.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[PartnerType]) =>
            Future(BadRequest(page(formWithErrors, partnerId))),
          (partnershipPartnerType: PartnerType) =>
            updatePartnerType(partnershipPartnerType, partnerId).flatMap { _ =>
              FormAction.bindFromRequest match {
                case SaveAndContinue =>
                  partnershipPartnerType.answer match {
                    case Some(SOLE_TRADER) =>
                      getSoleTraderRedirectUrl(appConfig.soleTraderJourneyUrl, partnerId)
                        .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                    case Some(UK_COMPANY) | Some(OVERSEAS_COMPANY_UK_BRANCH) =>
                      getUkCompanyRedirectUrl(appConfig.incorpLimitedCompanyJourneyUrl, partnerId)
                        .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                    case Some(LIMITED_LIABILITY_PARTNERSHIP) =>
                      getPartnershipRedirectUrl(appConfig.limitedLiabilityPartnershipJourneyUrl,
                                                partnerId
                      )
                        .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                    case Some(SCOTTISH_PARTNERSHIP) =>
                      getPartnershipRedirectUrl(appConfig.scottishPartnershipJourneyUrl, partnerId)
                        .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                    case Some(SCOTTISH_LIMITED_PARTNERSHIP) =>
                      getPartnershipRedirectUrl(appConfig.scottishLimitedPartnershipJourneyUrl,
                                                partnerId
                      )
                        .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
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

  private def getUkCompanyRedirectUrl(url: String, partnerId: Option[String])(implicit
    request: JourneyRequest[AnyContent]
  ): Future[String] =
    ukCompanyGrsConnector.createJourney(
      IncorpEntityGrsCreateRequest(appConfig.partnerGrsCallbackUrl(partnerId),
                                   Some(request2Messages(request)("service.name")),
                                   appConfig.serviceIdentifier,
                                   appConfig.signOutLink,
                                   appConfig.grsAccessibilityStatementPath,
                                   businessVerificationCheck = false
      ),
      url
    )

  private def getPartnershipRedirectUrl(url: String, partnerId: Option[String])(implicit
    request: JourneyRequest[AnyContent]
  ): Future[String] =
    partnershipGrsConnector.createJourney(
      PartnershipGrsCreateRequest(appConfig.partnerGrsCallbackUrl(partnerId),
                                  Some(request2Messages(request)("service.name")),
                                  appConfig.serviceIdentifier,
                                  appConfig.signOutLink,
                                  appConfig.grsAccessibilityStatementPath
      ),
      url
    )

  private def getSoleTraderRedirectUrl(url: String, partnerId: Option[String])(implicit
    request: JourneyRequest[AnyContent]
  ): Future[String] =
    soleTraderGrsConnector.createJourney(
      SoleTraderGrsCreateRequest(appConfig.partnerGrsCallbackUrl(partnerId),
                                 Some(request2Messages(request)("service.name")),
                                 appConfig.serviceIdentifier,
                                 appConfig.signOutLink,
                                 appConfig.grsAccessibilityStatementPath
      ),
      url
    )

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

}