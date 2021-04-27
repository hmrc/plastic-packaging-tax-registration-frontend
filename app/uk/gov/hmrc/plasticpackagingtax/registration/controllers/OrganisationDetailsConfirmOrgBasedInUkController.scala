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

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{RegistrationConnector, ServiceError}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{
  AuthAction,
  FormAction,
  SaveAndContinue
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.ConfirmOrganisationBasedInUk
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.confirm_organisation_based_in_uk
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OrganisationDetailsConfirmOrgBasedInUkController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: confirm_organisation_based_in_uk
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      request.registration.organisationDetails.isBasedInUk match {
        case Some(data) =>
          Future(
            Ok(
              page(
                ConfirmOrganisationBasedInUk.form().fill(ConfirmOrganisationBasedInUk(Some(data)))
              )
            )
          )
        case _ => Future(Ok(page(ConfirmOrganisationBasedInUk.form())))
      }
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      ConfirmOrganisationBasedInUk.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[ConfirmOrganisationBasedInUk]) =>
            Future(BadRequest(page(formWithErrors))),
          confirmOrganisationBasedInUk =>
            updateRegistration(confirmOrganisationBasedInUk).map {
              case Right(_) =>
                FormAction.bindFromRequest match {
                  case SaveAndContinue =>
                    if (confirmOrganisationBasedInUk.answer.getOrElse(false))
                      Redirect(routes.OrganisationDetailsTypeController.displayPage())
                    else Redirect(routes.OrganisationTypeNotSupportedController.onPageLoad())
                  case _ => Redirect(routes.RegistrationController.displayPage())
                }
              case Left(error) => throw error
            }
        )

    }

  private def updateRegistration(
    formData: ConfirmOrganisationBasedInUk
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val updatedOrganisationDetails =
        registration.organisationDetails.copy(isBasedInUk = formData.answer)
      registration.copy(organisationDetails = updatedOrganisationDetails)
    }

}
