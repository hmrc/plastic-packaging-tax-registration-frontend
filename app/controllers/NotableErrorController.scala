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

package controllers

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.actions.NotEnrolledAuthAction
import models.request.JourneyAction
import views.html.enrolment.enrolment_failure_page
import views.html.liability.grs_failure_page
import views.html.organisation.business_verification_failure_page
import views.html.organisation.sole_trader_verification_failure_page
import views.html.{duplicate_subscription_page, error_page, registration_failed_page}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}

@Singleton
class NotableErrorController @Inject() (
  authenticate: NotEnrolledAuthAction,
  journeyAction: JourneyAction,
  mcc: MessagesControllerComponents,
  errorPage: error_page,
  errorNoSavePage: enrolment_failure_page,
  grsFailurePage: grs_failure_page,
  businessVerificationFailurePage: business_verification_failure_page,
  soleTraderVerificationFailurePage: sole_trader_verification_failure_page,
  duplicateSubscriptionPage: duplicate_subscription_page,
  registrationFailedPage: registration_failed_page
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

  def soleTraderVerificationFailure(): Action[AnyContent] =
    authenticate { implicit request =>
      Ok(soleTraderVerificationFailurePage())
    }

  def grsFailure(): Action[AnyContent] =
    authenticate { implicit request =>
      Ok(grsFailurePage())
    }

  def duplicateRegistration(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      Ok(duplicateSubscriptionPage())
    }

  //noinspection ScalaUnusedSymbol
  def registrationFailed(timestamp: String): Action[AnyContent] = {
    // Note - timestamp will appear as part of referrer link on "contact us" hyperlink
    authenticate { implicit request =>
      Ok(registrationFailedPage())
    }
  }

}
