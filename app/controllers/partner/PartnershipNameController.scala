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

package controllers.partner

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import config.AppConfig
import connectors._
import connectors.grs.PartnershipGrsConnector
import controllers.actions.NotEnrolledAuthAction
import forms.organisation.PartnerTypeEnum.{
  GENERAL_PARTNERSHIP,
  SCOTTISH_PARTNERSHIP
}
import forms.organisation.PartnershipName
import models.genericregistration.PartnershipGrsCreateRequest
import models.registration.{Cacheable, Registration}
import models.request.{JourneyAction, JourneyRequest}
import views.html.organisation.partnership_name
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PartnershipNameController @Inject() (
                                            authenticate: NotEnrolledAuthAction,
                                            journeyAction: JourneyAction,
                                            appConfig: AppConfig,
                                            partnershipGrsConnector: PartnershipGrsConnector,
                                            override val registrationConnector: RegistrationConnector,
                                            mcc: MessagesControllerComponents,
                                            page: partnership_name
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      request.registration.organisationDetails.partnershipDetails match {
        case Some(partnershipDetails) =>
          partnershipDetails.partnershipName match {
            case Some(partnershipName) =>
              Ok(page(PartnershipName.form().fill(PartnershipName(partnershipName))))
            case _ => Ok(page(PartnershipName.form()))
          }
        case _ => throw new IllegalStateException("Partnership details must be present")
      }
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      PartnershipName.form()
        .bindFromRequest()
        .fold((formWithErrors: Form[PartnershipName]) => Future(BadRequest(page(formWithErrors))),
              partnershipName =>
                updatePartnershipName(partnershipName.value).flatMap { registration =>
                  getGrsRedirectUrl(getPartnershipType(registration) match {
                    case GENERAL_PARTNERSHIP  => appConfig.generalPartnershipJourneyUrl
                    case SCOTTISH_PARTNERSHIP => appConfig.scottishPartnershipJourneyUrl
                    case _                    => throw new IllegalStateException("Unexpected partnership type")
                  }).map(grsRedirectUrl => Redirect(grsRedirectUrl))
                }
        )
    }

  private def getPartnershipType(registration: Registration) =
    registration.organisationDetails.partnershipDetails.map(pd => pd.partnershipType).getOrElse(
      throw new IllegalStateException("Assumed partnership details missing")
    )

  private def updatePartnershipName(
    partnershipName: String
  )(implicit hc: HeaderCarrier, request: JourneyRequest[AnyContent]): Future[Registration] =
    update {
      registration =>
        registration.copy(organisationDetails =
          registration.organisationDetails.copy(partnershipDetails =
            registration.organisationDetails.partnershipDetails.map {
              pd => pd.copy(partnershipName = Some(partnershipName))
            }
          )
        )
    }.map {
      case Right(registration) => registration
      case Left(ex)            => throw ex
    }

  private def getGrsRedirectUrl(
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

}
