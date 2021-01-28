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

import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import base.unit.UnitViewSpec
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.LiabilityStartDate
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability_start_date_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class LiabilityStartDateViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[liability_start_date_page]

  private def createView(): Document = page(LiabilityStartDate.form())(request, messages)

  "Liability Start Date View" should {

    "have proper messages for labels" in {
      messages must haveTranslationFor("liabilityStartDatePage.sectionHeader")
      messages must haveTranslationFor("liabilityStartDatePage.title")
      messages must haveTranslationFor("liabilityStartDatePage.hint")
      messages must haveTranslationFor("liabilityStartDatePage.question")
    }

    val view = createView()

    "display 'Back' button" in {

      view.getElementById("back-link") must haveHref(routes.RegistrationController.displayPage())
    }

    "display title" in {

      view.select("title").text() must include(messages("liabilityStartDatePage.title"))
    }

    "display header" in {

      view.getElementsByClass("govuk-caption-xl").text() must include(
        messages("liabilityStartDatePage.sectionHeader")
      )
    }

    "display liability start date question" in {

      view.getElementsByClass("govuk-fieldset__heading").text() must include(
        messages("liabilityStartDatePage.question")
      )
    }

    "display question hint" in {

      view.getElementById("liability-start-date-hint") must containMessage(
        "liabilityStartDatePage.hint"
      )
    }

    "display day input box" in {

      view.getElementsByAttributeValueMatching("for", "day").text() must include(
        messages("date.day")
      )
    }

    "display month input box" in {

      view.getElementsByAttributeValueMatching("for", "month").text() must include(
        messages("date.month")
      )
    }

    "display year input box" in {

      view.getElementsByAttributeValueMatching("for", "year").text() must include(
        messages("date.year")
      )
    }

    "display 'Continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit") must containMessage("Continue")
    }
  }
}
