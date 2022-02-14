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

package uk.gov.hmrc.plasticpackagingtax.registration.views.amendment.partner

import base.unit.UnitViewSpec
import org.scalatest.matchers.must.Matchers
import play.twirl.api.Html
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.amendment.partner.manage_partners_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class ManagePartnersPageSpec extends UnitViewSpec with Matchers {

  private val page: manage_partners_page = inject[manage_partners_page]
  private val realAppConfig              = inject[AppConfig]

  private val partnershipRegistration = aRegistration(
    withPartnershipDetails(Some(generalPartnershipDetailsWithPartners))
  )

  private def createView(registration: Registration): Html =
    page(registration)(journeyRequestWithEnrolledUser, messages)

  val view: Html = createView(partnershipRegistration)

  "Amend Registration Page" when {

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    "display title" in {
      view.select("title").text() must include(messages("amend.partner.manage.title"))
    }

    "display back link to ppt home" in {
      view.getElementById("back-link") must haveHref(realAppConfig.pptAccountUrl)
    }

    "display page heading" in {
      view.select("h1").text() must include(messages("amend.partner.manage.title"))
    }

    "display nominated partner" in {
      val dataList = view.select("dl")
      dataList.select("dt").get(0).text() must include(messages("amend.partner.manage.nominated"))
      dataList.select("dd").get(0).text() must include(
        partnershipRegistration.nominatedPartner.map(_.name).get
      )
    }

    "display others partners" in {
      val dataList = view.select("dl")
      dataList.select("dt").get(1).text() must include(messages("amend.partner.manage.partners"))
      partnershipRegistration.otherPartners.map(_.name).foreach { otherPartnerName =>
        dataList.select("dd").get(1).text() must include(otherPartnerName)
      }
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(partnershipRegistration)(journeyRequest, messages)
    page.render(partnershipRegistration, journeyRequest, messages)
  }

}
