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
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import controllers.organisation.{
  routes => organisationRoutes
}
import forms.organisation.PartnerType.{form, FormMode}
import forms.organisation.PartnerTypeEnum.{
  GENERAL_PARTNERSHIP,
  LIMITED_LIABILITY_PARTNERSHIP,
  LIMITED_PARTNERSHIP,
  SCOTTISH_LIMITED_PARTNERSHIP,
  SCOTTISH_PARTNERSHIP
}
import forms.organisation.{
  PartnerType,
  PartnerTypeEnum
}
import views.html.organisation.partnership_type

class PartnershipTypeViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[partnership_type]

  private def createView(
    form: Form[PartnerType] = PartnerType.form(FormMode.NominatedPartnerType)
  ): Document =
    page(form)(registrationJourneyRequest, messages)

  "Partnership Type View" should {

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {

      displaySignOutLink(view)
    }

    "display 'Back' button" in {

      view.getElementById("back-link") must haveHref(
        organisationRoutes.OrganisationDetailsTypeController.displayPage()
      )
    }

    "display title" in {

      view.select("title").text() must include(messages("partnership.type.title"))
    }

    "display header" in {

      view.getElementsByClass("govuk-caption-l").text() must include(
        messages("partnership.name.section-header")
      )
    }

    "display radio inputs" in {

      view.getElementById("answer").attr("value").text() mustBe GENERAL_PARTNERSHIP.toString
      view.getElementsByClass("govuk-label").first().text() mustBe PartnerTypeEnum.displayName(
        GENERAL_PARTNERSHIP
      )
      view.getElementById("answer-2").attr(
        "value"
      ).text() mustBe LIMITED_LIABILITY_PARTNERSHIP.toString
      view.getElementsByClass("govuk-label").get(1).text() mustBe PartnerTypeEnum.displayName(
        LIMITED_LIABILITY_PARTNERSHIP
      )
      view.getElementById("answer-3").attr("value").text() mustBe LIMITED_PARTNERSHIP.toString
      view.getElementsByClass("govuk-label").get(2).text() mustBe PartnerTypeEnum.displayName(
        LIMITED_PARTNERSHIP
      )
      view.getElementById("answer-4").attr("value").text() mustBe SCOTTISH_PARTNERSHIP.toString
      view.getElementsByClass("govuk-label").get(3).text() mustBe PartnerTypeEnum.displayName(
        SCOTTISH_PARTNERSHIP
      )
      view.getElementById("answer-5").attr(
        "value"
      ).text() mustBe SCOTTISH_LIMITED_PARTNERSHIP.toString
      view.getElementsByClass("govuk-label").get(4).text() mustBe PartnerTypeEnum.displayName(
        SCOTTISH_LIMITED_PARTNERSHIP
      )
    }

    "display 'Save and continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  "Partnership Type when filled" should {

    "display checked radio button" in {

      val form = PartnerType
        .form(FormMode.NominatedPartnerType)
        .fill(PartnerType(LIMITED_LIABILITY_PARTNERSHIP))
      val view = createView(form)

      view.getElementById("answer-2").attr("value") mustBe LIMITED_LIABILITY_PARTNERSHIP.toString
    }

    "display error" when {
      "no radio button checked - partnership" in {

        val form = PartnerType
          .form(FormMode.PartnershipType)
          .bind(emptyFormData)
        val view = createView(form)

        view must haveGovukFieldError("answer",
                                      "Select the type of partnership you are registering"
        )
        view must haveGovukGlobalErrorSummary
      }
      "no radio button checked - nominated" in {

        val form = PartnerType
          .form(FormMode.NominatedPartnerType)
          .bind(emptyFormData)
        val view = createView(form)

        view must haveGovukFieldError("answer", "Select the nominated partner’s organisation type")
        view must haveGovukGlobalErrorSummary
      }
      "no radio button checked - other" in {

        val form = PartnerType
          .form(FormMode.OtherPartnerType)
          .bind(emptyFormData)
        val view = createView(form)

        view must haveGovukFieldError("answer",
                                      "Select the partner organisation type"
        )
        view must haveGovukGlobalErrorSummary
      }
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(form(FormMode.NominatedPartnerType))(request, messages)
    page.render(form(FormMode.NominatedPartnerType), request, messages)
  }

}
