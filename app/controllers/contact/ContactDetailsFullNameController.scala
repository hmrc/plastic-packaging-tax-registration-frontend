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

package controllers.contact

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import connectors.{RegistrationConnector, ServiceError}
import controllers.actions.NotEnrolledAuthAction
import controllers.actions.getRegistration.GetRegistrationAction
import controllers.{routes => commonRoutes}
import forms.contact.FullName
import models.registration.{Cacheable, Registration}
import models.request.JourneyRequest
import views.html.contact.full_name_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContactDetailsFullNameController @Inject() (
                                                   authenticate: NotEnrolledAuthAction,
                                                   journeyAction: GetRegistrationAction,
                                                   override val registrationConnector: RegistrationConnector,
                                                   mcc: MessagesControllerComponents,
                                                   page: full_name_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      request.registration.primaryContactDetails.name match {
        case Some(data) =>
          Ok(buildPage(FullName.form().fill(FullName(data))))
        case _ =>
          Ok(buildPage(FullName.form()))
      }
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      FullName.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[FullName]) =>
            Future.successful(
              BadRequest(buildPage(formWithErrors))
            ),
          fullName =>
            updateRegistration(fullName).map {
              case Right(_) => Redirect(routes.ContactDetailsJobTitleController.displayPage())
              case Left(error) => throw error
            }
        )
    }

  private def buildPage
  (
    form: Form[FullName]
  )(implicit request: JourneyRequest[AnyContent]) =
    page(
      form,
      routes.ContactDetailsFullNameController.submit(),
      request.registration.isGroup
    )

  private def updateRegistration(
    formData: FullName
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val updatedPrimaryContactDetails =
        registration.primaryContactDetails.copy(name = Some(formData.value))
      registration.copy(primaryContactDetails = updatedPrimaryContactDetails)
    }

}
