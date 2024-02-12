/*
 * Copyright 2024 HM Revenue & Customs
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

package views.deregistration

import base.unit.UnitViewSpec
import org.jsoup.nodes.{Document, Element}
import org.scalatest.matchers.must.Matchers
import spec.PptTestData
import controllers.deregistration.routes
import models.deregistration.{
  DeregistrationDetails,
  DeregistrationReason
}
import views.html.deregistration.deregister_check_your_answers_page

class DeregisterCheckYourAnswersViewSpec extends UnitViewSpec with Matchers with PptTestData {

  private val page = inject[deregister_check_your_answers_page]

  private def createView(deregistrationDetails: DeregistrationDetails): Document =
    page(deregistrationDetails)(registrationRequest, messages.messages)

  "Deregister Check Answers View with no answers" should {

    val view = createView(DeregistrationDetails(None, None))

    "display title" in {
      view.select("title").text() must include(messages("deregister.checkAnswers.title"))
    }

    "display heading" in {
      view.getElementsByTag("h1").text() must include(messages("deregister.checkAnswers.title"))
    }

    "display subheading" in {
      view.getElementsByTag("h2").text() must include(messages("deregister.checkAnswers.subheading"))
    }

    "display paragraph" in {
      val mainContent = view.select("main").text()
      mainContent must include(messages("deregister.checkAnswers.paragraph"))
    }

    "display 'Submit request' button" in {
      view.getElementById("submit").text() mustBe "Submit request"
    }

    "display want to deregister row" in {
      val row = getSummaryRow(view, 0)
      row must haveSummaryKey(messages("deregister.checkAnswers.wantToDeregister"))
      row must haveSummaryValue("")
      row must haveSummaryActionsTexts("site.link.change",
                                       "deregister.checkAnswers.wantToDeregister"
      )
      row must haveSummaryActionsHref(routes.DeregisterController.displayPage())
    }

    "display reason to deregister row" in {
      val row = getSummaryRow(view, 1)
      row must haveSummaryKey(messages("deregister.checkAnswers.reasonToDeregister"))
      row must haveSummaryValue("")
      row must haveSummaryActionsTexts("site.link.change",
                                       "deregister.checkAnswers.reasonToDeregister"
      )
      row must haveSummaryActionsHref(routes.DeregisterReasonController.displayPage())
    }
  }

  "Deregister Check Answers View with details" should {

    val view =
      createView(DeregistrationDetails(Some(true), Some(DeregistrationReason.CeasedTrading)))

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    "display want to deregister row" in {
      val row = getSummaryRow(view, 0)
      row must haveSummaryKey(messages("deregister.checkAnswers.wantToDeregister"))
      row must haveSummaryValue("Yes")
      row must haveSummaryActionsTexts("site.link.change",
                                       "deregister.checkAnswers.wantToDeregister"
      )
      row must haveSummaryActionsHref(routes.DeregisterController.displayPage())
    }

    "display reason to deregister row" in {
      val row = getSummaryRow(view, 1)
      row must haveSummaryKey(messages("deregister.checkAnswers.reasonToDeregister"))
      row must haveSummaryValue("Ceased trading")
      row must haveSummaryActionsTexts("site.link.change",
                                       "deregister.checkAnswers.reasonToDeregister"
      )
      row must haveSummaryActionsHref(routes.DeregisterReasonController.displayPage())
    }
  }

  def getSummaryRow(view: Document, index: Int): Element =
    view.getElementsByClass("govuk-summary-list__row").get(index)

  override def exerciseGeneratedRenderingMethods(): Unit = {
    page.f(DeregistrationDetails(None, None))(request, messages)
    page.render(DeregistrationDetails(None, None), request, messages)
  }

}
