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
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.test.FakeRequest
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation_type_not_supported
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class OrganisationTypeNotSupportedViewSpec extends UnitViewSpec with Matchers {

  private val page                   = instanceOf[organisation_type_not_supported]
  private def createView(): Document = page()(request, messages)

  "Organisation Type Not Supported View" should {

    "have proper messages for labels" in {
      messages must haveTranslationFor("organisationDetails.notSupportCompanyTypePage.heading")
      messages must haveTranslationFor("organisationDetails.notSupportCompanyTypePage.paragraph.1")
      messages must haveTranslationFor("organisationDetails.notSupportCompanyTypePage.tellUs")
      messages must haveTranslationFor("organisationDetails.notSupportCompanyTypePage.paragraph.2")
      messages must haveTranslationFor("organisationDetails.feedback")
      messages must haveTranslationFor("organisationDetails.feedback.link")
    }

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {

      displaySignOutLink(view)
    }

    "display title" in {

      view.getElementsByClass("govuk-heading-xl").first() must containMessage(
        "organisationDetails.notSupportCompanyTypePage.heading"
      )
    }

    "display page header" in {

      view.getElementById("section-header") must containMessage("organisationDetails.sectionHeader")
    }

    "display info paragraph" in {

      view.getElementsByClass("govuk-body").first() must containMessage(
        "organisationDetails.notSupportCompanyTypePage.paragraph.1"
      )
    }

    "display 'tell us what you think' paragraph" in {

      view.getElementsByClass("govuk-heading-m").first() must containMessage(
        "organisationDetails.notSupportCompanyTypePage.tellUs"
      )
      view.getElementsByClass("govuk-body").get(1) must containMessage(
        "organisationDetails.notSupportCompanyTypePage.paragraph.2"
      )
    }

    "display feedback link for authenticated users" in {

      view.getElementsByClass("govuk-link").get(2) must haveHref(
        "http://localhost:9250/contact/beta-feedback?service=plastic-packaging-tax&backUrl=http://localhost:9250/"
      )
      view.getElementsByClass("govuk-link").get(2) must containMessage(
        "organisationDetails.feedback.link"
      )
    }

    "display feedback link for unauthenticated users" in {

      val unauthenticatedView = page()(FakeRequest(), messages)
      unauthenticatedView.getElementsByClass("govuk-link").get(1) must haveHref(
        "http://localhost:9250/contact/beta-feedback-unauthenticated?service=plastic-packaging-tax&backUrl=http://localhost:9250/"
      )
      unauthenticatedView.getElementsByClass("govuk-link").get(1) must containMessage(
        "organisationDetails.feedback.link"
      )
    }
  }

}
