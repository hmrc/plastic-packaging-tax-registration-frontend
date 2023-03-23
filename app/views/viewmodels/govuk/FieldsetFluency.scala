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

package views.viewmodels.govuk

import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{Content, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.fieldset.{Fieldset, Legend}

object fieldset extends FieldsetFluency

trait FieldsetFluency {

  object FieldsetViewModel {

    def apply(legend: Legend): Fieldset =
      Fieldset(legend = Some(legend))

  }

  implicit class FluentFieldset(fieldset: Fieldset) {

    def describedBy(value: String): Fieldset =
      fieldset copy (describedBy = Some(value))

    def withCssClass(newClass: String): Fieldset =
      fieldset copy (classes = s"${fieldset.classes} $newClass")

    def withRole(role: String): Fieldset =
      fieldset copy (role = Some(role))

    def withAttribute(attribute: (String, String)): Fieldset =
      fieldset copy (attributes = fieldset.attributes + attribute)

    def withHtml(html: Html): Fieldset =
      fieldset copy (html = html)

  }

  object LegendViewModel {

    def apply(content: Content): Legend =
      Legend(content = content)

    def apply(content: String): Legend =
      Legend(content = Text(content))

  }

  implicit class FluentLegend(legend: Legend) {

    def asPageHeading(size: LegendSize = LegendSize.Large): Legend =
      legend
        .copy(isPageHeading = true)
        .withCssClass(size.toString)

    def withCssClass(newClass: String): Legend =
      legend copy (classes = s"${legend.classes} $newClass")

  }

}

