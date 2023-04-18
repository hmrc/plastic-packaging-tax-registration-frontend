/*
 * Copyright 2023 HM Revenue & Customs
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

package views.organisation

import base.unit.UnitViewSpec
import org.scalatest.matchers.must.Matchers
import play.twirl.api.Html
import views.html.organisation.sole_trader_verification_failure_page

class SoleTraderVerificationFailureViewSpec extends UnitViewSpec with Matchers {

  private val page: sole_trader_verification_failure_page =
    inject[sole_trader_verification_failure_page]

  private def createView(): Html = page()(registrationJourneyRequest, messages)

  "Sole Trader Verification Failure Page" should {

    val view: Html = createView()

    // Minimal asserts to verify the page is specific to sole traders and refers to self assessment
    "display sole trader heading" in {
      view.select("h1").text() must include(messages("soleTraderEntityVerification.failure.title"))
    }

    "display self assessment tax helpline details" in {
      val parasText = view.select("p").text
      parasText must include(messages("selfAssessmentHelpline.intro"))
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f()(request, messages)
    page.render(request, messages)
  }

}
