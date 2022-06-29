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
import org.scalatest.matchers.must.Matchers.{convertToAnyMustWrapper, include, not}
import play.api.mvc.{AnyContent, Call}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.liability.{routes => liabilityRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyRequest
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.tax_start_date_page

import java.time.LocalDate

class TaxStartDateViewSpec extends UnitViewSpec {

  private val page = inject[tax_start_date_page]

  private val startDate: LocalDate = LocalDate.of(2022, 4, 1)

  "liability tax start date page" should {

    val view =
      createView(startDate,
                 true
      )

    "have the correct Title" in {
      view.select("title").first must containMessage("liability.taxStartDate.title")
    }

    "have a back button" in {
      view.getElementById("back-link") must not be null
      view.getElementById("back-link").text() must include(messages("site.back.hiddenText"))
    }

    "have a section header" in {
      view.getElementById("section-header").text() mustBe messages("liability.sectionHeader")
    }

    "have a page heading" in {
      view.getElementsByClass("govuk-heading-l").text() mustBe messages(
        "liability.taxStartDate.pageHeading"
      )
    }

    "have a page content including the tax start date" when {
      "threshold has been breached" in {
        val elem = view.getElementsByClass("govuk-body")

        elem.get(0).text() mustBe messages("liability.taxStartDate.hint", "1 April 2022")
        elem.get(1).text() mustBe messages("liability.taxStartDate.threshHoldBreached.hint")
      }

      "threshold is expected to be exceeded" in {
        val elem =
          createView(startDate,
                     false
          ).getElementsByClass("govuk-body")

        elem.get(0).text() mustBe messages("liability.taxStartDate.hint", "1 April 2022")
        elem.get(1).text() mustBe messages(
          "liability.taxStartDate.realisedThresholdWouldBeExceeded.hint"
        )
      }
    }

    "display 'Continue' button" in {
      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Continue"
    }
  }

  private def createView(startDate: LocalDate, hasExceededThresholdWeight: Boolean)(
    implicit request: JourneyRequest[AnyContent]
  ): Document =
    page(startDate, hasExceededThresholdWeight)(request, messages(request))

  override def exerciseGeneratedRenderingMethods(): Unit = {
    page.f(startDate, true)(journeyRequest, messages)
    page.render(startDate, true, journeyRequest, messages)
  }

}
