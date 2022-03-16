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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.group

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthActioning
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.{ActionEnum, OrgType, OrganisationType}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{GroupDetail, Registration, RegistrationUpdater}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{AuthenticatedRequest, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.group.organisation_type
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

abstract class OrganisationDetailsTypeControllerBase(
  val authenticate: AuthActioning,
  val journeyAction: ActionRefiner[AuthenticatedRequest, JourneyRequest],
  val appConfig: AppConfig,
  val page: organisation_type,
  val registrationUpdater: RegistrationUpdater,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with OrganisationDetailsTypeHelper {

  protected def doDisplayPage(memberId: Option[String], submitCall: Call): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      val isFirstMember: Boolean = request.registration.isFirstGroupMember
      memberId match {
        case Some(memberId) =>
          val organisationType: String = request.registration.findMember(memberId).flatMap(
            _.organisationDetails.map(_.organisationType)
          ).getOrElse(throw new IllegalStateException("Organisation type is absent"))
          Ok(
            page(form = OrganisationType.form(ActionEnum.Group).fill(
                   OrganisationType(OrgType.withNameOpt(organisationType))
                 ),
                 isFirstMember,
                 Some(memberId),
                 submitCall
            )
          )
        case None =>
          Ok(page(form = OrganisationType.form(ActionEnum.Group), isFirstMember, memberId, submitCall))
      }

    }

  protected def doSubmit(memberId: Option[String], submitCall: Call): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      val isFirstMember: Boolean = request.registration.isFirstGroupMember
      OrganisationType.form(ActionEnum.Group)
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[OrganisationType]) =>
            Future(
              BadRequest(
                page(form = formWithErrors,
                     request.registration.isFirstGroupMember,
                     memberId,
                     submitCall
                )
              )
            ),
          organisationType =>
            updateRegistration(organisationType).flatMap { registration =>
              handleOrganisationType(organisationType, false, memberId)(
                new JourneyRequest[AnyContent](authenticatedRequest = request.authenticatedRequest,
                                               registration = registration,
                                               appConfig = appConfig
                ),
                ec,
                hc
              )
            }
        )
    }

  private def updateRegistration(
    formData: OrganisationType
  )(implicit req: JourneyRequest[AnyContent]): Future[Registration] =
    registrationUpdater.updateRegistration { registration =>
      val updatedGroupDetail: Option[GroupDetail] = registration.groupDetail.map { details =>
        details.copy(currentMemberOrganisationType = formData.answer)
      }
      registration.copy(groupDetail = updatedGroupDetail)
    }

  override def grsCallbackUrl(organisationId: Option[String]): String =
    appConfig.groupMemberGrsCallbackUrl(organisationId)

}
