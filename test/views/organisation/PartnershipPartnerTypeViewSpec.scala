/*
 * Copyright 2026 HM Revenue & Customs
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
import forms.organisation.PartnerType
import forms.organisation.PartnerTypeEnum
import forms.organisation.PartnerType.FormMode
import forms.organisation.PartnerTypeEnum.{LIMITED_LIABILITY_PARTNERSHIP, OVERSEAS_COMPANY_UK_BRANCH, REGISTERED_SOCIETY, SCOTTISH_LIMITED_PARTNERSHIP, SCOTTISH_PARTNERSHIP, SOLE_TRADER, UK_COMPANY}
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import play.api.mvc.Call
import spec.PptTestData
import views.html.organisation.partner_type
import org.scalatest.matchers.should.Matchers.shouldBe

class PartnershipPartnerTypeViewSpec extends UnitViewSpec with Matchers with PptTestData {

  private val submitLink = Call("POST", "/submit")
  private val page       = inject[partner_type]

  private val registrationWithOtherPartners = aRegistration(
    withPartnershipDetails(partnershipDetails = Some(generalPartnershipDetailsWithPartners))
  )

  private def createViewNominated(form: Form[PartnerType] = PartnerType.form(FormMode.NominatedPartnerType)): Document =
    page(form, registrationWithOtherPartners.nominatedPartner.map(_.id), submitLink)(
      registrationJourneyRequest.copy(registration = registrationWithOtherPartners),
      messages
    )

  private def createViewForOthers(form: Form[PartnerType] = PartnerType.form(FormMode.OtherPartnerType)): Document =
    page(form, None, submitLink)(registrationJourneyRequest, messages)

  "Confirm Partnership Type View for Nominated" should {

    implicit val view = createViewNominated()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) shouldBe true
    }

    "display sign out link" in {

      displaySignOutLink(view)
    }

    "display title for nominated partner" in {

      view.select("title").text() must include(messages("nominated.partner.type.title"))
    }

    "display radio inputs" in {

      radioInputshouldBe(1, SOLE_TRADER)
      radioInputshouldBe(2, UK_COMPANY)
      radioInputshouldBe(3, REGISTERED_SOCIETY)
      radioInputshouldBe(4, LIMITED_LIABILITY_PARTNERSHIP)
      radioInputshouldBe(5, SCOTTISH_PARTNERSHIP)
      radioInputshouldBe(6, SCOTTISH_LIMITED_PARTNERSHIP)
      radioInputshouldBe(7, OVERSEAS_COMPANY_UK_BRANCH)
    }

    "display 'Save and continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() shouldBe "Save and continue"
    }

  }

  "Confirm Partnership Type View for Other" should {

    implicit val view1 = createViewForOthers()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view1) shouldBe true
    }

    "display sign out link" in {

      displaySignOutLink(view1)
    }

    "display title for other partner" in {

      view1.select("title").text() must include(messages("other.partner.type.title"))
    }

    "display radio inputs" in {

      radioInputshouldBe(1, SOLE_TRADER)
      radioInputshouldBe(2, UK_COMPANY)
      radioInputshouldBe(3, REGISTERED_SOCIETY)
      radioInputshouldBe(4, LIMITED_LIABILITY_PARTNERSHIP)
      radioInputshouldBe(5, SCOTTISH_PARTNERSHIP)
      radioInputshouldBe(6, SCOTTISH_LIMITED_PARTNERSHIP)
      radioInputshouldBe(7, OVERSEAS_COMPANY_UK_BRANCH)
    }

    "display 'Save and continue' button" in {

      view1 must containElementWithID("submit")
      view1.getElementById("submit").text() shouldBe "Save and continue"
    }

  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(PartnerType.form(FormMode.NominatedPartnerType), None, submitLink)(registrationJourneyRequest, messages)
    page.render(PartnerType.form(FormMode.NominatedPartnerType), None, submitLink, registrationJourneyRequest, messages)
  }

  def radioInputshouldBe(number: Int, partnershipPartnerType: PartnerTypeEnum, labelKey: Option[String] = None)(implicit
    view: Document
  ) = {
    view.getElementById(s"answer${if (number == 1) "" else s"-$number"}").attr(
      "value"
    ).text() shouldBe partnershipPartnerType.value
    view.getElementsByClass("govuk-label").get(number - 1).text() shouldBe messages(
      labelKey.getOrElse(s"partner.type.${partnershipPartnerType.value}")
    )
  }

}
