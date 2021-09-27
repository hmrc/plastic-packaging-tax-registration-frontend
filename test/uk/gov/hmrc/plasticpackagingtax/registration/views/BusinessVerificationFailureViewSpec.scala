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

package uk.gov.hmrc.plasticpackagingtax.registration.views

import base.unit.UnitViewSpec
import org.scalatest.matchers.must.Matchers
import play.twirl.api.Html
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.business_verification_failure_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class BusinessVerificationFailureViewSpec extends UnitViewSpec with Matchers {

  private val page: business_verification_failure_page =
    instanceOf[business_verification_failure_page]

  private def createView(): Html = page()(journeyRequest, messages)

  "Business Verification Failure Page" should {

    "have proper messages for labels" in {
      messages must haveTranslationFor("businessEntityVerification.failure.title")
      messages must haveTranslationFor("businessEntityVerification.failure.heading")
      messages must haveTranslationFor("businessEntityVerification.failure.detail")
    }

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
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f()(request, messages)
    page.render(request, messages)
  }

}
