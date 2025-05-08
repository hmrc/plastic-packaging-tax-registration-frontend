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

package views.liability

import base.unit.UnitViewSpec
import controllers.liability.{routes => liabilityRoutes}
import models.registration.Registration
import models.request.JourneyRequest
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.mvc.AnyContent
import play.twirl.api.TwirlHelperImports.twirlJavaCollectionToScala
import views.components.Styles
import views.html.liability.check_liability_details_answers_page

class CheckLiabilityDetailsAnswersViewSpec extends UnitViewSpec with Matchers {

  private val page         = inject[check_liability_details_answers_page]
  private val registration = aRegistration()

  "Check liability details answers View" should {
    val view = createView(reg = registration)(registrationJourneyRequest)

    "have the correct title" in {
      view.select("title").first must containMessage("liability.checkAnswers.title")
    }

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    "display title" in {
      view.getElementsByClass(Styles.gdsPageHeading).first() must containMessage("liability.checkAnswers.title")
    }

    "display expected content" when {

      assertSummaryRows(
        view,
        List(
          SummaryRowDetail(
            "liability.checkAnswers.expectToExceededThreshold",
            "Yes",
            Some(liabilityRoutes.ExpectToExceedThresholdWeightController.displayPage().url)
          ),
          SummaryRowDetail(
            "liability.checkAnswers.dateRealisedExpectToExceededThreshold",
            "5 March 2022",
            Some(liabilityRoutes.ExpectToExceedThresholdWeightDateController.displayPage.url)
          ),
          SummaryRowDetail(
            "liability.checkAnswers.exceededThreshold",
            "No",
            Some(liabilityRoutes.ExceededThresholdWeightController.displayPage.url)
          ),
          SummaryRowDetail("liability.checkAnswers.startDate", "1 April 2022", None),
          SummaryRowDetail(
            "liability.checkAnswers.expectedWeightNext12m",
            "12000 kg",
            Some(liabilityRoutes.LiabilityWeightController.displayPage().url)
          ),
          SummaryRowDetail(
            "liability.checkAnswers.registrationType",
            "A single organisation",
            Some(liabilityRoutes.RegistrationTypeController.displayPage().url)
          )
        )
      )
    }

    "display 'Continue' button" in {
      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Continue"
    }

  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(registration)(registrationJourneyRequest, messages)
    page.render(registration, registrationJourneyRequest, messages)
  }

  private def createView(reg: Registration)(implicit request: JourneyRequest[AnyContent]): Document =
    page(reg)(request, messages(request))

  private def assertSummaryRows(view: Document, expectedRows: List[SummaryRowDetail]) = {
    val actualRows = view.getElementsByClass("govuk-summary-list__row")

    actualRows.zip(expectedRows).zipWithIndex.foreach { case ((actualRow, expectedRow), index) =>
      s"Row ${index + 1} has correct key, value and action" in {
        actualRow.getElementsByClass("govuk-summary-list__key").first must containMessage(expectedRow.label)
        actualRow.getElementsByClass("govuk-summary-list__value").first must containText(expectedRow.value)
        if (expectedRow.actionLink.isDefined)
          actualRow.getElementsByTag("a").first must haveHref(expectedRow.actionLink.get)
        else assert(actualRow.getElementsByTag("a").isEmpty)
      }
    }
  }

  private case class SummaryRowDetail(label: String, value: String, actionLink: Option[String])

}
