/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.actions.auth.RegistrationAuthAction
import views.html.organisation.register_as_other_organisation
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject

class RegisterAsOtherOrganisationController @Inject() (mcc: MessagesControllerComponents, page: register_as_other_organisation, authenticate: RegistrationAuthAction)
    extends FrontendController(mcc) with I18nSupport {

  def onPageLoad: Action[AnyContent] =
    authenticate { implicit request =>
      Ok(page())
    }

}
