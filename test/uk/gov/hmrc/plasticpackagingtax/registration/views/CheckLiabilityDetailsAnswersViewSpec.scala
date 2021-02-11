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
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.check_liability_details_answers_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

import java.time.format.DateTimeFormatter

@ViewTest
class CheckLiabilityDetailsAnswersViewSpec extends UnitViewSpec with Matchers {

  private val page                                                   = instanceOf[check_liability_details_answers_page]
  private val registration                                           = aRegistration()
  private def createView(reg: Registration = registration): Document = page(reg)(request, messages)

  "Chek liability details answers View" should {

    "have proper messages for labels" in {
      messages must haveTranslationFor("checkLiabilityDetailsAnswers.title")
      messages must haveTranslationFor("checkLiabilityDetailsAnswers.date")
      messages must haveTranslationFor("checkLiabilityDetailsAnswers.weight")
      messages must haveTranslationFor("site.link.change")
    }

    val view                      = createView()
    val viewWithEmptyRegistration = createView(Registration("id"))

    "display 'Back' button" in {

      view.getElementById("back-link") must haveHref(routes.LiabilityWeightController.displayPage())
    }

    "display title" in {

      view.getElementsByClass("govuk-heading-xl").first() must containMessage(
        "checkLiabilityDetailsAnswers.title"
      )
    }

    "display liability date" in {

      view.getElementsByClass("govuk-summary-list__key").first() must containMessage(
        "checkLiabilityDetailsAnswers.date"
      )
      view.getElementsByClass("govuk-summary-list__value").first() must containText(
        registration.liabilityDetails.startDate.get.asLocalDate.format(
          DateTimeFormatter.ofPattern("dd MMM yyyy")
        )
      )
    }

    "display empty value when no date on registration object" in {

      viewWithEmptyRegistration.getElementsByClass(
        "govuk-summary-list__key"
      ).first() must containMessage("checkLiabilityDetailsAnswers.date")
      viewWithEmptyRegistration.getElementsByClass(
        "govuk-summary-list__value"
      ).first.text() mustBe ""
    }

    val summaryList = view.getElementsByClass("govuk-summary-list")

    "display link to change liability date" in {

      summaryList.first.getElementsByClass("govuk-link").first() must haveHref(
        routes.LiabilityStartDateController.displayPage()
      )
    }

    "display liability weight" in {

      view.getElementsByClass("govuk-summary-list__key").get(1) must containMessage(
        "checkLiabilityDetailsAnswers.weight"
      )
      view.getElementsByClass("govuk-summary-list__value").get(1) must containText(
        registration.liabilityDetails.weight.get.totalKg.get.toString
      )
    }

    "display empty value when no weight on registration object" in {

      viewWithEmptyRegistration.getElementsByClass("govuk-summary-list__key").get(
        1
      ) must containMessage("checkLiabilityDetailsAnswers.weight")
      viewWithEmptyRegistration.getElementsByClass("govuk-summary-list__value").get(
        1
      ).text() mustBe ""
    }

    "display link to change liability weight" in {

      summaryList.first.getElementsByClass("govuk-link").get(1) must haveHref(
        routes.LiabilityWeightController.displayPage()
      )

    }

    "display 'Continue' button" in {

      view.getElementsByClass("govuk-button").text() must include("Save and Continue")
      view.getElementsByClass("govuk-button").first() must haveHref(
        routes.RegistrationController.displayPage()
      )
    }
  }
}
