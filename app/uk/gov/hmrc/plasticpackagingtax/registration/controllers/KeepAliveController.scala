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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{
  AuthActioning,
  AuthNoEnrolmentCheckAction
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{
  KeepAliveAction,
  KeepAliveActionImpl
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.unauthorised
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject

class KeepAliveController @Inject() (
  mcc: MessagesControllerComponents,
  keepAliveAction: KeepAliveAction,
  authenticate: AuthNoEnrolmentCheckAction
) extends FrontendController(mcc) with I18nSupport {

  def keepAlive(): Action[AnyContent] =
    (authenticate andThen keepAliveAction) { implicit request =>
      Ok(request.uri)
    }

}
