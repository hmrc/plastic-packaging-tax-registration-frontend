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
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partner.partner_email_address_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class PartnerEmailAddressPageViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[partner_email_address_page]

  private val backLink   = Call("GET", "/back-link")
  private val updateLink = Call("PUT", "/update")

  private val contactName = "A Contact"

  private val nominated        = true
  private val notNominated     = false

  private def createViewNominated(form: Form[EmailAddress] = EmailAddress.form()): Document =
    page(form, backLink, updateLink, contactName, nominated)(journeyRequest, messages)

  private def createViewOther(form: Form[EmailAddress] = EmailAddress.form()): Document =
    page(form, backLink, updateLink, contactName, notNominated)(journeyRequest, messages)

  "Email address View" should {

    val view = createViewNominated()

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

      view.select("title").text() must include(
        messages("partnership.otherPartners.contactEmailAddressPage.title", contactName)
      )
    }

    "display email address input box" in {

      view must containElementWithID("value")
    }

    "display 'Save and continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  "display error" when {

    "email address is not entered" in {

      val form = EmailAddress
        .form()
        .fillAndValidate(EmailAddress(""))
      val view = createViewNominated(form)

      view must haveGovukGlobalErrorSummary
    }
  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    page.f(EmailAddress.form(), backLink, updateLink, contactName, nominated)(journeyRequest, messages)
    page.render(EmailAddress.form(), backLink, updateLink, contactName, nominated, journeyRequest, messages)
  }

}
