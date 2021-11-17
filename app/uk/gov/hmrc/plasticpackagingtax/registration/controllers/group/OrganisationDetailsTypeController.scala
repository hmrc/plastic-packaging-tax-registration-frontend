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

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.connectors._
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs.{
  RegisteredSocietyGrsConnector,
  SoleTraderGrsConnector,
  UkCompanyGrsConnector
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{
  AuthAction,
  FormAction,
  SaveAndContinue
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => pptRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrganisationType
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  Cacheable,
  GroupDetail,
  Registration
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.group.organisation_type
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
  override val registeredSocietyGrsConnector: RegisteredSocietyGrsConnector,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: organisation_type
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport
    with OrganisationDetailsTypeHelper {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      val isFirstMember: Boolean = request.registration.isFirstGroupMember
      Future(Ok(page(form = OrganisationType.form(), isFirstMember)))
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      OrganisationType.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[OrganisationType]) =>
            Future(
              BadRequest(page(form = formWithErrors, request.registration.isFirstGroupMember))
            ),
          organisationType =>
            updateRegistration(organisationType).flatMap {
              case Right(_) =>
                FormAction.bindFromRequest match {
                  case SaveAndContinue =>
                    handleOrganisationType(organisationType)
                  case _ => Future(Redirect(pptRoutes.TaskListController.displayPage()))
                }
              case Left(error) => throw error
            }
        )
    }

  private def updateRegistration(
    formData: OrganisationType
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val updatedGroupDetail: Option[GroupDetail] = registration.groupDetail.map { details =>
        details.copy(currentMemberOrganisationType = formData.answer)
      }
      registration.copy(groupDetail = updatedGroupDetail)
    }

  override def grsCallbackUrl(): String =
    appConfig.groupMemberGrsCallbackUrl

}
