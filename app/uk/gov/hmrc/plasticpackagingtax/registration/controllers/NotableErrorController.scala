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

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.{error_no_save_page, error_page}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}

@Singleton
class NotableErrorController @Inject() (
  mcc: MessagesControllerComponents,
  errorPage: error_page,
  errorNoSavePage: error_no_save_page
) extends FrontendController(mcc) with I18nSupport {

  def subscriptionFailure(): Action[AnyContent] =
    Action { implicit request =>
      Ok(errorPage())
    }

  def enrolmentFailure(): Action[AnyContent] =
    Action { implicit request =>
      Ok(errorNoSavePage())
    }

}