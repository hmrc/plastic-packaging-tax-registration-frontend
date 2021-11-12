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

package uk.gov.hmrc.plasticpackagingtax.registration.views.group

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrgType.{
  CHARITABLE_INCORPORATED_ORGANISATION,
  OVERSEAS_COMPANY,
  OVERSEAS_COMPANY_NO_UK_BRANCH,
  OrgType,
  PARTNERSHIP,
  UK_COMPANY
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrganisationType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrganisationType.form
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.group.organisation_type
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class OrganisationDetailsTypeViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[organisation_type]

  private def createView(
    form: Form[OrganisationType] = OrganisationType.form(),
    isFirstMember: Boolean = true
  ): Document =
    page(form, isFirstMember)(journeyRequest, messages)

  "Confirm Organisation Type View" should {

    implicit val view = createView()

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

      view.select("title").text() must include(messages("organisationDetails.other.group.title"))
    }

    "display title for next group member" in {
      val view: Document = createView(OrganisationType.form(), false)
      view.select("title").text() must include(
        messages("organisationDetails.other.next.group.title")
      )
    }

    "display header" in {

      view.getElementsByClass("govuk-caption-l").text() must include(
        messages("organisationDetails.other.organisation.sectionHeader")
      )
    }

    "display 'Save and continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  "Confirm Organisation Type View for groups" should {

    implicit val view = createView()

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

      view.select("title").text() must include(messages("organisationDetails.other.group.title"))
    }

    "display title for next group member" in {
      val view: Document = createView(OrganisationType.form(), false)
      view.select("title").text() must include(
        messages("organisationDetails.other.next.group.title")
      )
    }

    "display header" in {

      view.getElementsByClass("govuk-caption-l").text() must include(
        messages("organisationDetails.other.organisation.sectionHeader")
      )
    }

    "display radio inputs" in {

      radioInputMustBe(1, UK_COMPANY)
      radioInputMustBe(2, PARTNERSHIP, Some("organisationDetails.type.GroupPartnership"))
      radioInputMustBe(3, CHARITABLE_INCORPORATED_ORGANISATION)
      radioInputMustBe(4, OVERSEAS_COMPANY, Some("organisationDetails.type.GroupOverseasCompany"))
      radioInputMustBe(5,
                       OVERSEAS_COMPANY_NO_UK_BRANCH,
                       Some("organisationDetails.type.GroupOverseasCompanyNoBranchInUK")
      )
    }

    "display 'Save and continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
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
          .bind(emptyFormData)
        val view = createView(form)

        view must haveGovukFieldError("answer", "This field is required")
        view must haveGovukGlobalErrorSummary
      }
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(form(), true)(request, messages)
    page.render(form(), true, request, messages)
  }

  def radioInputMustBe(number: Int, orgType: OrgType, labelKey: Option[String] = None)(implicit
    view: Document
  ) = {
    view.getElementById(s"answer${if (number == 1) "" else s"-$number"}").attr(
      "value"
    ).text() mustBe orgType.toString
    view.getElementsByClass("govuk-label").get(number - 1).text() mustBe messages(
      labelKey.getOrElse(s"organisationDetails.type.$orgType")
    )
  }

}
