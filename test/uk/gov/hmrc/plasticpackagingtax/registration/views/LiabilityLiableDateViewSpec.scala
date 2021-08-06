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
import play.api.data.Form
import play.api.mvc.Call
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.LiabilityLiableDate
import uk.gov.hmrc.plasticpackagingtax.registration.forms.LiabilityLiableDate.form
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability_liable_date_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class LiabilityLiableDateViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[liability_liable_date_page]

  private def createView(
    form: Form[LiabilityLiableDate] = LiabilityLiableDate.form(),
    backLink: Call = routes.LiabilityExpectToExceedThresholdWeightController.displayPage()
  ): Document =
    page(form, backLink = backLink)(request, messages)

  "Liability section 'Liable Date' view" should {

    "have proper messages for labels" in {
      messages must haveTranslationFor("liabilityLiableDatePage.sectionHeader")
      messages must haveTranslationFor("liabilityLiableDatePage.title")
      messages must haveTranslationFor("liabilityLiableDatePage.hint")
      messages must haveTranslationFor("liabilityLiableDatePage.question")
      messages must haveTranslationFor("liabilityLiableDatePage.question.empty.error")
    }

    val view = createView()

    "validate other rendering  methods" in {
      page.f(form(), routes.LiabilityWeightController.displayPage())(request, messages).select(
        "title"
      ).text() must include(messages("liabilityLiableDatePage.title"))
      page.render(form(), routes.LiabilityWeightController.displayPage(), request, messages).select(
        "title"
      ).text() must include(messages("liabilityLiableDatePage.title"))
    }

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {

      displaySignOutLink(view)
    }

    "display 'Back' button" in {

      view.getElementById("back-link") must haveHref(
        routes.LiabilityExpectToExceedThresholdWeightController.displayPage()
      )
    }

    "display title" in {

      view.select("title").text() must include(messages("liabilityLiableDatePage.title"))
    }

    "display header" in {

      view.getElementsByClass("govuk-caption-xl").text() must include(
        messages("liabilityLiableDatePage.sectionHeader")
      )
    }

    "display radio inputs" in {

      view must containElementWithID("answer")
      view.getElementsByClass("govuk-label").first().text() mustBe "Yes"
      view must containElementWithID("answer-2")
      view.getElementsByClass("govuk-label").get(1).text() mustBe "No"
    }

    "display 'Save And Continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and Continue"
    }

    "display 'Save and come back later' button" in {

      view.getElementById("save_and_come_back_later").text() mustBe "Save and come back later"
    }
  }

  "Liability section 'Liable Date' view when filled" should {

    "display radio button checked" in {

      val form = LiabilityLiableDate.form()
        .fill(LiabilityLiableDate("yes"))
      val view = createView(form)

      view.getElementById("answer").attr("value") mustBe "yes"
    }

    "display error" when {

      "no radio button checked" in {

        val form = LiabilityLiableDate.form()
          .fillAndValidate(LiabilityLiableDate(None))
        val view = createView(form)

        view must haveGovukGlobalErrorSummary
      }
    }
  }
}
