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
  RegisteredSocietyGrsConnector,
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
  GENERAL_PARTNERSHIP,
  LIMITED_LIABILITY_PARTNERSHIP,
  LIMITED_PARTNERSHIP,
  SCOTTISH_LIMITED_PARTNERSHIP,
  SCOTTISH_PARTNERSHIP
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.PartnershipDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.services.GRSRedirections
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation.partnership_type
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PartnershipTypeController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  val appConfig: AppConfig,
  val soleTraderGrsConnector: SoleTraderGrsConnector,
  val ukCompanyGrsConnector: UkCompanyGrsConnector,
  val partnershipGrsConnector: PartnershipGrsConnector,
  val registeredSocietyGrsConnector: RegisteredSocietyGrsConnector,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: partnership_type
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport with GRSRedirections {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      request.registration.organisationDetails.partnershipDetails match {
        case Some(partnershipDetails) =>
          Ok(page(PartnerType.form().fill(PartnerType(partnershipDetails.partnershipType))))
        case _ => Ok(page(PartnerType.form()))
      }
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      PartnerType.form()
        .bindFromRequest()
        .fold((formWithErrors: Form[PartnerType]) => Future(BadRequest(page(formWithErrors))),
              partnerType =>
                updateRegistration(partnerType).flatMap {
                  case Right(_) =>
                    FormAction.bindFromRequest match {
                      case SaveAndContinue =>
                        partnerType.answer match {
                          case GENERAL_PARTNERSHIP | SCOTTISH_PARTNERSHIP =>
                            Future(Redirect(routes.PartnershipNameController.displayPage().url))
                          case LIMITED_PARTNERSHIP =>
                            getPartnershipRedirectUrl(appConfig.limitedPartnershipJourneyUrl,
                                                      appConfig.grsCallbackUrl
                            )
                              .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                          case SCOTTISH_LIMITED_PARTNERSHIP =>
                            getPartnershipRedirectUrl(
                              appConfig.scottishLimitedPartnershipJourneyUrl,
                              appConfig.grsCallbackUrl
                            )
                              .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                          case LIMITED_LIABILITY_PARTNERSHIP =>
                            getPartnershipRedirectUrl(
                              appConfig.limitedLiabilityPartnershipJourneyUrl,
                              appConfig.grsCallbackUrl
                            )
                              .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                          case _ =>
                            Future(
                              Redirect(
                                organisationRoutes.RegisterAsOtherOrganisationController.onPageLoad()
                              )
                            )
                        }
                      case _ => Future(Redirect(commonRoutes.TaskListController.displayPage()))
                    }
                  case Left(error) => throw error
                }
        )
    }

  private def updateRegistration(
    formData: PartnerType
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] = {

    val partnershipType = formData.answer
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
  }

}
