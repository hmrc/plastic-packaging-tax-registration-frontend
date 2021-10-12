/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.config.{AppConfig, Features}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors._
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs.{
  SoleTraderGrsConnector,
  UkCompanyGrsConnector
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{
  AuthAction,
  FormAction,
  SaveAndContinue
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.{OrgType, OrganisationType}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  SoleTraderGrsCreateRequest,
  UkCompanyGrsCreateRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation_type
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OrganisationDetailsTypeController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  appConfig: AppConfig,
  soleTraderGrsConnector: SoleTraderGrsConnector,
  ukCompanyGrsConnector: UkCompanyGrsConnector,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: organisation_type
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      request.registration.organisationDetails.organisationType match {
        case Some(data) =>
          Future(Ok(page(OrganisationType.form().fill(OrganisationType(Some(data))))))
        case _ => Future(Ok(page(OrganisationType.form())))
      }
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      OrganisationType.form()
        .bindFromRequest()
        .fold((formWithErrors: Form[OrganisationType]) => Future(BadRequest(page(formWithErrors))),
              organisationType =>
                updateRegistration(organisationType).flatMap {
                  case Right(_) =>
                    FormAction.bindFromRequest match {
                      case SaveAndContinue =>
                        organisationType.answer match {
                          case Some(OrgType.UK_COMPANY) =>
                            getUkCompanyRedirectUr()
                              .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                          case Some(OrgType.SOLE_TRADER)
                              if !request.isFeatureFlagEnabled(Features.isUkCompanyPrivateBeta) =>
                            getSoleTraderRedirectUr()
                              .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                          case Some(OrgType.PARTNERSHIP)
                              if !request.isFeatureFlagEnabled(Features.isUkCompanyPrivateBeta) =>
                            Future(Redirect(routes.PartnershipTypeController.displayPage()))
                          case _ =>
                            Future(
                              Redirect(routes.OrganisationTypeNotSupportedController.onPageLoad())
                            )
                        }
                      case _ => Future(Redirect(routes.RegistrationController.displayPage()))
                    }
                  case Left(error) => throw error
                }
        )

    }

  private def getSoleTraderRedirectUr()(implicit
    request: JourneyRequest[AnyContent]
  ): Future[String] =
    soleTraderGrsConnector.createJourney(
      SoleTraderGrsCreateRequest(appConfig.grsCallbackUrl,
                                 Some(request2Messages(request)("service.name")),
                                 appConfig.serviceIdentifier,
                                 appConfig.externalSignOutLink
      )
    )

  private def getUkCompanyRedirectUr()(implicit
    request: JourneyRequest[AnyContent]
  ): Future[String] =
    ukCompanyGrsConnector.createJourney(
      UkCompanyGrsCreateRequest(appConfig.grsCallbackUrl,
                                Some(request2Messages(request)("service.name")),
                                appConfig.serviceIdentifier,
                                appConfig.externalSignOutLink
      )
    )

  private def updateRegistration(
    formData: OrganisationType
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val updatedOrganisationDetails =
        registration.organisationDetails.copy(organisationType = formData.answer)
      registration.copy(organisationDetails = updatedOrganisationDetails)
    }

}
