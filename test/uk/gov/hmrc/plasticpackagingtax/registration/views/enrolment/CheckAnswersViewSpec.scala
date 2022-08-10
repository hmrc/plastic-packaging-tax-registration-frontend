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

package uk.gov.hmrc.plasticpackagingtax.registration.views.enrolment

import base.unit.UnitViewSpec
import org.jsoup.nodes.{Document, Element}
import org.scalatest.matchers.must.Matchers
import spec.PptTestData
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.enrolment.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.enrolment.IsUkAddress
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.UserEnrolmentDetails
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.enrolment.check_answers_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class CheckAnswersViewSpec extends UnitViewSpec with Matchers with PptTestData {

  private val page = inject[check_answers_page]

  private def createView(answers: UserEnrolmentDetails = UserEnrolmentDetails()): Document =
    page(answers)(journeyRequest, messages)

  "Check Answers View with no answers" should {

    val view = createView()

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    "not display 'Back' button" in {
      view.getElementById("back-link") mustBe null
    }

    "display title" in {
      view.select("title").text() must include(messages("enrolment.checkAnswers.title"))
    }

    "display heading" in {
      view.getElementsByTag("h1").text() must include(messages("enrolment.checkAnswers.title"))
    }

    "display 'Confirm and continue' button" in {
      view.getElementById("submit").text() mustBe "Confirm and continue"
    }

    "display 'PPT reference' row" in {
      val row = getSummaryRow(view, 0)
      row must haveSummaryKey(messages("enrolment.checkAnswers.pptReference"))
      row must haveSummaryValue("")
      row must haveSummaryActionsTexts("site.link.change", "enrolment.checkAnswers.pptReference")
      row must haveSummaryActionsHref(routes.PptReferenceController.displayPage())
    }

    "display 'is UK address' row" in {
      val row = getSummaryRow(view, 1)
      row must haveSummaryKey(messages("enrolment.checkAnswers.ukAddress"))
      row must haveSummaryValue("")
      row must haveSummaryActionsTexts("site.link.change", "enrolment.checkAnswers.ukAddress")
      row must haveSummaryActionsHref(routes.IsUkAddressController.displayPage())
    }

    "display 'registration date' row" in {
      val row = getSummaryRow(view, 2)
      row must haveSummaryKey(messages("enrolment.checkAnswers.registrationDate"))
      row must haveSummaryValue("")
      row must haveSummaryActionsTexts("site.link.change",
                                       "enrolment.checkAnswers.registrationDate"
      )
      row must haveSummaryActionsHref(routes.RegistrationDateController.displayPage())
    }

    "not display 'postcode' row" in {
      view.getElementsByClass("govuk-summary-list__row").text() must not include (messages(
        "enrolment.checkAnswers.postcode"
      ))
    }

  }

  "Check Answers View for Uk registered organisation" should {

    val view = createView(userEnrolmentDetails)
    "display 'PPT reference' row" in {
      val row = getSummaryRow(view, 0)
      row must haveSummaryKey(messages("enrolment.checkAnswers.pptReference"))
      row must haveSummaryValue("XAPPT000123456")
      row must haveSummaryActionsTexts("site.link.change", "enrolment.checkAnswers.pptReference")
      row must haveSummaryActionsHref(routes.PptReferenceController.displayPage())
    }

    "display 'is UK address' row" in {
      val row = getSummaryRow(view, 1)
      row must haveSummaryKey(messages("enrolment.checkAnswers.ukAddress"))
      row must haveSummaryValue("Yes")
      row must haveSummaryActionsTexts("site.link.change", "enrolment.checkAnswers.ukAddress")
      row must haveSummaryActionsHref(routes.IsUkAddressController.displayPage())
    }

    "display 'postcode' row" in {
      val row = getSummaryRow(view, 2)
      row must haveSummaryKey(messages("enrolment.checkAnswers.postcode"))
      row must haveSummaryValue("AB1 2BC")
      row must haveSummaryActionsTexts("site.link.change", "enrolment.checkAnswers.postcode")
      row must haveSummaryActionsHref(routes.PostcodeController.displayPage())
    }

    "display 'registration date' row" in {
      val row = getSummaryRow(view, 3)
      row must haveSummaryKey(messages("enrolment.checkAnswers.registrationDate"))
      row must haveSummaryValue("01/02/2021")
      row must haveSummaryActionsTexts("site.link.change",
                                       "enrolment.checkAnswers.registrationDate"
      )
      row must haveSummaryActionsHref(routes.RegistrationDateController.displayPage())
    }

  }

  "Check Answers View for non-Uk registered organisation" should {

    val view = createView(userEnrolmentDetails.copy(isUkAddress = Some(IsUkAddress(Some(false)))))
    "display 'PPT reference' row" in {
      val row = getSummaryRow(view, 0)
      row must haveSummaryKey(messages("enrolment.checkAnswers.pptReference"))
      row must haveSummaryValue("XAPPT000123456")
      row must haveSummaryActionsTexts("site.link.change", "enrolment.checkAnswers.pptReference")
      row must haveSummaryActionsHref(routes.PptReferenceController.displayPage())
    }

    "display 'is UK address' row" in {
      val row = getSummaryRow(view, 1)
      row must haveSummaryKey(messages("enrolment.checkAnswers.ukAddress"))
      row must haveSummaryValue("No")
      row must haveSummaryActionsTexts("site.link.change", "enrolment.checkAnswers.ukAddress")
      row must haveSummaryActionsHref(routes.IsUkAddressController.displayPage())
    }

    "display 'registration date' row" in {
      val row = getSummaryRow(view, 2)
      row must haveSummaryKey(messages("enrolment.checkAnswers.registrationDate"))
      row must haveSummaryValue("01/02/2021")
      row must haveSummaryActionsTexts("site.link.change",
                                       "enrolment.checkAnswers.registrationDate"
      )
      row must haveSummaryActionsHref(routes.RegistrationDateController.displayPage())
    }

    "not display 'postcode' row" in {
      view.getElementsByClass("govuk-summary-list__row").text() must not include (messages(
        "enrolment.checkAnswers.postcode"
      ))
    }
  }

  def getSummaryRow(view: Document, index: Int): Element =
    view.getElementsByClass("govuk-summary-list__row").get(index)

  override def exerciseGeneratedRenderingMethods(): Unit = {
    page.f(UserEnrolmentDetails())(request, messages)
    page.render(UserEnrolmentDetails(), request, messages)
  }

}
