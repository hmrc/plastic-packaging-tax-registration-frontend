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

package views.enrolment

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import forms.enrolment.PptReference
import views.html.enrolment.ppt_reference_page

class PptReferenceViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[ppt_reference_page]

  private def createView(form: Form[PptReference] = PptReference.form()): Document =
    page(form)(registrationJourneyRequest, messages)

  "The Initial PPT Reference View" should {

    val view = createView()

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    "display title" in {
      view.select("title").text() must include(messages("enrolment.pptReference.title"))
    }

    "display ppt reference question" in {
      view.getElementsByAttributeValueMatching("for", "value").text() must include(messages("enrolment.pptReference.title"))
    }

    "display question hint" in {
      view.getElementById("value-hint").text() must include(messages("enrolment.pptReference.hint"))
    }

    "display ppt reference input boxes" in {
      view must containElementWithID("value")
    }

    "display 'Save and continue' button" in {
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  "A Redisplayed PPT Reference View" should {

    "display previously entered data" in {
      val form = PptReference
        .form()
        .fill(PptReference("XMPPT1234567890"))
      val view = createView(form)

      view.getElementById("value").attr("value") mustBe "XMPPT1234567890"
    }

    "display the appropriate validation error" when {

      "a ppt reference was not supplied" in {
        val form = PptReference
          .form()
          .fillAndValidate(PptReference(""))
        val view = createView(form)

        view must haveGovukGlobalErrorSummary
        view must haveGovukFieldError("value", messages("enrolment.pptReference.value.error.empty"))
      }

      "an invalid ppt reference was supplied" in {
        val form = PptReference
          .form()
          .fillAndValidate(PptReference("XXX"))
        val view = createView(form)

        view must haveGovukGlobalErrorSummary
        view must haveGovukFieldError("value", messages("enrolment.pptReference.value.error.regex"))
      }

    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(PptReference.form())(request, messages)
    page.render(PptReference.form(), request, messages)
  }

}
