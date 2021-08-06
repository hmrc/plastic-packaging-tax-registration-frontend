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
import play.api.data.Form
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.ConfirmOrganisationBasedInUk
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.confirm_organisation_based_in_uk
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class ConfirmOrganisationBasedInUkViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[confirm_organisation_based_in_uk]

  private def createView(
    form: Form[ConfirmOrganisationBasedInUk] = ConfirmOrganisationBasedInUk.form()
  ): Document =
    page(form)(request, messages)

  "Confirm Organisation Based In Uk View" should {

    "have proper messages for labels" in {
      messages must haveTranslationFor("organisationDetails.sectionHeader")
      messages must haveTranslationFor("organisationDetails.basedInUk.title")
      messages must haveTranslationFor("organisationDetails.basedInUk.empty.error")
    }

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {

      displaySignOutLink(view)
    }

    "display 'Back' button" in {

      view.getElementById("back-link") must haveHref(routes.RegistrationController.displayPage())
    }

    "display title" in {

      view.select("title").text() must include(messages("organisationDetails.basedInUk.title"))
    }

    "display header" in {

      view.getElementsByClass("govuk-caption-xl").text() must include(
        messages("organisationDetails.sectionHeader")
      )
    }

    "display radio inputs" in {

      view must containElementWithID("answer")
      view.getElementsByClass("govuk-label").first().text() mustBe "Yes"
      view must containElementWithID("answer-2")
      view.getElementsByClass("govuk-label").get(1).text() mustBe "No"
    }

    "display 'Save And Continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and Continue"
    }

    "display 'Save and come back later' button" in {

      view.getElementById("save_and_come_back_later").text() mustBe "Save and come back later"
    }
  }

  "Confirm Organisation Based In Uk view when filled" should {

    "display radio button checked" in {

      val form = ConfirmOrganisationBasedInUk
        .form()
        .fill(ConfirmOrganisationBasedInUk("yes"))
      val view = createView(form)

      view.getElementById("answer").attr("value") mustBe "yes"
    }

    "display error" when {

      "no radio button checked" in {

        val form = ConfirmOrganisationBasedInUk
          .form()
          .fillAndValidate(ConfirmOrganisationBasedInUk(None))
        val view = createView(form)

        view must haveGovukGlobalErrorSummary
      }
    }
  }

  // TODO: we need a better way of achieving the minimum test code coverage than doing this!
  "validate other rendering methods" in {
    page.f(ConfirmOrganisationBasedInUk.form())(request, messages).select(
      "title"
    ).text() must include(messages("organisationDetails.basedInUk.title"))
    page.render(ConfirmOrganisationBasedInUk.form(), request, messages).select(
      "title"
    ).text() must include(messages("organisationDetails.basedInUk.title"))
  }

}
