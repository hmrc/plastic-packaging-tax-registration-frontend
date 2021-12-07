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
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.connectors._
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs.PartnershipGrsConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{
  AuthAction,
  FormAction,
  SaveAndContinue
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnershipType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnershipTypeEnum.{
  GENERAL_PARTNERSHIP,
  LIMITED_LIABILITY_PARTNERSHIP,
  LIMITED_PARTNERSHIP,
  SCOTTISH_LIMITED_PARTNERSHIP,
  SCOTTISH_PARTNERSHIP
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  PartnershipDetails,
  PartnershipGrsCreateRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation.partnership_type
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PartnershipTypeController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  appConfig: AppConfig,
  partnershipGrsConnector: PartnershipGrsConnector,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: partnership_type
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      request.registration.organisationDetails.partnershipDetails match {
        case Some(partnershipDetails) =>
          Future(
            Ok(
              page(
                PartnershipType.form().fill(
                  PartnershipType(Some(partnershipDetails.partnershipType))
                )
              )
            )
          )
        case _ => Future(Ok(page(PartnershipType.form())))
      }
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      PartnershipType.form()
        .bindFromRequest()
        .fold((formWithErrors: Form[PartnershipType]) => Future(BadRequest(page(formWithErrors))),
              partnershipType =>
                updateRegistration(partnershipType).flatMap {
                  case Right(_) =>
                    FormAction.bindFromRequest match {
                      case SaveAndContinue =>
                        partnershipType.answer match {
                          case Some(GENERAL_PARTNERSHIP) =>
                            getRedirectUrl(appConfig.generalPartnershipJourneyUrl)
                              .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                          case Some(SCOTTISH_PARTNERSHIP) =>
                            getRedirectUrl(appConfig.scottishPartnershipJourneyUrl)
                              .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                          case Some(LIMITED_PARTNERSHIP) =>
                            getRedirectUrl(appConfig.limitedPartnershipJourneyUrl)
                              .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                          case Some(SCOTTISH_LIMITED_PARTNERSHIP) =>
                            getRedirectUrl(appConfig.scottishLimitedPartnershipJourneyUrl)
                              .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                          case Some(LIMITED_LIABILITY_PARTNERSHIP) =>
                            getRedirectUrl(appConfig.limitedLiabilityPartnershipJourneyUrl)
                              .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                          case _ =>
                            Future(
                              Redirect(routes.OrganisationTypeNotSupportedController.onPageLoad())
                            )
                        }
                      case _ => Future(Redirect(commonRoutes.TaskListController.displayPage()))
                    }
                  case Left(error) => throw error
                }
        )
    }

  private def getRedirectUrl(
    url: String
  )(implicit request: JourneyRequest[AnyContent]): Future[String] =
    partnershipGrsConnector.createJourney(
      PartnershipGrsCreateRequest(appConfig.grsCallbackUrl,
                                  Some(request2Messages(request)("service.name")),
                                  appConfig.serviceIdentifier,
                                  appConfig.signOutLink,
                                  appConfig.accessibilityStatementServicePath
      ),
      url
    )

  private def updateRegistration(
    formData: PartnershipType
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    formData.answer match {
      case Some(partnershipType) =>
        update { registration =>
          registration.organisationDetails.partnershipDetails match {
            case Some(_) =>
              registration.copy(organisationDetails =
                registration.organisationDetails.copy(partnershipDetails =
                  Some(
                    registration.organisationDetails.partnershipDetails.get.copy(partnershipType =
                      partnershipType
                    )
                  )
                )
              )
            case _ =>
              registration.copy(organisationDetails =
                registration.organisationDetails.copy(partnershipDetails =
                  Some(PartnershipDetails(partnershipType = partnershipType))
                )
              )
          }
        }
      case _ => throw new IllegalStateException("No partnership type selected")
    }

}
