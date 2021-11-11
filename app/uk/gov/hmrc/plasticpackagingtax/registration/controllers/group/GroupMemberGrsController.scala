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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.group

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors._
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs._
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrgType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrgType.OrgType
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.IncorporationDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.{
  GroupMember,
  OrganisationDetails
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  Cacheable,
  GroupDetail,
  Registration
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GroupMemberGrsController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  ukCompanyGrsConnector: UkCompanyGrsConnector,
  mcc: MessagesControllerComponents
)(implicit val executionContext: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def grsCallback(journeyId: String): Action[AnyContent] =
    (authenticate andThen journeyAction).async {
      implicit request =>
        saveRegistrationDetails(journeyId).flatMap {
          case Right(_) =>
            Future.successful(Redirect(routes.OrganisationDetailsTypeController.displayPage()))
          case Left(error) => throw error
        }
    }

  private def saveRegistrationDetails(journeyId: String)(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] = {
    val orgType: Option[OrgType] =
      request.registration.groupDetail.flatMap(_.currentMemberOrganisationType)
    orgType match {
      case Some(OrgType.UK_COMPANY) => updateUkCompanyDetails(journeyId, OrgType.UK_COMPANY)
      case _                        => throw new InternalServerException(s"Invalid organisation type")
    }
  }

  private def updateUkCompanyDetails(journeyId: String, orgType: OrgType)(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] =
    updateGroupMemberDetails(journeyId, orgType, ukCompanyGrsConnector.getDetails)

  private def updateGroupMemberDetails(
    journeyId: String,
    orgType: OrgType,
    getDetails: String => Future[IncorporationDetails]
  )(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] =
    for {
      details <- getDetails(journeyId)
      result <- update { registration =>
        val updatedGroupDetails: GroupDetail = registration.groupDetail match {
          case Some(value) =>
            val members: Seq[GroupMember] = value.members :+ addGroupMember(details, orgType)
            value.copy(members = members, currentMemberOrganisationType = None)
          case None => throw new InternalServerException(s"No group detail")
        }
        registration.copy(groupDetail = Some(updatedGroupDetails))
      }
    } yield result

  private def addGroupMember(details: IncorporationDetails, orgType: OrgType): GroupMember =
    GroupMember(customerIdentification1 = details.companyNumber,
                customerIdentification2 = Some(details.ctutr),
                organisationDetails =
                  Some(OrganisationDetails(orgType.toString, details.companyName)),
                addressDetails = details.companyAddress.toGroupAddressDetails
    )

}
