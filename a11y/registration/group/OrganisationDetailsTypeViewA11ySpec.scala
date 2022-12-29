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

package registration.group

import play.api.data.Form
import support.BaseViewSpec
import controllers.group.routes
import forms.organisation.{ActionEnum, OrganisationType}
import views.html.group.organisation_type



class OrganisationDetailsTypeViewA11ySpec extends BaseViewSpec {

  private val page = inject[organisation_type]

  private def createView(
    form: Form[OrganisationType] = OrganisationType.form(ActionEnum.Group),
    isFirstMember: Boolean = true
  ): String =
    page(form = form,
         isFirstMember = isFirstMember,
         memberId = Some(groupMember.id),
         routes.OrganisationDetailsTypeController.submitNewMember()
    )(journeyRequest, messages).toString()

  "Confirm Organisation Type View" should {

    implicit val view = createView()

    "pass accessibility checks without error" in {
      view must passAccessibilityChecks
    }

    "pass accessibility checks with error" in {
      val form = OrganisationType
        .form(ActionEnum.Group)
        .fillAndValidate(OrganisationType(""))
      val view = createView(form)

      view must passAccessibilityChecks
    }
  }
}
