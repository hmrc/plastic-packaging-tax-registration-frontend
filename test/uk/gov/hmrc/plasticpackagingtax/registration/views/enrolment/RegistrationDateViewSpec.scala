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
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import play.api.mvc.Call
import uk.gov.hmrc.plasticpackagingtax.registration.forms.DateData
import uk.gov.hmrc.plasticpackagingtax.registration.forms.enrolment.RegistrationDate
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.enrolment.registration_date_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class RegistrationDateViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[registration_date_page]

  private val previousPage = Call("GET", "/some-back")

  private def createView(
    form: Form[RegistrationDate] = RegistrationDate.form(),
    previousPage: Call = previousPage
  ): Document =
    page(form, previousPage)(journeyRequest, messages)

  "The Initial Registration Date View" should {

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
      view.select("title").text() must include(messages("enrolment.registrationDate.title"))
    }

    "display registration date question" in {
      view.getElementsByTag("h1").text() must include(messages("enrolment.registrationDate.title"))
    }

    "display registration date boxes" in {
      view must containElementWithID("date.day")
      view must containElementWithID("date.month")
      view must containElementWithID("date.year")
    }

    "display 'Save and continue' button" in {
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  "A Redisplayed Registration Date View" should {

    "display previously entered data" in {
      val form = RegistrationDate
        .form()
        .fill(RegistrationDate(DateData("1", "2", "2021")))
      val view = createView(form)

      view.getElementById("date.day").attr("value") mustBe "1"
      view.getElementById("date.month").attr("value") mustBe "2"
      view.getElementById("date.year").attr("value") mustBe "2021"
    }

    "display the appropriate validation error" when {

      "a registration date was not supplied" in {
        val form = RegistrationDate
          .form()
          .fillAndValidate(RegistrationDate(DateData("", "", "")))
        val view = createView(form)

        view must haveGovukGlobalErrorSummary
        view must haveGovukFieldError("date", messages("date.day.error"))
      }

      "an invalid registration date was supplied" in {
        val form = RegistrationDate
          .form()
          .fillAndValidate(RegistrationDate(DateData("31", "13", "2000")))
        val view = createView(form)

        view must haveGovukGlobalErrorSummary
        view must haveGovukFieldError("date", messages("date.month.error"))
      }

    }
  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    page.f(RegistrationDate.form(), previousPage)(request, messages)
    page.render(RegistrationDate.form(), previousPage, request, messages)
  }

}
