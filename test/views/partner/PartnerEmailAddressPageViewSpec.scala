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

package views.partner

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import play.api.mvc.Call
import forms.contact.EmailAddress
import views.html.partner.partner_email_address_page

class PartnerEmailAddressPageViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[partner_email_address_page]

  private val updateLink = Call("PUT", "/update")

  private val contactName = "A Contact"

  private val nominated        = true
  private val notNominated     = false

  private def createViewNominated(form: Form[EmailAddress] = EmailAddress.form()): Document =
    page(form, updateLink, contactName, nominated)(journeyRequest, messages)

  private def createViewOther(form: Form[EmailAddress] = EmailAddress.form()): Document =
    page(form, updateLink, contactName, notNominated)(journeyRequest, messages)

  "Email address View" should {

    val viewNom   = createViewNominated()
    val viewOther = createViewOther()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(viewNom) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(viewNom)

    }

    "display title" in {

      viewNom.select("title").text() must include(
        messages("partnership.otherPartners.contactEmailAddressPage.title", contactName)
      )
    }

    "display email address input box" in {

      viewNom must containElementWithID("value")
    }

    "nominated" should {

      "display a caption" in {

        viewNom.getElementById("section-header").text() must include("Nominated partner details")

      }

      "display a hint for each name input" in {

        viewNom.getElementById("value-hint").text() must include(
          "We’ll only use this to send notifications about this registration, the account and returns."
        )

      }
    }

    "other" should {

      "display a caption" in {

        viewOther.getElementById("section-header").text() must include("Other partner details")

      }

      "not display a hint for each name input" in {

        viewNom.getElementById("value-hint").text() must include(
          "We’ll only use this to send notifications about this registration, the account and returns."
        )

      }
    }

    "display 'Save and continue' button" in {

      viewNom must containElementWithID("submit")
      viewNom.getElementById("submit").text() mustBe "Save and continue"
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
    page.f(EmailAddress.form(), updateLink, contactName, nominated)(journeyRequest, messages)
    page.render(EmailAddress.form(), updateLink, contactName, nominated, journeyRequest, messages)
  }

}
