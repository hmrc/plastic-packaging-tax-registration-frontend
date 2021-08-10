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

package uk.gov.hmrc.plasticpackagingtax.registration.views

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.unauthorised
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class UnauthorisedViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[unauthorised]

  private def createView(): Document =
    page()(journeyRequest, messages)

  "Unauthorised Page view" should {

    "have proper messages for labels" in {
      messages must haveTranslationFor("unauthorised.heading")
      messages must haveTranslationFor("unauthorised.paragraph.1")
      messages must haveTranslationFor("unauthorised.paragraph.1.link")
      messages must haveTranslationFor("unauthorised.paragraph.2")
      messages must haveTranslationFor("unauthorised.paragraph.2.link")
    }

    val view = createView()

    "display page header" in {
      view.getElementsByTag("h1").first() must containMessage("unauthorised.heading")
    }

    "display register for ppt link" in {
      val link = view.getElementById("register_for_ppt_link")

      link must containMessage("unauthorised.paragraph.1.link")
      link must haveHref(routes.StartController.displayStartPage().url)
      link.attr("target") mustBe "_self"
    }

    "display ppt guidance link" in {
      val link = view.getElementById("find_out_about_ppt_link")

      link must containMessage("unauthorised.paragraph.2.link")
      link must haveHref(
        "https://www.gov.uk/government/publications/introduction-of-plastic-packaging-tax/plastic-packaging-tax"
      )
      link.attr("target") mustBe "_self"
    }
  }
}
