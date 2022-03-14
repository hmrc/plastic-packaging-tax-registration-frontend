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

package uk.gov.hmrc.plasticpackagingtax.registration.views.partner

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.test.FakeRequest
import uk.gov.hmrc.plasticpackagingtax.registration.views.components.Styles
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partner.partner_registration_available_soon_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class PartnerRegistrationAvailableSoonPageViewSpec extends UnitViewSpec with Matchers {

  private val page                   = inject[partner_registration_available_soon_page]
  private def createView(): Document = page()(journeyRequest, messages)

  "Partner Registration Available Soon Page View" should {

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {

      displaySignOutLink(view)
    }

    "display title" in {

      view.getElementsByClass(Styles.gdsPageHeading).first() must containMessage(
        "partnership.registrationAvailableSoon.title"
      )
    }

    "display page header" in {

      view.getElementById("section-header") must containMessage(
        "partnership.registrationAvailableSoon.header"
      )
    }

    "display info and link" in {

      view.getElementById("registrationAvailableSoon-link") must haveHref(
        "mailto:pptpartnerships@hmrc.gov.uk"
      )
      view.getElementById("registrationAvailableSoon-link") must containMessage(
        "partnership.registrationAvailableSoon.link"
      )
    }

    "display feedback link for authenticated users" in {

      view.getElementById("govuk-feedback-link") must haveHref(
        "http://localhost:9250/contact/beta-feedback?service=plastic-packaging-tax&backUrl=http://localhost:8503/"
      )
      view.getElementById("govuk-feedback-link") must containMessage(
        "partnership.registrationAvailableSoon.feedback.link"
      )
    }

    "display feedback link for unauthenticated users" in {

      val unauthenticatedView = page()(FakeRequest(), messages)
      unauthenticatedView.getElementById("govuk-feedback-link") must haveHref(
        "http://localhost:9250/contact/beta-feedback-unauthenticated?service=plastic-packaging-tax&backUrl=http://localhost:8503/"
      )
      unauthenticatedView.getElementById("govuk-feedback-link") must containMessage(
        "partnership.registrationAvailableSoon.feedback.link"
      )
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f()(request, messages)
    page.render(request, messages)
  }

}
