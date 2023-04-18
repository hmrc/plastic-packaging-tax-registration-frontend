/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.organisation

import audit.Auditor
import config.AppConfig
import connectors._
import connectors.grs._
import controllers.actions.JourneyAction
import controllers.group.OrganisationDetailsTypeHelper
import forms.organisation.{ActionEnum, OrgType, OrganisationType, PartnerTypeEnum}
import models.genericregistration.PartnershipDetails
import models.registration.{Cacheable, NewRegistrationUpdateService, OrganisationDetails, Registration}
import models.request.JourneyRequest
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.organisation.organisation_type

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OrganisationDetailsTypeController @Inject() (
                                                    auditor: Auditor,
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

  def displayPageRepresentativeMember(): Action[AnyContent] =
    doDisplayPage(ActionEnum.RepresentativeMember)

  def displayPage(): Action[AnyContent]                = doDisplayPage(ActionEnum.Org)
  def submitRepresentativeMember(): Action[AnyContent] = doSubmit(ActionEnum.RepresentativeMember)
  def submit(): Action[AnyContent]                     = doSubmit(ActionEnum.Org)

  private def doDisplayPage(action: ActionEnum.Type) =
    journeyAction.register.async { implicit request =>
      request.registration.organisationDetails.organisationType match {
        case Some(data) =>
          Future(
            Ok(
              page(form = OrganisationType.form(action).fill(OrganisationType(Some(data))),
                   isGroup = request.registration.isGroup
              )
            )
          )
        case _ =>
          Future(
            Ok(page(form = OrganisationType.form(action), isGroup = request.registration.isGroup))
          )
      }
    }

  private def doSubmit(action: ActionEnum.Type) =
    journeyAction.register.async { implicit request =>
      OrganisationType.form(action)
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[OrganisationType]) =>
            Future(BadRequest(page(form = formWithErrors, isGroup = request.registration.isGroup))),
          organisationType =>
            updateRegistration(organisationType).flatMap {
              case Right(_) =>
                auditor.orgTypeSelected(request.authenticatedRequest.internalID,
                                        organisationType.answer
                )
                handleOrganisationType(organisationType, true, None)
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
