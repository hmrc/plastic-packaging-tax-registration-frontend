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
import org.scalatest.matchers.must.Matchers
import play.twirl.api.Html
import uk.gov.hmrc.plasticpackagingtax.registration.views.components.Styles._
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.enrolment.verify_organisation_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class VerifyOrganisationViewSpec extends UnitViewSpec with Matchers {

  private val page: verify_organisation_page = inject[verify_organisation_page]

  private def createView(): Html =
    page()(journeyRequest, messages)

  "Verify Organisation Page view" should {

    val view: Html = createView()

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    "display 'Back' button" in {
      view.getElementById("back-link") mustBe null
    }

    "display title" in {
      view.select("title").text() must include(messages("verify.organisation.title"))
    }

    "display 'Confirm and continue' button" in {
      view.getElementById("submit").text() mustBe "Save and continue"
    }

    "display paragraph " in {

      val paragraphBodyText = view.getElementsByClass(gdsPageBodyText)
      paragraphBodyText.get(0) must containMessage("verify.organisation.detail.1")
      paragraphBodyText.get(1) must containMessage("verify.organisation.detail.2")
    }

  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f()(request, messages)
    page.render(request, messages)
  }

}
