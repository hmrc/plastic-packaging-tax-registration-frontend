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

package uk.gov.hmrc.plasticpackagingtax.registration.views.liability

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import play.api.mvc.Call
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.liability.{
  routes => liabilityRoutes
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Date
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.LiabilityStartDate
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.LiabilityStartDate.form
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.liability_start_date_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class LiabilityStartDateViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[liability_start_date_page]

  private def createView(
    form: Form[Date] = LiabilityStartDate.form(),
    backLink: Call = liabilityRoutes.LiabilityWeightController.displayPage()
  ): Document =
    page(form, backLink)(journeyRequest, messages)

  "Liability Start Date View" should {

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(view)

    }

    "display 'Back' button" in {

      view.getElementById("back-link") must haveHref(
        liabilityRoutes.LiabilityWeightController.displayPage()
      )
    }

    "display title" in {

      view.select("title").text() must include(messages("liabilityStartDatePage.title"))
    }

    "display header" in {

      view.getElementsByClass("govuk-caption-l").text() must include(
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

    "display 'Save and continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  "Liability Weight View when filled" should {

    "display data in date inputs" in {

      val form = LiabilityStartDate.form()
        .fill(aRegistration().liabilityDetails.startDate.get)
      val view = createView(form)

      view.getElementById("day").attr("value") mustBe "1"
      view.getElementById("month").attr("value") mustBe "4"
      view.getElementById("year").attr("value") mustBe "2022"
    }
  }

  "display error" when {

    "Start date is not entered" in {

      val form = LiabilityStartDate.form()
        .fillAndValidate(Date(None, None, None))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("liability-start-date", "Enter the day")
      view must haveGovukFieldError("liability-start-date", "Enter the month")
      view must haveGovukFieldError("liability-start-date", "Enter the year")
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(form(), liabilityRoutes.LiabilityExpectToExceedThresholdWeightController.displayPage())(
      journeyRequest,
      messages
    )
    page.render(form(),
                liabilityRoutes.LiabilityExpectToExceedThresholdWeightController.displayPage(),
                journeyRequest,
                messages
    )
  }

}
