/*
 * Copyright 2024 HM Revenue & Customs
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

package config

import javax.inject.{Inject, Singleton}
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Request
import play.twirl.api.Html
import views.html.error_template
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler

@Singleton
class ErrorHandler @Inject() (error_template: error_template, val messagesApi: MessagesApi)
    extends FrontendErrorHandler {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit
    request: Request[_]
  ): Html =
    error_template(pageTitle, heading, List(message))

  override def badRequestTemplate(implicit request: Request[_]): Html          = pptErrorTemplate()
  override def notFoundTemplate(implicit request: Request[_]): Html            = pptErrorTemplate()
  override def internalServerErrorTemplate(implicit request: Request[_]): Html = pptErrorTemplate()

  private def pptErrorTemplate()(implicit request: Request[_]) =
    customisedErrorTemplate(Messages("error.title"),
                            Messages("error.title"),
                            List(Messages("error.detail1"), Messages("error.detail2"))
    )

  private def customisedErrorTemplate(pageTitle: String, heading: String, messages: List[String])(
    implicit request: Request[_]
  ): Html =
    error_template(pageTitle, heading, messages)

}
