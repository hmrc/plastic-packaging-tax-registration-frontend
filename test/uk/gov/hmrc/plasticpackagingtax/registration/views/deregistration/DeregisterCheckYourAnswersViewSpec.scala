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

package uk.gov.hmrc.plasticpackagingtax.registration.views.deregistration

import base.unit.UnitViewSpec
import org.jsoup.nodes.{Document, Element}
import org.scalatest.matchers.must.Matchers
import spec.PptTestData
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.deregistration.routes
import uk.gov.hmrc.plasticpackagingtax.registration.models.deregistration.{
  DeregistrationDetails,
  DeregistrationReason
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.deregistration.deregister_check_your_answers_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class DeregisterCheckYourAnswersViewSpec extends UnitViewSpec with Matchers with PptTestData {

  private val page = inject[deregister_check_your_answers_page]

  private def createView(deregistrationDetails: DeregistrationDetails): Document =
    page(deregistrationDetails)(authenticatedRequest, messages)

  "Deregister Check Answers View with no answers" should {

    val view = createView(DeregistrationDetails(None, None))

    "display 'Back' button" in {
      view.getElementById("back-link") must haveHref(
        routes.DeregisterReasonController.displayPage()
      )
    }

    "display title" in {
      view.select("title").text() must include(messages("deregister.checkAnswers.title"))
    }

    "display heading" in {
      view.getElementsByTag("h1").text() must include(messages("deregister.checkAnswers.title"))
    }

    "display 'Confirm and continue' button" in {
      view.getElementById("submit").text() mustBe "Confirm and continue"
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
      row must haveSummaryValue("Ceased Trading")
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
