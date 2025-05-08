/*
 * Copyright 2025 HM Revenue & Customs
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

package views.contact

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import play.api.mvc.Call
import forms.contact.JobTitle
import views.html.contact.job_title_page

class ContactDetailsJobTitleViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[job_title_page]

  private val updateLink = Call("PUT", "/update")

  private def createView(form: Form[JobTitle] = JobTitle.form(), isGroup: Boolean = false): Document =
    page(form, updateLink, isGroup)(registrationJourneyRequest, messages)

  val contactName = registrationJourneyRequest.registration.primaryContactDetails.name.get

  "Job Title View" should {

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(view)

    }

    "display title including contact name" in {

      view.select("title").text() must include(messages("primaryContactDetails.jobTitlePage.title", contactName))
    }

    "display section header" when {
      "Single organisation" in {
        view.getElementsByClass("govuk-caption-l").text() must include(messages("primaryContactDetails.sectionHeader"))
      }

      "Group organisation" in {
        val view = createView(isGroup = true)
        view.getElementsByClass("govuk-caption-l").text() must include("Representative member details")
        view.getElementsByClass("govuk-caption-l").text() must include(
          messages("primaryContactDetails.group.sectionHeader")
        )

      }
    }

    "display job title question including contact name" in {

      view.getElementsByAttributeValueMatching("for", "value").text() must include(
        messages("primaryContactDetails.jobTitlePage.title", contactName)
      )
    }

    "display job title input box" in {

      view must containElementWithID("value")
    }

    "display 'Save and continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
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
    page.f(JobTitle.form(), updateLink, false)(registrationJourneyRequest, messages)
    page.render(JobTitle.form(), updateLink, false, registrationJourneyRequest, messages)
  }

}
