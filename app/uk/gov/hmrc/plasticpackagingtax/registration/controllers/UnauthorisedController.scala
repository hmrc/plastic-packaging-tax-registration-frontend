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

import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.unauthorised
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.unauthorised_not_admin
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject

class UnauthorisedController @Inject() (
  mcc: MessagesControllerComponents,
  unauthorisedPage: unauthorised,
  unauthorisedNotAdminPage: unauthorised_not_admin
) extends FrontendController(mcc) with I18nSupport {

  private val logger = Logger(this.getClass)

  def onPageLoad(nonAdminCredRole: Boolean = false): Action[AnyContent] =
    Action { implicit request =>
      if (nonAdminCredRole) {
        logger.info(s"User without 'User' credential role - showing unauthorised page")
        Ok(unauthorisedNotAdminPage())
      } else
        Ok(unauthorisedPage())
    }

}
