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

import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors._
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs.{
  GeneralPartnershipGrsConnector,
  ScottishPartnershipGrsConnector,
  SoleTraderGrsConnector,
  UkCompanyGrsConnector
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrgType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.PartnershipTypeEnum.{
  GENERAL_PARTNERSHIP,
  PartnershipTypeEnum,
  SCOTTISH_PARTNERSHIP
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  GeneralPartnershipDetails,
  PartnershipDetails,
  ScottishPartnershipDetails
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  Cacheable,
  OrganisationDetails,
  Registration
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GrsController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  ukCompanyGrsConnector: UkCompanyGrsConnector,
  soleTraderGrsConnector: SoleTraderGrsConnector,
  generalPartnershipGrsConnector: GeneralPartnershipGrsConnector,
  scottishPartnershipGrsConnector: ScottishPartnershipGrsConnector,
  mcc: MessagesControllerComponents
)(implicit val executionContext: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def grsCallback(journeyId: String): Action[AnyContent] =
    (authenticate andThen journeyAction).async {
      implicit request =>
        saveRegistrationDetails(journeyId).map {
          case Right(registration) =>
            if (registration.organisationDetails.businessVerificationFailed)
              Redirect(routes.NotableErrorController.businessVerificationFailure())
            else if (registration.organisationDetails.businessPartnerId().isDefined)
              Redirect(routes.RegistrationController.displayPage())
            else
              Redirect(routes.NotableErrorController.grsFailure())
          case Left(error) => throw error
        }
    }

  private def saveRegistrationDetails(journeyId: String)(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] =
    request.registration.organisationDetails.organisationType match {
      case Some(OrgType.UK_COMPANY)  => updateIncorporationDetails(journeyId)
      case Some(OrgType.SOLE_TRADER) => updateSoleTraderDetails(journeyId)
      case Some(OrgType.PARTNERSHIP) => updatePartnershipDetails(journeyId)
      case _                         => throw new InternalServerException(s"Invalid organisation type")
    }

  private def updateIncorporationDetails(journeyId: String)(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] =
    for {
      details <- ukCompanyGrsConnector.getDetails(journeyId)
      result <- update { model =>
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
  ): Future[Either[ServiceError, Registration]] =
    for {
      details <- soleTraderGrsConnector.getDetails(journeyId)
      result <- update { model =>
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
  ): Future[Either[ServiceError, Registration]] =
    request.registration.organisationDetails.partnershipDetails match {
      case Some(partnershipDetails) =>
        partnershipDetails.partnershipType match {
          case GENERAL_PARTNERSHIP =>
            for {
              generalPartnershipDetails <- generalPartnershipGrsConnector.getDetails(journeyId)
              result <- update { registration =>
                val updatedOrgDetails = mergePartnershipDetails(organisationDetails =
                                                                  registration.organisationDetails,
                                                                generalPartnershipDetails =
                                                                  Some(generalPartnershipDetails),
                                                                scottishPartnershipDetails = None,
                                                                GENERAL_PARTNERSHIP
                )
                registration.copy(incorpJourneyId = Some(journeyId),
                                  organisationDetails = updatedOrgDetails
                )
              }
            } yield result
          case SCOTTISH_PARTNERSHIP =>
            for {
              scottishPartnershipDetails <- scottishPartnershipGrsConnector.getDetails(journeyId)
              result <- update { registration =>
                val updatedOrgDetails = mergePartnershipDetails(organisationDetails =
                                                                  registration.organisationDetails,
                                                                generalPartnershipDetails = None,
                                                                scottishPartnershipDetails =
                                                                  Some(scottishPartnershipDetails),
                                                                SCOTTISH_PARTNERSHIP
                )
                registration.copy(incorpJourneyId = Some(journeyId),
                                  organisationDetails = updatedOrgDetails
                )
              }
            } yield result
          case _ => throw new IllegalStateException("Unsupported partnership type")
        }
      case _ => throw new IllegalStateException("Missing partnership details")
    }

  private def mergePartnershipDetails(
    organisationDetails: OrganisationDetails,
    generalPartnershipDetails: Option[GeneralPartnershipDetails],
    scottishPartnershipDetails: Option[ScottishPartnershipDetails],
    partnershipTypeEnum: PartnershipTypeEnum
  ): OrganisationDetails = {
    val updatedPartnershipDetails: PartnershipDetails = organisationDetails.partnershipDetails.fold(
      PartnershipDetails(partnershipType = partnershipTypeEnum,
                         partnershipName = None,
                         generalPartnershipDetails = generalPartnershipDetails,
                         scottishPartnershipDetails = scottishPartnershipDetails
      )
    )(
      partnershipDetails =>
        partnershipDetails.copy(generalPartnershipDetails = generalPartnershipDetails,
                                scottishPartnershipDetails = scottishPartnershipDetails
        )
    )
    organisationDetails.copy(partnershipDetails = Some(updatedPartnershipDetails))
  }

}
