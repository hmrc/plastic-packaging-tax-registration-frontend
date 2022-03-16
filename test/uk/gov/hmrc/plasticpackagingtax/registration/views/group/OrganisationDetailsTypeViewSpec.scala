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

package uk.gov.hmrc.plasticpackagingtax.registration.views.group

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.group.routes
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType.{
  CHARITABLE_INCORPORATED_ORGANISATION,
  OVERSEAS_COMPANY_NO_UK_BRANCH,
  OVERSEAS_COMPANY_UK_BRANCH,
  OrgType,
  PARTNERSHIP,
  UK_COMPANY
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrganisationType
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.group.organisation_type
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class OrganisationDetailsTypeViewSpec extends UnitViewSpec with Matchers {

  private val page    = inject[organisation_type]
  private val isGroup = true

  private def createView(
    form: Form[OrganisationType] = OrganisationType.form(isGroup),
    isFirstMember: Boolean = true
  ): Document =
    page(form = form,
         isFirstMember = isFirstMember,
         memberId = Some(groupMember.id),
         routes.OrganisationDetailsTypeController.submitNewMember()
    )(journeyRequest, messages)

  "Confirm Organisation Type View" should {

    implicit val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {

      displaySignOutLink(view)
    }

    "display 'Back' button" in {

      view.getElementById("back-link") must haveHref(commonRoutes.TaskListController.displayPage())
    }

    "display title" in {

      view.select("title").text() must include(messages("organisationDetails.other.group.title"))
    }

    "display title for next group member" in {
      val view: Document = createView(form = OrganisationType.form(isGroup), isFirstMember = false)
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

      view.getElementById("back-link") must haveHref(commonRoutes.TaskListController.displayPage())
    }

    "display title" in {

      view.select("title").text() must include(messages("organisationDetails.other.group.title"))
    }

    "display title for next group member" in {
      val view: Document = createView(form = OrganisationType.form(isGroup), isFirstMember = false)
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
      radioInputMustBe(3, OVERSEAS_COMPANY_UK_BRANCH)
    }

    "display 'Save and continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  "Confirm Organisation Based In Uk view when filled" should {

    "display checked radio button" in {

      val form = OrganisationType
        .form(isGroup)
        .fill(OrganisationType(UK_COMPANY.toString))
      val view = createView(form)

      view.getElementById("answer").attr("value") mustBe UK_COMPANY.toString
    }

    "display error" when {

      "no radio button checked" in {

        val form = OrganisationType
          .form(isGroup)
          .bind(emptyFormData)
        val view = createView(form)

        view must haveGovukFieldError("answer", "Select the type of organisation you want to add")
        view must haveGovukGlobalErrorSummary
      }
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(OrganisationType.form(isGroup),
           true,
           Some(groupMember.id),
           routes.OrganisationDetailsTypeController.submitNewMember()
    )(request, messages)
    page.render(OrganisationType.form(isGroup),
                isFirstMember = true,
                Some(groupMember.id),
                routes.OrganisationDetailsTypeController.submitNewMember(),
                request,
                messages
    )
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
