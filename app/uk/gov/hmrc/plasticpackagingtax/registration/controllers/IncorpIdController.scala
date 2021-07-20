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

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{
  IncorpIdConnector,
  PartnershipConnector,
  RegistrationConnector,
  ServiceError,
  SoleTraderInorpIdConnector
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrgType
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncorpIdController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  ukLimitedConnector: IncorpIdConnector,
  soleTraderConnector: SoleTraderInorpIdConnector,
  partnershipConnector: PartnershipConnector,
  mcc: MessagesControllerComponents
)(implicit val executionContext: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def incorpIdCallback(journeyId: String): Action[AnyContent] =
    (authenticate andThen journeyAction).async {
      implicit request =>
        saveRegistrationDetails(journeyId).flatMap(res => res)
          .map {
            case Right(_)    => Redirect(routes.RegistrationController.displayPage())
            case Left(error) => throw error
          }
    }

  private def saveRegistrationDetails(journeyId: String)(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Future[Either[ServiceError, Registration]]] =
    request.registration.organisationDetails.organisationType match {
      case Some(OrgType.UK_COMPANY)  => updateIncorporationDetails(journeyId)
      case Some(OrgType.SOLE_TRADER) => updateSoleTraderDetails(journeyId)
      case Some(OrgType.PARTNERSHIP) => updatePartnershipDetails(journeyId)
      case _                         => throw new InternalServerException(s"Invalid organisation type")
    }

  private def updateIncorporationDetails(journeyId: String)(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Future[Either[ServiceError, Registration]]] =
    for {
      details <- ukLimitedConnector.getDetails(journeyId)
      result = update { model =>
        val updatedOrgDetails = model.organisationDetails.copy(incorporationDetails = Some(details),
                                                               soleTraderDetails = None,
                                                               partnershipDetails = None
        )
        model.copy(incorpJourneyId = Some(journeyId), organisationDetails = updatedOrgDetails)
      }
    } yield result

  private def updateSoleTraderDetails(journeyId: String)(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Future[Either[ServiceError, Registration]]] =
    for {
      details <- soleTraderConnector.getDetails(journeyId)
      result = update { model =>
        val updatedOrgDetails = model.organisationDetails.copy(incorporationDetails = None,
                                                               partnershipDetails = None,
                                                               soleTraderDetails = Some(details)
        )
        model.copy(incorpJourneyId = Some(journeyId), organisationDetails = updatedOrgDetails)
      }
    } yield result

  private def updatePartnershipDetails(journeyId: String)(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Future[Either[ServiceError, Registration]]] =
    for {
      partnershipDetails <- partnershipConnector.getDetails(journeyId)
      result = update { model =>
        val updatedOrgDetails = model.organisationDetails.copy(incorporationDetails = None,
                                                               soleTraderDetails = None,
                                                               partnershipDetails =
                                                                 Some(partnershipDetails)
        )
        model.copy(incorpJourneyId = Some(journeyId), organisationDetails = updatedOrgDetails)
      }
    } yield result

}
