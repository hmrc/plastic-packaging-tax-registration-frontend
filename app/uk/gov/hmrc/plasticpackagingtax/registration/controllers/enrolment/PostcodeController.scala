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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.enrolment

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.forms.enrolment.Postcode
import uk.gov.hmrc.plasticpackagingtax.registration.repositories.UserEnrolmentDetailsRepository
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.enrolment.postcode_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PostcodeController @Inject() (
  authenticate: AuthAction,
  mcc: MessagesControllerComponents,
  cache: UserEnrolmentDetailsRepository,
  page: postcode_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  def displayPage(): Action[AnyContent] =
    authenticate.async { implicit request =>
      cache.get().map {
        case Some(data) if data.postcode.isDefined =>
          Ok(page(Postcode.form().fill(data.postcode.get)))
        case _ => Ok(page(Postcode.form()))
      }
    }

  def submit(): Action[AnyContent] =
    authenticate.async { implicit request =>
      Postcode.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[Postcode]) => Future.successful(BadRequest(page(formWithErrors))),
          postcode =>
            cache.update(data => data.copy(postcode = Some(postcode))).map {
              _ => Redirect(routes.PostcodeController.displayPage())
            }
        )
    }

}