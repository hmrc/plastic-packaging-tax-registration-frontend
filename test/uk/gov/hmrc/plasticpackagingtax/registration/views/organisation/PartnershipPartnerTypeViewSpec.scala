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

package uk.gov.hmrc.plasticpackagingtax.registration.views.organisation

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum.{
  CHARITABLE_INCORPORATED_ORGANISATION,
  LIMITED_LIABILITY_PARTNERSHIP,
  OVERSEAS_COMPANY_NO_UK_BRANCH,
  OVERSEAS_COMPANY_UK_BRANCH,
  PartnerTypeEnum,
  SCOTTISH_LIMITED_PARTNERSHIP,
  SCOTTISH_PARTNERSHIP,
  SOLE_TRADER,
  UK_COMPANY
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation.partnership_partner_type
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class PartnershipPartnerTypeViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[partnership_partner_type]

  private def createView(form: Form[PartnerType] = PartnerType.form()): Document =
    page(form)(journeyRequest, messages)

  "Confirm Partnership Type View" should {

    implicit val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {

      displaySignOutLink(view)
    }

    "display 'Back' button" in {

      view.getElementById("back-link") must haveHref(routes.TaskListController.displayPage())
    }

    "display title" in {

      view.select("title").text() must include(messages("nominated.partner.type.title"))
    }

    "display radio inputs" in {

      radioInputMustBe(1, SOLE_TRADER)
      radioInputMustBe(2, UK_COMPANY)
      radioInputMustBe(3, LIMITED_LIABILITY_PARTNERSHIP)
      radioInputMustBe(4, SCOTTISH_PARTNERSHIP)
      radioInputMustBe(5, SCOTTISH_LIMITED_PARTNERSHIP)
      radioInputMustBe(6, CHARITABLE_INCORPORATED_ORGANISATION)
      radioInputMustBe(7, OVERSEAS_COMPANY_UK_BRANCH)
      radioInputMustBe(8, OVERSEAS_COMPANY_NO_UK_BRANCH)
    }

    "display 'Save and continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(PartnerType.form())(request, messages)
    page.render(PartnerType.form(), request, messages)
  }

  def radioInputMustBe(
    number: Int,
    partnershipPartnerType: PartnerTypeEnum,
    labelKey: Option[String] = None
  )(implicit view: Document) = {
    view.getElementById(s"answer${if (number == 1) "" else s"-$number"}").attr(
      "value"
    ).text() mustBe partnershipPartnerType.toString
    view.getElementsByClass("govuk-label").get(number - 1).text() mustBe messages(
      labelKey.getOrElse(s"nominated.partner.type.$partnershipPartnerType")
    )
  }

}
