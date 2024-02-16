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

package views.viewmodels

import play.api.data.Form
import play.api.i18n.Messages

class Title(headingKey: String, headingArg: String = "", headingArgs: Option[Seq[String]] = None, hasErrors: Boolean = false) {

  def toString(implicit messages: Messages): String = {
    def args   = headingArgs.getOrElse(Seq(headingArg))
    val prefix = if (hasErrors) messages("error.browser.title.prefix") + " " else ""
    prefix + messages("title.format", messages(headingKey, args: _*), messages("service.name"))
  }

}

object Title {

  def apply(form: Form[_], headingKey: String) =
    new Title(headingKey, "", None, form.hasErrors || form.hasGlobalErrors)

  def apply(form: Form[_], headingKey: String, headingArg: String) =
    new Title(headingKey, headingArg, None, form.hasErrors || form.hasGlobalErrors)

  def apply(form: Form[_], headingKey: String, headingArgs: Option[Seq[String]]) =
    new Title(headingKey, "", headingArgs, form.hasErrors || form.hasGlobalErrors)

  def apply(headingKey: String, headingArg: String = "", headingArgs: Option[Seq[String]] = None) =
    new Title(headingKey, headingArg, headingArgs, false)

}
