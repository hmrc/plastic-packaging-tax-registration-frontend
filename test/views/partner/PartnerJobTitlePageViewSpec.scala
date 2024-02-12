/*
 * Copyright 2024 HM Revenue & Customs
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
import forms.contact.JobTitle
import views.html.partner.partner_job_title_page

class PartnerJobTitlePageViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[partner_job_title_page]
  private val updateLink = Call("PUT", "/update")

  private val contactName = "A Contact"

  private def createView(form: Form[JobTitle] = JobTitle.form()): Document =
    page(form, contactName, updateLink)(registrationJourneyRequest, messages)

  "Job title View" should {

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(view)

    }

    "display title" in {

      view.select("title").text() must include(
        messages("partnership.otherPartners.jobTitlePage.title", contactName)
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

  "display error" when {

    "job title is not entered" in {

      val form = JobTitle
        .form()
        .fillAndValidate(JobTitle(""))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary
    }
  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    page.f(JobTitle.form(), contactName, updateLink)(registrationJourneyRequest, messages)
    page.render(JobTitle.form(), contactName, updateLink, registrationJourneyRequest, messages)
  }

}
