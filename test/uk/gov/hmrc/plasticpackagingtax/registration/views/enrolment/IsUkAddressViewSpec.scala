/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.enrolment.IsUkAddress
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.enrolment.is_uk_address_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class IsUkAddressViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[is_uk_address_page]

  private def createView(form: Form[IsUkAddress] = IsUkAddress.form()): Document =
    page(form)(journeyRequest, messages)

  "The Initial Is UK Address View" should {

    val view = createView()

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    "display 'Back' button" in {
      view.getElementById("back-link") must haveHref(routes.PptReferenceController.displayPage())
    }

    "display title" in {
      view.select("title").text() must include(messages("enrolment.isUkAddress.title"))
    }

    "display is uk address question" in {
      view.select("h1").text() must include(messages("enrolment.isUkAddress.title"))
    }

    "display is uk address radio buttons" in {
      val radios = view.select("input[type=radio]")
      radios.size() mustBe 2
      radios.get(0).attr("value").text() mustBe IsUkAddress.YES
      radios.get(1).attr("value").text() mustBe IsUkAddress.NO
    }

    "display 'Save and continue' button" in {
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  "A Redisplayed Is UK Address View" should {

    "select previously selected yes" in {
      val form = IsUkAddress
        .form()
        .fill(IsUkAddress(Some(true)))
      val view = createView(form)

      view.select(s"input[value=${IsUkAddress.YES}]").hasAttr("checked") mustBe true
      view.select(s"input[value=${IsUkAddress.NO}]").hasAttr("checked") mustBe false
    }

    "select previously selected no" in {
      val form = IsUkAddress
        .form()
        .fill(IsUkAddress(Some(false)))
      val view = createView(form)

      view.select(s"input[value=${IsUkAddress.YES}]").hasAttr("checked") mustBe false
      view.select(s"input[value=${IsUkAddress.NO}]").hasAttr("checked") mustBe true
    }

    "show validation error when no selection made" in {
      val form = IsUkAddress
        .form()
        .bind(emptyFormData)
      val view = createView(form)

      view must haveGovukGlobalErrorSummary
      view must haveGovukFieldError("value", messages("enrolment.isUkAddress.value.error.empty"))
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(IsUkAddress.form())(request, messages)
    page.render(IsUkAddress.form(), request, messages)
  }

}
