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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.PartnershipTypeEnum.{
  GENERAL_PARTNERSHIP,
  LIMITED_LIABILITY_PARTNERSHIP,
  LIMITED_PARTNERSHIP,
  SCOTTISH_LIMITED_PARTNERSHIP,
  SCOTTISH_PARTNERSHIP
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.{OrganisationType, PartnershipType}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partnership_type
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class PartnershipTypeViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[partnership_type]

  private def createView(form: Form[PartnershipType] = PartnershipType.form()): Document =
    page(form)(request, messages)

  "Partnership Type View" should {

    "have proper messages for labels" in {
      messages must haveTranslationFor("partnership.type.title")
      messages must haveTranslationFor("organisationDetails.sectionHeader")
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
        routes.OrganisationDetailsTypeController.displayPage()
      )
    }

    "display title" in {

      view.select("title").text() must include(messages("partnership.type.title"))
    }

    "display header" in {

      view.getElementsByClass("govuk-caption-xl").text() must include(
        messages("organisationDetails.sectionHeader")
      )
    }

    "display radio inputs" in {

      view.getElementById("answer").attr("value").text() mustBe GENERAL_PARTNERSHIP.toString
      view.getElementsByClass("govuk-label").first().text() mustBe GENERAL_PARTNERSHIP.toString
      view.getElementById("answer-2").attr(
        "value"
      ).text() mustBe LIMITED_LIABILITY_PARTNERSHIP.toString
      view.getElementsByClass("govuk-label").get(
        1
      ).text() mustBe LIMITED_LIABILITY_PARTNERSHIP.toString
      view.getElementById("answer-3").attr("value").text() mustBe LIMITED_PARTNERSHIP.toString
      view.getElementsByClass("govuk-label").get(2).text() mustBe LIMITED_PARTNERSHIP.toString
      view.getElementById("answer-4").attr("value").text() mustBe SCOTTISH_PARTNERSHIP.toString
      view.getElementsByClass("govuk-label").get(3).text() mustBe SCOTTISH_PARTNERSHIP.toString
      view.getElementById("answer-5").attr(
        "value"
      ).text() mustBe SCOTTISH_LIMITED_PARTNERSHIP.toString
      view.getElementsByClass("govuk-label").get(
        4
      ).text() mustBe SCOTTISH_LIMITED_PARTNERSHIP.toString
    }

    "display 'Save And Continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and Continue"
    }

    "display 'Save and come back later' button" in {

      view.getElementById("save_and_come_back_later").text() mustBe "Save and come back later"
    }
  }

  "Partnership Type when filled" should {

    "display checked radio button" in {

      val form = PartnershipType
        .form()
        .fill(PartnershipType(LIMITED_LIABILITY_PARTNERSHIP.toString))
      val view = createView(form)

      view.getElementById("answer-2").attr("value") mustBe LIMITED_LIABILITY_PARTNERSHIP.toString
    }

    "display error" when {

      "no radio button checked" in {

        val form = PartnershipType
          .form()
          .fillAndValidate(PartnershipType(""))
        val view = createView(form)

        view must haveGovukGlobalErrorSummary
      }
    }
  }
}