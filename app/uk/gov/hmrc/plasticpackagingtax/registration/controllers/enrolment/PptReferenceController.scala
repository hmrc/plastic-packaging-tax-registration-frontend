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

import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.forms.enrolment.PptReference
import uk.gov.hmrc.plasticpackagingtax.registration.repositories.UserDataRepository
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.enrolment.ppt_reference_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PptReferenceController @Inject() (
  authenticate: AuthAction,
  mcc: MessagesControllerComponents,
  cache: UserDataRepository,
  page: ppt_reference_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  def displayPage(): Action[AnyContent] =
    authenticate.async { implicit request =>
      cache.getData[PptReference]("pptReference").map {
        case Some(reference) =>
          Ok(page(PptReference.form().fill(reference)))
        case _ => Ok(page(PptReference.form()))
      }
    }

  def submit(): Action[AnyContent] =
    authenticate.async { implicit request =>
      PptReference.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[PptReference]) =>
            Future.successful(BadRequest(page(formWithErrors))),
          pptReference =>
            cache.putData[PptReference]("pptReference", pptReference).map {
              ref => Ok(page(PptReference.form().fill(ref)))
            }
        )
    }

}
