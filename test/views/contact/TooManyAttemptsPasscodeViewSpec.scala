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

package views.contact

import base.unit.UnitViewSpec
import org.scalatest.matchers.must.Matchers
import views.html.contact.too_many_attempts_passcode_page

class TooManyAttemptsPasscodeViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[too_many_attempts_passcode_page]

  "Too Many Attempts Passcode View" should {

    val view = page()(request = registrationJourneyRequest, messages = messages)

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display title" in {

      view.select("title").text() must include(messages("primaryContactDetails.tooManyAttempts.passcode.title"))
    }

    "display header" in {

      view.getElementsByClass("govuk-caption-l").text() must include(messages("primaryContactDetails.sectionHeader"))
    }

    "display paragraph" in {
      view.getElementById("tooManyAttempts-passcode-id-1").text() must include(messages("primaryContactDetails.tooManyAttempts.passcode.1"))
      view.getElementById("tooManyAttempts-passcode-id-2").text() must include(messages("primaryContactDetails.tooManyAttempts.passcode.2"))
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f()(request, messages)
    page.render(request, messages)
  }

}
