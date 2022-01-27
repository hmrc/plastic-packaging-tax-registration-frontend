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

package uk.gov.hmrc.plasticpackagingtax.registration.views.partner

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import play.api.mvc.Call
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.EmailAddress
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnershipName
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partner.{
  partner_email_address_page,
  partner_name_page
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class PartnerNamePageViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[partner_name_page]

  private val backLink   = Call("GET", "/back-link")
  private val updateLink = Call("PUT", "/update")

  private def createView(form: Form[PartnershipName] = PartnershipName.form()): Document =
    page(form, backLink, updateLink)(journeyRequest, messages)

  "Email address View" should {

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(view)

    }

    "display 'Back' button" in {

      view.getElementById("back-link") must haveHref(backLink.url)
    }

    "display title" in {

      view.select("title").text() must include(messages("partner.name.title"))
    }

    "display name input box" in {
      view must containElementWithID("value")
    }

    "display 'Save and continue' button" in {
      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  "display error" when {
    "name is not entered" in {

      val form = PartnershipName
        .form()
        .fillAndValidate(PartnershipName(""))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary
    }
  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    page.f(PartnershipName.form(), backLink, updateLink)(journeyRequest, messages)
    page.render(PartnershipName.form(), backLink, updateLink, journeyRequest, messages)
  }

}
