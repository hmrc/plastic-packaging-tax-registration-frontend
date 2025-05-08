/*
 * Copyright 2025 HM Revenue & Customs
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
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import views.components.Styles
import views.html.organisation.register_as_other_organisation

class RegisterAsOtherOrganisationViewSpec extends UnitViewSpec with Matchers {

  private val page                   = inject[register_as_other_organisation]
  private def createView(): Document = page()(registrationJourneyRequest, messages)

  "Register As Other Organisation View" should {

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {

      displaySignOutLink(view)
    }

    "display title" in {

      view.getElementsByClass(Styles.gdsPageHeading).first() must containMessage(
        "organisationDetails.registerAsOtherOrganisation.title"
      )
    }

    "display page header" in {

      view.getElementById("section-header") must containMessage("organisationDetails.sectionHeader")
    }

    "display paragraphs" in {

      view.getElementsByClass(Styles.gdsPageBodyText).first() must containMessage(
        "organisationDetails.registerAsOtherOrganisation.paragraph.1"
      )

      view.getElementsByClass(Styles.gdsPageBodyText).get(1).text() must include(
        messages(
          "organisationDetails.registerAsOtherOrganisation.paragraph.2",
          messages("organisationDetails.registerAsOtherOrganisation.gform.link.description")
        )
      )

      view.getElementsByClass(Styles.gdsPageBodyText).get(2) must containMessage(
        "organisationDetails.registerAsOtherOrganisation.paragraph.3"
      )
    }

    "display gform link" in {

      view.getElementById("gform-link") must haveHref(
        "https://www.tax.service.gov.uk/submissions/new-form/register-for-plastic-packaging-tax"
      )
      view.getElementById("gform-text") must containMessage(
        "organisationDetails.registerAsOtherOrganisation.gform.link.description"
      )
    }

    "display feedback content" in {
      view.getElementById("feedback-heading") must containMessage("common.feedback.title")
      view.getElementById("feedback-text1") must containMessage("common.feedback.info")
      view.getElementById("feedback-text2") must containMessage(
        "common.feedback.link.description",
        messages("common.feedback.link")
      )
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f()(request, messages)
    page.render(request, messages)
  }

}
