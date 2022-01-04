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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.RegType.{GROUP, SINGLE_ENTITY}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.RegistrationType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.RegistrationType.form
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.registration_type_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class RegistrationTypeViewSpec extends UnitViewSpec with Matchers {

  private val backLink = Call("GET", "/backLink")
  private val page     = instanceOf[registration_type_page]

  private def createView(form: Form[RegistrationType] = RegistrationType.form()): Document =
    page(form, backLink)(journeyRequest, messages)

  "Registration Type View" should {

    val view = createView()

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    "display 'Back' button" in {
      view.getElementById("back-link") must haveHref(backLink.url)
    }

    "display title" in {
      view.select("title").text() must include(messages("registrationType.title"))
    }

    "display header" in {
      view.select("h1").text() must include(messages("registrationType.title"))
    }

    "display radio inputs" in {
      view.getElementById("value").attr("value").text() mustBe SINGLE_ENTITY.toString
      view.getElementsByClass("govuk-label").first().text() mustBe messages(
        "registrationType.singleEntity"
      )

      view.getElementById("value-2").attr("value").text() mustBe GROUP.toString
      view.getElementsByClass("govuk-label").get(1).text() mustBe messages("registrationType.group")
    }

    "display 'Save and continue' button" in {
      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  "Populated Registration Type View" should {

    "display checked radio button" in {
      val form = RegistrationType
        .form()
        .fill(RegistrationType(SINGLE_ENTITY.toString))
      val view = createView(form)

      view.getElementById("value").attr("value") mustBe SINGLE_ENTITY.toString
    }

    "display error" when {

      "no radio button checked" in {
        val form = RegistrationType
          .form()
          .bind(emptyFormData)
        val view = createView(form)

        view must haveGovukFieldError("value", "This field is required")
        view must haveGovukGlobalErrorSummary
      }

    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(form(), backLink)(journeyRequest, messages)
    page.render(form(), backLink, journeyRequest, messages)
  }

}
