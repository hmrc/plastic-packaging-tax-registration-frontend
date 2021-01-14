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

package uk.gov.hmrc.plasticpackagingtax.registration.config

import javax.inject.{Inject, Singleton}
import play.api.i18n.MessagesApi
import play.api.mvc.{Request, RequestHeader, Result, Results}
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.{NoActiveSession}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.error_template
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler

@Singleton
class ErrorHandler @Inject() (error_template: error_template, val messagesApi: MessagesApi)(implicit
  appConfig: AppConfig
) extends FrontendErrorHandler {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit
    request: Request[_]
  ): Html =
    error_template(pageTitle, heading, message)

  override def resolveError(rh: RequestHeader, ex: Throwable): Result =
    ex match {
      case _: NoActiveSession =>
        Results.Redirect(appConfig.loginUrl, Map("continue" -> Seq(appConfig.loginContinueUrl)))
      case _ => super.resolveError(rh, ex)
    }

}
