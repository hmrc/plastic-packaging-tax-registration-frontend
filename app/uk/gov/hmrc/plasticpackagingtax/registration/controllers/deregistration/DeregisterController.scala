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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.deregistration

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthNoEnrolmentCheckAction
import uk.gov.hmrc.plasticpackagingtax.registration.models.deregistration.DeregistrationDetails
import uk.gov.hmrc.plasticpackagingtax.registration.repositories.DeregistrationDetailRepository
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.deregistration.deregister_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class DeregisterController @Inject() (
  authenticate: AuthNoEnrolmentCheckAction,
  mcc: MessagesControllerComponents,
  deregistrationDetailRepository: DeregistrationDetailRepository,
  page: deregister_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  def displayPage(): Action[AnyContent] =
    authenticate.async { implicit request =>
      deregistrationDetailRepository.put(DeregistrationDetails(None, None)).map { _ =>
        Ok(page())
      }
    }

  def submit(): Action[AnyContent] =
    authenticate.async { implicit request =>
      deregistrationDetailRepository.update(
        deregistrationDetails =>
          // TODO: update here
          deregistrationDetails
      ).map { _ =>
        Redirect(routes.DeregisterReasonController.displayPage())
      }
    }

}
