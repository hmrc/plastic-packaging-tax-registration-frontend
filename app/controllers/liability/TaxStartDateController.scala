/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.liability

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.actions.NotEnrolledAuthAction
import controllers.actions.getRegistration.GetRegistrationAction
import services.TaxStartDateService
import views.html.liability.tax_start_date_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}

@Singleton
class TaxStartDateController @Inject() (
                                         authenticate: NotEnrolledAuthAction,
                                         journeyAction: GetRegistrationAction,
                                         taxStarDateService: TaxStartDateService,
                                         mcc: MessagesControllerComponents,
                                         page: tax_start_date_page,
) 
  extends FrontendController(mcc) with I18nSupport {

  def displayPage: Action[AnyContent] = 
    (authenticate andThen journeyAction) { implicit request =>
      val taxStartDate = taxStarDateService.calculateTaxStartDate(request.registration.liabilityDetails)
      taxStartDate.act(
        notLiableAction = Redirect(routes.NotLiableController.displayPage()),
        isLiableAction = (startDate, isBackwardsBasedDate) => Ok(page(startDate, isBackwardsBasedDate))
    )
  }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction) {
      Redirect(routes.LiabilityWeightController.displayPage())
    }

}
