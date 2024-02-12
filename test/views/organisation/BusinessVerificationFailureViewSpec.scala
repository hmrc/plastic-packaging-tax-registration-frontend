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

package views.organisation

import base.unit.UnitViewSpec
import org.scalatest.matchers.must.Matchers
import play.twirl.api.Html
import views.html.organisation.business_verification_failure_page

class BusinessVerificationFailureViewSpec extends UnitViewSpec with Matchers {

  private val page: business_verification_failure_page =
    inject[business_verification_failure_page]

  private def createView(): Html = page()(registrationJourneyRequest, messages)

  "Business Verification Failure Page" should {

    val view: Html = createView()

    "display title" in {
      view.select("title").text() must include(messages("businessEntityVerification.failure.title"))
    }

    "display heading" in {
      view.select("h1").text() must include(messages("businessEntityVerification.failure.heading"))
    }

    "display detail" in {
      view.select("p.govuk-body").text() must include(
        messages("businessEntityVerification.failure.detail")
      )
    }

    "display corp tax helpline details" in {
      val parasText = view.select("p").text
      parasText must include(messages("corpTaxHelpline.intro"))
      parasText must include(messages("corpTaxHelpline.telephone.title"))
      parasText must include(messages("corpTaxHelpline.telephone.detail"))
      parasText must include(messages("corpTaxHelpline.telephone.outsideUK.title"))
      parasText must include(messages("corpTaxHelpline.telephone.outsideUK.detail"))
      parasText must include(messages("corpTaxHelpline.openingTimes.title"))
      parasText must include(messages("corpTaxHelpline.openingTimes.detail.1"))
      parasText must include(messages("corpTaxHelpline.openingTimes.detail.2"))
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f()(request, messages)
    page.render(request, messages)
  }

}
