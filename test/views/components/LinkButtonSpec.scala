/*
 * Copyright 2025 HM Revenue & Customs
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

package views.components

import base.unit.UnitViewSpec
import org.jsoup.nodes.Element
import org.scalatest.matchers.must.Matchers
import play.twirl.api.Html
import views.html.components.linkButton

class LinkButtonSpec extends UnitViewSpec with Matchers {

  val linkButton: linkButton = inject[linkButton]

  val view: Html =
    linkButton("organisation.checkAnswers.title", "/some/page/somewhere", "example-id")

  "A linkButton" must {

    val a_element: Element = view.select("span a").get(0)

    "show the given text" in {
      view.text() mustBe messages("organisation.checkAnswers.title")
    }

    "have the given id" in {
      a_element.attr("id") mustBe "example-id"
    }

    "have given href" in {
      a_element.attr("href") mustBe "/some/page/somewhere"
    }

    "have class" in {
      a_element.attr("class") must include("govuk-!-margin-right-1")
    }

    "wrapped in group class" in {
      val wrapper_element = view.select("span").get(0)
      wrapper_element.attr("class") must include("govuk-button-group")
    }

  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    linkButton.f("organisation.checkAnswers.title", "/url", "id")
    linkButton.render("organisation.checkAnswers.title", "/url", "id", messages)
  }

}
