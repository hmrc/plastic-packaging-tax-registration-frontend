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

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.forms.enrolment.IsUkAddress
import uk.gov.hmrc.plasticpackagingtax.registration.repositories.UserEnrolmentDetailsRepository
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.enrolment.is_uk_address_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IsUkAddressController @Inject() (
  authenticate: AuthAction,
  mcc: MessagesControllerComponents,
  cache: UserEnrolmentDetailsRepository,
  page: is_uk_address_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  def displayPage(): Action[AnyContent] =
    authenticate.async { implicit request =>
      cache.get().map {
        case Some(userEnrolmentDetails) if userEnrolmentDetails.isUkAddress.isDefined =>
          Ok(page(IsUkAddress.form().fill(userEnrolmentDetails.isUkAddress.get)))
        case _ => Ok(page(IsUkAddress.form()))
      }
    }

  def submit(): Action[AnyContent] =
    authenticate.async { implicit request =>
      IsUkAddress.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[IsUkAddress]) =>
            Future.successful(BadRequest(page(formWithErrors))),
          isUkAddress =>
            cache.update(data => data.copy(isUkAddress = Some(isUkAddress))).map {
              userEnrolmentDetails =>
                userEnrolmentDetails.isUkAddress match {
                  case Some(IsUkAddress(Some(true))) =>
                    Redirect(routes.PostcodeController.displayPage())
                  // TODO: route to date of registration when available
                  case _ => Redirect(routes.IsUkAddressController.displayPage())
                }
            }
        )
    }

}