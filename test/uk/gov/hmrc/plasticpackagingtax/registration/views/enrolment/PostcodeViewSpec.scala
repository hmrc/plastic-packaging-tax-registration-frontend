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
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.enrolment.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.enrolment.Postcode
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.enrolment.postcode_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class PostcodeViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[postcode_page]

  private def createView(form: Form[Postcode] = Postcode.form()): Document =
    page(form)(journeyRequest, messages)

  "The Initial Postcode View" should {

    val view = createView()

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    "display 'Back' button" in {
      view.getElementById("back-link") must haveHref(routes.IsUkAddressController.displayPage())
    }

    "display title" in {
      view.select("title").text() must include(messages("enrolment.postcode.title"))
    }

    "display postcode question" in {
      view.getElementsByAttributeValueMatching("for", "value").text() must include(
        messages("enrolment.postcode.title")
      )
    }

    "display postcode input boxes" in {
      view must containElementWithID("value")
    }

    "display 'Save and continue' button" in {
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  "A Redisplayed Postcode View" should {

    "display previously entered data" in {
      val form = Postcode
        .form()
        .fill(Postcode("LS1 1AA"))
      val view = createView(form)

      view.getElementById("value").attr("value") mustBe "LS1 1AA"
    }

    "display the appropriate validation error" when {

      "a postcode was not supplied" in {
        val form = Postcode
          .form()
          .fillAndValidate(Postcode(""))
        val view = createView(form)

        view must haveGovukGlobalErrorSummary
        view must haveGovukFieldError("value", messages("enrolment.postcode.value.error.empty"))
      }

      "an invalid postcode was supplied" in {
        val form = Postcode
          .form()
          .fillAndValidate(Postcode("XXX"))
        val view = createView(form)

        view must haveGovukGlobalErrorSummary
        view must haveGovukFieldError("value", messages("enrolment.postcode.value.error.regex"))
      }

    }
  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    page.f(Postcode.form())(request, messages)
    page.render(Postcode.form(), request, messages)
  }

}
