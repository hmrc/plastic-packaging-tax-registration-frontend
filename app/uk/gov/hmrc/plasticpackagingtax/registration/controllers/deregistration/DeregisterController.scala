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

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.EnrolledAuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.forms.deregistration.DeregisterForm
import uk.gov.hmrc.plasticpackagingtax.registration.repositories.DeregistrationDetailRepository
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.deregistration.deregister_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeregisterController @Inject() (
                                       authenticate: EnrolledAuthAction,
                                       mcc: MessagesControllerComponents,
                                       deregistrationDetailRepository: DeregistrationDetailRepository,
                                       appConfig: AppConfig,
                                       page: deregister_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  def displayPage(): Action[AnyContent] =
    authenticate.async { implicit request =>
      deregistrationDetailRepository.get().map { deregistrationDetails =>
        val form = deregistrationDetails.deregister.map(DeregisterForm.form().fill(_)).getOrElse(
          DeregisterForm.form()
        )
        Ok(page(form))
      }
    }

  def submit(): Action[AnyContent] =
    authenticate.async { implicit request =>
      DeregisterForm.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[Boolean]) => Future.successful(BadRequest(page(formWithErrors))),
          deregister =>
            deregistrationDetailRepository.update(
              deregistrationDetails => deregistrationDetails.copy(deregister = Some(deregister))
            ).map { deregistrationDetails =>
              if (deregistrationDetails.deregister.contains(true))
                Redirect(routes.DeregisterReasonController.displayPage())
              else
                Redirect(appConfig.pptAccountUrl)
            }
        )
    }

}
