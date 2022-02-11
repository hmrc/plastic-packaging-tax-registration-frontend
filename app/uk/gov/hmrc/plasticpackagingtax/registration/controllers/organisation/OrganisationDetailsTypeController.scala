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
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs._
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions._
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.group.OrganisationDetailsTypeHelper
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.{
  OrgType,
  OrganisationType,
  PartnerTypeEnum
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.PartnershipDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  Cacheable,
  NewRegistrationUpdateService,
  OrganisationDetails,
  Registration
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation.organisation_type
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OrganisationDetailsTypeController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val appConfig: AppConfig,
  override val soleTraderGrsConnector: SoleTraderGrsConnector,
  override val ukCompanyGrsConnector: UkCompanyGrsConnector,
  override val partnershipGrsConnector: PartnershipGrsConnector,
  override val registeredSocietyGrsConnector: RegisteredSocietyGrsConnector,
  override val registrationConnector: RegistrationConnector,
  override val registrationUpdater: NewRegistrationUpdateService,
  mcc: MessagesControllerComponents,
  page: organisation_type
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport
    with OrganisationDetailsTypeHelper {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      request.registration.organisationDetails.organisationType match {
        case Some(data) =>
          Future(
            Ok(
              page(form = OrganisationType.form().fill(OrganisationType(Some(data))),
                   isGroup = request.registration.isGroup
              )
            )
          )
        case _ =>
          Future(Ok(page(form = OrganisationType.form(), isGroup = request.registration.isGroup)))
      }
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      OrganisationType.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[OrganisationType]) =>
            Future(BadRequest(page(form = formWithErrors, isGroup = request.registration.isGroup))),
          organisationType =>
            updateRegistration(organisationType).flatMap {
              case Right(_) =>
                FormAction.bindFromRequest match {
                  case SaveAndContinue =>
                    handleOrganisationType(organisationType, true, None)
                  case _ => Future(Redirect(commonRoutes.TaskListController.displayPage()))
                }
              case Left(error) => throw error
            }
        )

    }

  private def updateRegistration(
    formData: OrganisationType
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      if (registration.isGroup && formData.answer.exists(_.equals(OrgType.PARTNERSHIP))) {
        val updatedOrganisationDetails: OrganisationDetails =
          registration.organisationDetails.partnershipDetails match {
            case Some(_) =>
              registration.organisationDetails.copy(
                partnershipDetails =
                  Some(
                    registration.organisationDetails.partnershipDetails.get.copy(partnershipType =
                      PartnerTypeEnum.LIMITED_LIABILITY_PARTNERSHIP
                    )
                  ),
                organisationType = formData.answer
              )
            case _ =>
              registration.organisationDetails.copy(
                partnershipDetails =
                  Some(
                    PartnershipDetails(partnershipType =
                      PartnerTypeEnum.LIMITED_LIABILITY_PARTNERSHIP
                    )
                  ),
                organisationType = formData.answer
              )
          }
        registration.copy(organisationDetails = updatedOrganisationDetails)
      } else {
        val updatedOrganisationDetails =
          registration.organisationDetails.copy(organisationType = formData.answer)
        registration.copy(organisationDetails = updatedOrganisationDetails)
      }

    }

  override def grsCallbackUrl(organisationId: Option[String]): String =
    appConfig.grsCallbackUrl

}
