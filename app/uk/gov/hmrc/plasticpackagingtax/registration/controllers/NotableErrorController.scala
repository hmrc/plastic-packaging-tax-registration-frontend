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
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.{
  business_verification_failure_page,
  duplicate_subscription_page,
  error_no_save_page,
  error_page,
  grs_failure_page
}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}

@Singleton
class NotableErrorController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  mcc: MessagesControllerComponents,
  errorPage: error_page,
  errorNoSavePage: error_no_save_page,
  grsFailurePage: grs_failure_page,
  businessVerificationFailurePage: business_verification_failure_page,
  duplicateSubscriptionPage: duplicate_subscription_page
) extends FrontendController(mcc) with I18nSupport {

  def subscriptionFailure(): Action[AnyContent] =
    authenticate { implicit request =>
      Ok(errorPage())
    }

  def enrolmentFailure(): Action[AnyContent] =
    authenticate { implicit request =>
      Ok(errorNoSavePage())
    }

  def businessVerificationFailure(): Action[AnyContent] =
    authenticate { implicit request =>
      Ok(businessVerificationFailurePage())
    }

  def grsFailure(): Action[AnyContent] =
    authenticate { implicit request =>
      Ok(grsFailurePage())
    }

  def duplicateRegistration(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      Ok(duplicateSubscriptionPage())
    }

}
