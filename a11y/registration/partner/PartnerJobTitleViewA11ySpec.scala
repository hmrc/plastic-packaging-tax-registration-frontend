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

package registration.partner

import play.api.data.Form
import play.api.mvc.Call
import support.BaseViewSpec
import forms.contact.JobTitle
import views.html.partner.partner_job_title_page

class PartnerJobTitleViewA11ySpec extends BaseViewSpec {

  private val page = inject[partner_job_title_page]

  private val updateLink = Call("GET", "/update")
  
  private val contactName = "A Contact"

  private def createView(form: Form[JobTitle] = JobTitle.form()): String =
    page(form, contactName, updateLink)(registrationJourneyRequest, messages).toString()

  "Job title View" should {

    val view = createView()

    "pass accessibility checks without error" in {
      view must passAccessibilityChecks
    }

    "pass accessibility checks with error" in {
      val form = JobTitle
        .form()
        .fillAndValidate(JobTitle(""))
      val view = createView(form)

      view must passAccessibilityChecks
    }
  }
}
