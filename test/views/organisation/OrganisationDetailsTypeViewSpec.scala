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
import play.api.data.Form
import forms.organisation.OrgType.{CHARITABLE_INCORPORATED_ORGANISATION, OVERSEAS_COMPANY_NO_UK_BRANCH, OVERSEAS_COMPANY_UK_BRANCH, OrgType, PARTNERSHIP, REGISTERED_SOCIETY, SOLE_TRADER, TRUST, UK_COMPANY}
import forms.organisation.{ActionEnum, OrganisationType}
import views.html.organisation.organisation_type

class OrganisationDetailsTypeViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[organisation_type]

  private def createView(
    form: Form[OrganisationType] = OrganisationType.form(ActionEnum.Org),
    isGroup: Boolean = false
  ): Document =
    page(form, isGroup)(registrationJourneyRequest, messages)

  "Confirm Organisation Type View" should {

    implicit val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {

      displaySignOutLink(view)
    }

    "display title" in {

      view.select("title").text() must include(messages("organisationDetails.type.title"))
    }

    "display header" in {

      view.getElementsByClass("govuk-caption-l").text() must include(messages("organisationDetails.sectionHeader"))
    }

    "display radio inputs" in {

      radioInputMustBe(1, UK_COMPANY)
      radioInputMustBe(2, OVERSEAS_COMPANY_UK_BRANCH)
      radioInputMustBe(3, OVERSEAS_COMPANY_NO_UK_BRANCH)
      radioInputMustBe(4, PARTNERSHIP)
      radioInputMustBe(5, CHARITABLE_INCORPORATED_ORGANISATION)
      radioInputMustBe(6, REGISTERED_SOCIETY)
      radioInputMustBe(7, SOLE_TRADER)
      radioInputMustBe(8, TRUST)
    }

    "display no hint link" in {

      val link = view.getElementById("organisationDetails-other-type-link")

      link mustBe null
    }

    "display 'Save and continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  "Confirm Organisation Type View for groups" should {

    implicit val view = createView(isGroup = true)

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {

      displaySignOutLink(view)
    }

    "display title" in {

      view.select("title").text() must include(messages("organisationDetails.type.group.title"))
    }

    "display header" in {

      view.getElementsByClass("govuk-caption-l").text() must include(
        messages("organisationDetails.nominated.organisation.sectionHeader")
      )
    }

    "display radio inputs" in {

      radioInputMustBe(1, UK_COMPANY)
      radioInputMustBe(2, PARTNERSHIP, Some("organisationDetails.type.GroupNominatedPartnership"))
      radioInputMustBe(3, OVERSEAS_COMPANY_UK_BRANCH)
    }

    "display hint link" in {

      val link = view.getElementById("organisationDetails-type-link")

      link must containMessage("organisationDetails.type.hint")
      link must haveHref(messages("gform.link"))
    }

    "display 'Save and continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  "Confirm Organisation Based In Uk view when filled" should {

    "display checked radio button" in {

      val form = OrganisationType
        .form(ActionEnum.Org)
        .fill(OrganisationType(UK_COMPANY.toString))
      val view = createView(form)

      view.getElementById("answer").attr("value") mustBe UK_COMPANY.toString
    }

    "display error" when {

      "no radio button checked" in {

        val form = OrganisationType
          .form(ActionEnum.Org)
          .bind(emptyFormData)
        val view = createView(form)

        view must haveGovukFieldError("answer", "Select what type of organisation you want to register")
        view must haveGovukGlobalErrorSummary
      }

      "no radio button checked for representative member" in {

        val form = OrganisationType
          .form(ActionEnum.RepresentativeMember)
          .bind(emptyFormData)
        val view = createView(form)

        view must haveGovukFieldError("answer", "Select the representative member organisation type")
        view must haveGovukGlobalErrorSummary
      }
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(OrganisationType.form(ActionEnum.Org), false)(request, messages)
    page.render(OrganisationType.form(ActionEnum.Org), false, request, messages)
  }

  def radioInputMustBe(number: Int, orgType: OrgType, labelKey: Option[String] = None)(implicit view: Document) = {
    view.getElementById(s"answer${if (number == 1) "" else s"-$number"}").attr("value").text() mustBe orgType.toString
    view.getElementsByClass("govuk-label").get(number - 1).text() mustBe messages(
      labelKey.getOrElse(s"organisationDetails.type.$orgType")
    )
  }

}
