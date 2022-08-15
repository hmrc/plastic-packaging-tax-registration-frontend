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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.enrolment

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.hmrcfrontend.config.ContactFrontendConfig
import uk.gov.hmrc.hmrcfrontend.views.Utils
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.NotEnrolledAuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.enrolment.{reference_number_already_used_failure_page, verification_failure_page}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject

class NotableErrorController @Inject() (
  authenticate: NotEnrolledAuthAction,
  mcc: MessagesControllerComponents,
  verificationFailurePage: verification_failure_page,
  referenceNumberAlreadyUsedPage: reference_number_already_used_failure_page,
  frontendConfig: ContactFrontendConfig,
  appConfig: AppConfig,
  
) extends FrontendController(mcc) with I18nSupport {

  def enrolmentVerificationFailurePage(): Action[AnyContent] =
    authenticate { implicit request =>
      val referrer = frontendConfig.referrerUrl.map(Utils.urlEncode).getOrElse("<referrer-unknown>")
      val contactUsHref = appConfig.reportTechincalProblemUrl + "&referrerUrl=" + referrer
      Ok(verificationFailurePage(contactUsHref))
    }

  def enrolmentReferenceNumberAlreadyUsedPage(): Action[AnyContent] =
    authenticate { implicit request =>
      Ok(referenceNumberAlreadyUsedPage())
    }

}
