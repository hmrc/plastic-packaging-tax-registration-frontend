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
import views.html.enrolment.verification_failure_page

class VerificationFailureViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[verification_failure_page]

  private def createView(): Document = page("contact-us-href") (registrationJourneyRequest, messages)

  "Enrolment not verified page" should {

    val view = createView()

    "contain title" in {
      view.select("title").text() must include(messages("enrolment.not.verified.title"))
    }

    "contain heading" in {
      view.select("h1").text() mustBe messages("enrolment.not.verified.title")
    }

    "contain detail" in {
      val detail = view.select("p").text()
      detail must include(messages("enrolment.not.verified.detail.1"))
    }

    "display 'Save and continue' button" in {
      view.getElementById("submit").text() mustBe "Try again"
    }
  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    page.f("contact-us-href") (request, messages)
    page.render("contact-us-href", request, messages)
  }

}
