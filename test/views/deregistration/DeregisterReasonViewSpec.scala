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
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import forms.deregistration.DeregisterReasonForm
import models.deregistration.DeregistrationReason.{BelowDeminimis, CeasedTrading, RegisteredIncorrectly, TakenIntoGroupRegistration}
import views.components.Styles.gdsFieldsetPageHeading
import views.html.deregistration.deregister_reason_page

class DeregisterReasonViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[deregister_reason_page]

  private def createView(form: Form[DeregisterReasonForm] = DeregisterReasonForm.form()): Document =
    page(form)(registrationRequest, messages)

  "Confirm Deregistration Reason View" should {

    implicit val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {

      displaySignOutLink(view)
    }

    "display title" in {

      view.select("title").text() must include(messages("deregistration.reason.title"))
    }

    "display header" in {

      view.getElementsByClass(gdsFieldsetPageHeading).text() must include(
        messages("deregistration.reason.title")
      )
    }

    "display radio inputs" in {

      view.getElementById("answer").attr("value").text() mustBe CeasedTrading.toString
      view.getElementsByClass("govuk-label").get(0).text() mustBe "Ceased trading"

      view.getElementById("answer-2").attr("value").text() mustBe BelowDeminimis.toString
      view.getElementsByClass("govuk-label").get(1).text(
      )mustBe "Have not met the 10,000kg threshold for a 12-month period since my tax start date and do not expect to meet it in the next 12 months"

      view.getElementById("answer-3").attr("value").text() mustBe TakenIntoGroupRegistration.toString
      view.getElementsByClass("govuk-label").get(2).text() mustBe "Want to register as part of a group"

      view.getElementById("answer-4").attr("value").text() mustBe RegisteredIncorrectly.toString
      view.getElementsByClass("govuk-label").get(3).text() mustBe "Registered incorrectly"

    }

    "display 'Save and continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  "Confirm Deregistration Reason view when filled" should {

    "display checked radio button" in {

      val form = DeregisterReasonForm
        .form()
        .fill(DeregisterReasonForm.toForm(BelowDeminimis))
      val view = createView(form)

      view.getElementById("answer-2").attr("value") mustBe BelowDeminimis.toString
    }

    "display error" when {

      "no radio button checked" in {

        val form = DeregisterReasonForm.form().bind(emptyFormData)
        val view = createView(form)

        view must haveGovukFieldError("answer", "Select why you want to deregister")
        view must haveGovukGlobalErrorSummary
      }
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(DeregisterReasonForm.form())(request, messages)
    page.render(DeregisterReasonForm.form(), request, messages)
  }

}
