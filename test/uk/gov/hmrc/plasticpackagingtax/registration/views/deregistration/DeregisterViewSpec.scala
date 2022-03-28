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
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import uk.gov.hmrc.plasticpackagingtax.registration.forms.deregistration.DeregisterForm
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.deregistration.deregister_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class DeregisterViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[deregister_page]

  private def createView(form: Form[Boolean] = DeregisterForm.form()): Document =
    page(form)(request, messages)

  "The Initial Deregister View" should {

    val view = createView()

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    "display 'Back' button" in {
      view.getElementById("back-link") must haveHref(appConfig.pptAccountUrl)
    }

    "display title" in {
      view.select("title").text() must include(messages("deregistration.deregister.title"))
    }

    "display page heading" in {
      view.select("h1").text() must include(messages("deregistration.deregister.title"))
    }

    "display page detail" in {
      val mainContent = view.select("main").text()
      mainContent must include(messages("deregistration.deregister.eligibility"))
      mainContent must include(messages("deregistration.deregister.eligibility.1"))
      mainContent must include(messages("deregistration.deregister.eligibility.2"))
      mainContent must include(messages("deregistration.deregister.eligibility.3"))
      mainContent must include(messages("deregistration.deregister.eligibility.4"))
      mainContent must include(messages("deregistration.deregister.detail1"))
      mainContent must include(messages("deregistration.deregister.detail2"))
    }

    "display deregister question" in {
      view.select("legend").text() must include(messages("deregistration.deregister.label"))
    }

    "display deregister radio buttons" in {
      val radios = view.select("input[type=radio]")
      radios.size() mustBe 2
      radios.get(0).attr("value").text() mustBe DeregisterForm.YES
      radios.get(1).attr("value").text() mustBe DeregisterForm.NO
    }

    "display 'Save and continue' button" in {
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  "A Redisplayed Is UK Address View" should {

    "select previously selected yes" in {
      val form = DeregisterForm
        .form()
        .fill(true)
      val view = createView(form)

      view.select(s"input[value=${DeregisterForm.YES}]").hasAttr("checked") mustBe true
      view.select(s"input[value=${DeregisterForm.NO}]").hasAttr("checked") mustBe false
    }

    "select previously selected no" in {
      val form = DeregisterForm
        .form()
        .fill(false)
      val view = createView(form)

      view.select(s"input[value=${DeregisterForm.YES}]").hasAttr("checked") mustBe false
      view.select(s"input[value=${DeregisterForm.NO}]").hasAttr("checked") mustBe true
    }

    "show validation error when no selection made" in {
      val form = DeregisterForm
        .form()
        .bind(emptyFormData)
      val view = createView(form)

      view must haveGovukGlobalErrorSummary
      view must haveGovukFieldError("value", messages("deregistration.deregister.empty.error"))
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(DeregisterForm.form())(request, messages)
    page.render(DeregisterForm.form(), request, messages)
  }

}
