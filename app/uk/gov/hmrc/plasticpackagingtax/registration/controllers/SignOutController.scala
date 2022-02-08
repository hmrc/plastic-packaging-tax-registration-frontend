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

import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.session_timed_out
import uk.gov.hmrc.plasticpackagingtax.registration.views.models.SignOutReason
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

class SignOutController @Inject() (
  appConfig: AppConfig,
  sessionTimedOut: session_timed_out,
  mcc: MessagesControllerComponents
) extends FrontendController(mcc) with I18nSupport {

  def signOut(signOutReason: SignOutReason): Action[AnyContent] =
    Action {
      signOutReason match {
        case SignOutReason.SessionTimeout =>
          Redirect(routes.SignOutController.sessionTimeoutSignedOut()).withNewSession
        case SignOutReason.UserAction => Redirect(appConfig.exitSurveyUrl).withNewSession
      }
    }

  def sessionTimeoutSignedOut: Action[AnyContent] =
    Action { implicit request =>
      Ok(sessionTimedOut())
    }

}
