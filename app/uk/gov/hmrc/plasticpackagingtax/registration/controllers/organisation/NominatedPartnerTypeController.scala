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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.organisation

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
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
  PartnershipGrsCreateRequest,
  SoleTraderGrsCreateRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation.partnership_partner_type
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NominatedPartnerTypeController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  appConfig: AppConfig,
  soleTraderGrsConnector: SoleTraderGrsConnector,
  ukCompanyGrsConnector: UkCompanyGrsConnector,
  partnershipGrsConnector: PartnershipGrsConnector,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: partnership_partner_type
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      request.registration.organisationDetails.partnershipDetails match {
        case Some(partnershipDetails) =>
          Future(
            Ok(
              page(
                PartnerType.form().fill(
                  PartnerType(
                    partnershipDetails.nominatedPartner.map(_.partnerType).getOrElse(
                      throw new IllegalStateException("No Partner partnership type found")
                    )
                  )
                )
              )
            )
          )
        case _ => Future(Ok(page(PartnerType.form())))
      }
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      PartnerType.form()
        .bindFromRequest()
        .fold((formWithErrors: Form[PartnerType]) => Future(BadRequest(page(formWithErrors))),
              (partnershipPartnerType: PartnerType) =>
                updateNominatedPartnershipType(partnershipPartnerType).flatMap { _ =>
                  FormAction.bindFromRequest match {
                    case SaveAndContinue =>
                      partnershipPartnerType.answer match {
                        case Some(SOLE_TRADER) =>
                          getSoleTraderRedirectUrl(appConfig.soleTraderJourneyUrl)
                            .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                        case Some(UK_COMPANY) | Some(OVERSEAS_COMPANY_UK_BRANCH) =>
                          getUkCompanyRedirectUrl(appConfig.incorpLimitedCompanyJourneyUrl)
                            .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                        case Some(LIMITED_LIABILITY_PARTNERSHIP) =>
                          getPartnershipRedirectUrl(appConfig.limitedLiabilityPartnershipJourneyUrl)
                            .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                        case Some(SCOTTISH_PARTNERSHIP) =>
                          getPartnershipRedirectUrl(appConfig.scottishPartnershipJourneyUrl)
                            .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                        case Some(SCOTTISH_LIMITED_PARTNERSHIP) =>
                          getPartnershipRedirectUrl(appConfig.scottishLimitedPartnershipJourneyUrl)
                            .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                        case _ =>
                          //TODO later CHARITABLE_INCORPORATED_ORGANISATION & OVERSEAS_COMPANY_NO_UK_BRANCH will have their own not supported page
                          Future(
                            Redirect(routes.OrganisationTypeNotSupportedController.onPageLoad())
                          )
                      }
                    case _ => Future(Redirect(commonRoutes.TaskListController.displayPage()))
                  }
                }
        )
    }

  private def getUkCompanyRedirectUrl(
    url: String
  )(implicit request: JourneyRequest[AnyContent]): Future[String] =
    ukCompanyGrsConnector.createJourney(
      IncorpEntityGrsCreateRequest(appConfig.grsCallbackUrl,
                                   Some(request2Messages(request)("service.name")),
                                   appConfig.serviceIdentifier,
                                   appConfig.signOutLink,
                                   appConfig.grsAccessibilityStatementPath,
                                   businessVerificationCheck = false
      ),
      url
    )

  private def getPartnershipRedirectUrl(
    url: String
  )(implicit request: JourneyRequest[AnyContent]): Future[String] =
    partnershipGrsConnector.createJourney(
      PartnershipGrsCreateRequest(appConfig.grsCallbackUrl,
                                  Some(request2Messages(request)("service.name")),
                                  appConfig.serviceIdentifier,
                                  appConfig.signOutLink,
                                  appConfig.grsAccessibilityStatementPath
      ),
      url
    )

  private def getSoleTraderRedirectUrl(
    url: String
  )(implicit request: JourneyRequest[AnyContent]): Future[String] =
    soleTraderGrsConnector.createJourney(
      SoleTraderGrsCreateRequest(appConfig.grsCallbackUrl,
                                 Some(request2Messages(request)("service.name")),
                                 appConfig.serviceIdentifier,
                                 appConfig.signOutLink,
                                 appConfig.grsAccessibilityStatementPath
      ),
      url
    )

  private def updateNominatedPartnershipType(
    partnershipPartnerType: PartnerType
  )(implicit hc: HeaderCarrier, request: JourneyRequest[AnyContent]): Future[Registration] =
    update {
      registration =>
        registration.copy(organisationDetails =
          registration.organisationDetails.copy(partnershipDetails =
            Some(
              registration.organisationDetails.partnershipDetails.map(
                details =>
                  details.copy(nominatedPartner = details.nominatedPartner match {
                    case Some(partner) =>
                      Some(partner.copy(partnerType = partnershipPartnerType.answer))
                    case _ => throw new IllegalStateException("No nominated partner found")
                  })
              ).getOrElse(throw new IllegalStateException("No partnership details found"))
            )
          )
        )
    }.map {
      case Right(registration) =>
        registration
      case Left(ex) => throw ex
    }

}
