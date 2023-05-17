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

package views

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import config.AppConfig
import views.html.unauthorised.unauthorised

class UnauthorisedViewSpec extends UnitViewSpec with Matchers {

  private val page       = inject[unauthorised]
  val appConfig = inject[AppConfig]

  private def createView(): Document =
    page()(registrationJourneyRequest, messages)

  "Unauthorised Page view" should {

    val view = createView()

    "display page header" in {
      view.getElementsByTag("h1").first() must containMessage("unauthorised.heading")
    }

    "display required information for start page" in {
      val contentParagraphs = view.select("p.govuk-body")

      stripSpaceBeforeFullstop(contentParagraphs.get(0).text()) must include(
        messages("unauthorised.paragraph.1", messages("unauthorised.paragraph.1.link"))
      )
    }

    "display required information for register" in {
      val contentParagraphs = view.select("p.govuk-body")

      stripSpaceBeforeFullstop(contentParagraphs.get(1).text()) must include(
        messages("unauthorised.paragraph.2", messages("unauthorised.paragraph.2.link"))
      )
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f()(request, messages)
    page.render(request, messages)
  }

  private def stripSpaceBeforeFullstop(text: String) = text.replaceAll(" \\.", ".")

}
