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

package registration.amendment.group

import support.BaseViewSpec
import config.AppConfig
import forms.organisation.OrgType.UK_COMPANY
import models.registration.{OrganisationDetails, Registration}
import views.html.amendment.group.manage_group_members_page

class ManageGroupMembersPageA11ySpec extends BaseViewSpec {

  val page: manage_group_members_page = inject[manage_group_members_page]
  val realAppConfig: AppConfig        = inject[AppConfig]

  val registration: Registration = Registration("someID",
                                                organisationDetails = OrganisationDetails(
                                                  organisationType = Some(UK_COMPANY),
                                                  incorporationDetails = Some(incorporationDetails)
                                                ),
                                                groupDetail = Some(
                                                  groupDetails.copy(members =
                                                    Seq(groupMember, groupMember)
                                                  )
                                                )
  )

  private def createView(): String =
    page(registration)(journeyRequest, messages).toString()

  val view = createView()

  "manage_group_members_page" must {

    "pass accessibility checks without error" in {
      view must passAccessibilityChecks
    }
  }
}
