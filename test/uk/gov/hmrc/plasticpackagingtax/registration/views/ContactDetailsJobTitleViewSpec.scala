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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.JobTitle
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.job_title_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class ContactDetailsJobTitleViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[job_title_page]

  private def createView(form: Form[JobTitle] = JobTitle.form()): Document =
    page(form)(journeyRequest, messages)

  val contactName = journeyRequest.registration.primaryContactDetails.name.get

  "Job Title View" should {

    "have proper messages for labels" in {
      messages must haveTranslationFor("primaryContactDetails.sectionHeader")
      messages must haveTranslationFor("primaryContactDetails.jobTitlePage.title")
      messages must haveTranslationFor("primaryContactDetails.jobTitle.empty.error")
      messages must haveTranslationFor("primaryContactDetails.jobTitle.tooLong.error")
      messages must haveTranslationFor("primaryContactDetails.jobTitle.nonAlphaChar.error")
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
        routes.ContactDetailsFullNameController.displayPage()
      )
    }

    "display title including contact name" in {

      view.select("title").text() must include(
        messages("primaryContactDetails.jobTitlePage.title", contactName)
      )
    }

    "display header" in {

      view.getElementsByClass("govuk-caption-l").text() must include(
        messages("primaryContactDetails.sectionHeader")
      )
    }

    "display job title question including contact name" in {

      view.getElementsByAttributeValueMatching("for", "value").text() must include(
        messages("primaryContactDetails.jobTitlePage.title", contactName)
      )
    }

    "display job title input box" in {

      view must containElementWithID("value")
    }

    "display 'Save And Continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and Continue"
    }

    "display 'Save and come back later' button" in {

      view.getElementById("save_and_come_back_later").text() mustBe "Save and come back later"
    }
  }

  "Job title View when filled" should {

    "display data in job title input box" in {

      val form = JobTitle
        .form()
        .fill(JobTitle("tester"))
      val view = createView(form)

      view.getElementById("value").attr("value") mustBe "tester"
    }
  }
  "display error" when {

    "job title is not entered" in {

      val form = JobTitle
        .form()
        .fillAndValidate(JobTitle(""))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("value", "Enter a job title")
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(JobTitle.form())(journeyRequest, messages)
    page.render(JobTitle.form(), journeyRequest, messages)
  }

}
