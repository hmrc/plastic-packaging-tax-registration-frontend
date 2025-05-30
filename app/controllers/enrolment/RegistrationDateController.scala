/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.enrolment

import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import controllers.actions.auth.RegistrationAuthAction
import forms.enrolment.RegistrationDate
import models.registration.UserEnrolmentDetails
import repositories.UserEnrolmentDetailsRepository
import views.html.enrolment.registration_date_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationDateController @Inject() (
  authenticate: RegistrationAuthAction,
  mcc: MessagesControllerComponents,
  cache: UserEnrolmentDetailsRepository,
  page: registration_date_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport {

  def displayPage(): Action[AnyContent] =
    authenticate.async { implicit request =>
      cache.get().map { data =>
        data.registrationDate match {
          case Some(registrationDate) =>
            Ok(page(RegistrationDate.form().fill(registrationDate), previousPage(data)))
          case _ => Ok(page(RegistrationDate.form(), previousPage(data)))
        }
      }
    }

  def submit(): Action[AnyContent] =
    authenticate.async { implicit request =>
      cache.get().flatMap { data =>
        RegistrationDate.form()
          .bindFromRequest()
          .fold(
            (formWithErrors: Form[RegistrationDate]) =>
              Future.successful(BadRequest(page(formWithErrors, previousPage(data)))),
            registrationDate =>
              cache.update(data => data.copy(registrationDate = Some(registrationDate))).map { _ =>
                Redirect(routes.CheckAnswersController.displayPage())
              }
          )

      }

    }

  private def previousPage(data: UserEnrolmentDetails): Call =
    data.postcode match {
      case Some(_) => routes.PostcodeController.displayPage()
      case _       => routes.IsUkAddressController.displayPage()
    }

}
