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

package uk.gov.hmrc.plasticpackagingtax.registration.views

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.EmailAddress
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.email_address_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class ContactDetailsEmailAddressViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[email_address_page]

  private def createView(form: Form[EmailAddress] = EmailAddress.form()): Document =
    page(form)(request, messages)

  "Email Address View" should {

    "have proper messages for labels" in {
      messages must haveTranslationFor("primaryContactDetails.sectionHeader")
      messages must haveTranslationFor("primaryContactDetails.emailAddress.title")
      messages must haveTranslationFor("primaryContactDetails.emailAddress.empty.error")
      messages must haveTranslationFor("primaryContactDetails.emailAddress.format.error")
      messages must haveTranslationFor("primaryContactDetails.emailAddress.privacyNotice")
      messages must haveTranslationFor("primaryContactDetails.emailAddress.privacyNotice.link")
    }

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(view)

    }

    "display 'Back' button" in {

      view.getElementById("back-link") must haveHref(
        routes.ContactDetailsJobTitleController.displayPage()
      )
    }

    "display title" in {

      view.select("title").text() must include(messages("primaryContactDetails.emailAddress.title"))
    }

    "display header" in {

      view.getElementsByClass("govuk-caption-xl").text() must include(
        messages("primaryContactDetails.sectionHeader")
      )
    }

    "display hint" in {

      view.getElementById("value-hint").text() must include(
        messages("primaryContactDetails.emailAddress.hint")
      )
    }

    "display email address question" in {

      view.getElementsByAttributeValueMatching("for", "value").text() must include(
        messages("primaryContactDetails.emailAddress.title")
      )
    }

    "display email address input box" in {

      view must containElementWithID("value")
    }

    "display 'Privacy' Notice" in {

      view.getElementsByClass("govuk-body").get(0).text() must include(
        messages("primaryContactDetails.emailAddress.privacyNotice",
                 messages("primaryContactDetails.emailAddress.privacyNotice.link")
        )
      )
    }

    "display 'Save And Continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and Continue"
    }

    "display 'Save and come back later' button" in {

      view.getElementById("save_and_come_back_later").text() mustBe "Save and come back later"
    }
  }

  "Email address view when filled" should {

    "display data in email address input box" in {

      val form = EmailAddress
        .form()
        .fill(EmailAddress("test@test.com"))
      val view = createView(form)

      view.getElementById("value").attr("value") mustBe "test@test.com"
    }
  }
  "display error" when {

    "email address has not been entered" in {

      val form = EmailAddress
        .form()
        .fillAndValidate(EmailAddress(""))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("value", "Enter an email address")
    }

    "email address is not valid" in {

      val form = EmailAddress
        .form()
        .fillAndValidate(EmailAddress("test@"))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("value", "Enter an email address in the correct format")
    }
  }
}
