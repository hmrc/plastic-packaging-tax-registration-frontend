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

import base.PptTestData
import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import play.api.mvc.Call
import play.api.test.FakeRequest
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum.{
  CHARITABLE_INCORPORATED_ORGANISATION,
  LIMITED_LIABILITY_PARTNERSHIP,
  OVERSEAS_COMPANY_NO_UK_BRANCH,
  OVERSEAS_COMPANY_UK_BRANCH,
  PartnerTypeEnum,
  REGISTERED_SOCIETY,
  SCOTTISH_LIMITED_PARTNERSHIP,
  SCOTTISH_PARTNERSHIP,
  SOLE_TRADER,
  UK_COMPANY
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{
  AuthenticatedRequest,
  JourneyRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation.partner_type
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest
import utils.FakeRequestCSRFSupport.CSRFFakeRequest

@ViewTest
class PartnershipPartnerTypeViewSpec extends UnitViewSpec with Matchers {

  private val submitLink = Call("POST", "/submit")
  private val page       = inject[partner_type]

  private val registrationWithOtherPartners = aRegistration(
    withPartnershipDetails(partnershipDetails = Some(generalPartnershipDetailsWithPartners))
  )

  val journeyReqForOthers = JourneyRequest(
    new AuthenticatedRequest(FakeRequest().withCSRFToken, PptTestData.newUser(), appConfig),
    registrationWithOtherPartners,
    appConfig
  )

  private def createViewNominated(form: Form[PartnerType] = PartnerType.form()): Document =
    page(form, registrationWithOtherPartners.nominatedPartner.map(_.id), submitLink)(
      journeyReqForOthers,
      messages
    )

  private def createViewForOthers(form: Form[PartnerType] = PartnerType.form()): Document =
    page(form, None, submitLink)(journeyReqForOthers, messages)

  "Confirm Partnership Type View for Nominated" should {

    implicit val view = createViewNominated()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {

      displaySignOutLink(view)
    }

    "display 'Back' button" in {

      view.getElementById("back-link") must haveHref(routes.TaskListController.displayPage())
    }

    "display title for nominated partner" in {

      view.select("title").text() must include(messages("nominated.partner.type.title"))
    }

    "display radio inputs" in {

      radioInputMustBe(1, SOLE_TRADER)
      radioInputMustBe(2, UK_COMPANY)
      radioInputMustBe(3, REGISTERED_SOCIETY)
      radioInputMustBe(4, LIMITED_LIABILITY_PARTNERSHIP)
      radioInputMustBe(5, SCOTTISH_PARTNERSHIP)
      radioInputMustBe(6, SCOTTISH_LIMITED_PARTNERSHIP)
      radioInputMustBe(7, CHARITABLE_INCORPORATED_ORGANISATION)
      radioInputMustBe(8, OVERSEAS_COMPANY_UK_BRANCH)
      radioInputMustBe(9, OVERSEAS_COMPANY_NO_UK_BRANCH)
    }

    "display 'Save and continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  "Confirm Partnership Type View for Other" should {

    implicit val view1 = createViewForOthers()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view1) mustBe true
    }

    "display sign out link" in {

      displaySignOutLink(view1)
    }

    "display 'Back' button" in {

      view1.getElementById("back-link") must haveHref(routes.TaskListController.displayPage())
    }

    "display title for other partner" in {

      view1.select("title").text() must include(messages("other.partner.type.title"))
    }

    "display radio inputs" in {

      radioInputMustBe(1, SOLE_TRADER)
      radioInputMustBe(2, UK_COMPANY)
      radioInputMustBe(3, REGISTERED_SOCIETY)
      radioInputMustBe(4, LIMITED_LIABILITY_PARTNERSHIP)
      radioInputMustBe(5, SCOTTISH_PARTNERSHIP)
      radioInputMustBe(6, SCOTTISH_LIMITED_PARTNERSHIP)
      radioInputMustBe(7, CHARITABLE_INCORPORATED_ORGANISATION)
      radioInputMustBe(8, OVERSEAS_COMPANY_UK_BRANCH)
      radioInputMustBe(9, OVERSEAS_COMPANY_NO_UK_BRANCH)
    }

    "display 'Save and continue' button" in {

      view1 must containElementWithID("submit")
      view1.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(PartnerType.form(), None, submitLink)(journeyRequest, messages)
    page.render(PartnerType.form(), None, submitLink, journeyRequest, messages)
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
      labelKey.getOrElse(s"partner.type.$partnershipPartnerType")
    )
  }

}
