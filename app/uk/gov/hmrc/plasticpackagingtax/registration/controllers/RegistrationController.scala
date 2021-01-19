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
import play.api.mvc._
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.registration_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.model.{TaskName, TaskStatus}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}

@Singleton
class RegistrationController @Inject() (
  authenticate: AuthAction,
  mcc: MessagesControllerComponents,
  registration_page: registration_page
) extends FrontendController(mcc) with I18nSupport {

  val taskStatuses: Map[TaskName, TaskStatus] = Map(
    TaskName.OrganisationDetails     -> TaskStatus.Completed,
    TaskName.PlasticPackagingDetails -> TaskStatus.InProgress,
    TaskName.BusinessContactDetails  -> TaskStatus.NotStarted
  )

  def displayPage(): Action[AnyContent] =
    authenticate { implicit request =>
      Ok(registration_page(taskStatuses))
    }

}
