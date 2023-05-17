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

package controllers.amendment

import controllers.actions.auth.AmendAuthAction
import play.api.mvc._
import controllers.actions.JourneyAction
import services.AmendRegistrationService
import views.html.amendment.amend_registration_page
import views.html.partials.amendment.amend_error_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AmendRegistrationController @Inject() (
                                              journeyAction: JourneyAction,
                                              amendRegistrationService: AmendRegistrationService,
                                              amendAuthAction: AmendAuthAction,
                                              mcc: MessagesControllerComponents,
                                              amendRegistrationPage: amend_registration_page,
                                              amendErrorPage: amend_error_page
)(implicit ec: ExecutionContext)
    extends AmendmentController(mcc, amendRegistrationService) {

  def displayPage(): Action[AnyContent] =
    journeyAction.amend { implicit request =>
      Ok(amendRegistrationPage(request.registration))
    }

  def registrationUpdateFailed(): Action[AnyContent] =
    amendAuthAction { implicit request =>
      Ok(amendErrorPage())
    }

}
