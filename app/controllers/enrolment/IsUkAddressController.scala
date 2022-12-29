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

package controllers.enrolment

import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.actions.NotEnrolledAuthAction
import forms.enrolment.IsUkAddress
import repositories.UserEnrolmentDetailsRepository
import views.html.enrolment.is_uk_address_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IsUkAddressController @Inject() (
                                        authenticate: NotEnrolledAuthAction,
                                        mcc: MessagesControllerComponents,
                                        cache: UserEnrolmentDetailsRepository,
                                        page: is_uk_address_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  def displayPage(): Action[AnyContent] =
    authenticate.async { implicit request =>
      cache.get().map { data =>
        data.isUkAddress match {
          case Some(isUkAddress) => Ok(page(IsUkAddress.form().fill(isUkAddress)))
          case _                 => Ok(page(IsUkAddress.form()))
        }
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
            cache.update(
              data =>
                data.copy(isUkAddress = Some(isUkAddress),
                          postcode = if (isUkAddress.requiresPostCode) data.postcode else None
                )
            ).map {
              userEnrolmentDetails =>
                userEnrolmentDetails.isUkAddress match {
                  case Some(IsUkAddress(Some(true))) =>
                    Redirect(routes.PostcodeController.displayPage())
                  case _ => Redirect(routes.RegistrationDateController.displayPage())
                }
            }
        )
    }

}
