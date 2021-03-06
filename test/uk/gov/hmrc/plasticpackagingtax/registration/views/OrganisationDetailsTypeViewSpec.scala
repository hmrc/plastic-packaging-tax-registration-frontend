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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrgType.{
  CHARITY_OR_NOT_FOR_PROFIT,
  PARTNERSHIP,
  SOLE_TRADER,
  UK_COMPANY
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrganisationType
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation_type
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class OrganisationDetailsTypeViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[organisation_type]

  private def createView(form: Form[OrganisationType] = OrganisationType.form()): Document =
    page(form)(request, messages)

  "Confirm Organisation Based In Uk View" should {

    "have proper messages for labels" in {
      messages must haveTranslationFor("organisationDetails.type.title")
      messages must haveTranslationFor("organisationDetails.type.empty.error")
    }

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {

      displaySignOutLink(view)
    }

    "display 'Back' button" in {

      view.getElementById("back-link") must haveHref(
        routes.OrganisationDetailsConfirmOrgBasedInUkController.displayPage()
      )
    }

    "display title" in {

      view.select("title").text() must include(messages("organisationDetails.type.title"))
    }

    "display header" in {

      view.getElementsByClass("govuk-caption-xl").text() must include(
        messages("organisationDetails.sectionHeader")
      )
    }

    "display radio inputs" in {

      view.getElementById("answer").attr("value").text() mustBe UK_COMPANY.toString
      view.getElementsByClass("govuk-label").first().text() mustBe UK_COMPANY.toString
      view.getElementById("answer-2").attr("value").text() mustBe SOLE_TRADER.toString
      view.getElementsByClass("govuk-label").get(1).text() mustBe SOLE_TRADER.toString
      view.getElementById("answer-3").attr("value").text() mustBe PARTNERSHIP.toString
      view.getElementsByClass("govuk-label").get(2).text() mustBe PARTNERSHIP.toString
      view.getElementById("answer-4").attr("value").text() mustBe CHARITY_OR_NOT_FOR_PROFIT.toString
      view.getElementsByClass("govuk-label").get(3).text() mustBe CHARITY_OR_NOT_FOR_PROFIT.toString
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

    "display checked radio button" in {

      val form = OrganisationType
        .form()
        .fill(OrganisationType(UK_COMPANY.toString))
      val view = createView(form)

      view.getElementById("answer").attr("value") mustBe UK_COMPANY.toString
    }

    "display error" when {

      "no radio button checked" in {

        val form = OrganisationType
          .form()
          .fillAndValidate(OrganisationType(""))
        val view = createView(form)

        view must haveGovukGlobalErrorSummary
      }
    }
  }
}
