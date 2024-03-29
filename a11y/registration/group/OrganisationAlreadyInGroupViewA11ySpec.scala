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

package registration.group

import support.BaseViewSpec
import models.registration.group.GroupError
import models.registration.group.GroupErrorType.MEMBER_IN_GROUP
import views.html.group.organisation_already_in_group_page

class OrganisationAlreadyInGroupViewA11ySpec extends BaseViewSpec {

  private val page: organisation_already_in_group_page =
    inject[organisation_already_in_group_page]

  private val groupError = GroupError(MEMBER_IN_GROUP, "Member Name")

  private def createView(groupError: GroupError = groupError): String =
    page(groupError)(registrationJourneyRequest, messages).toString()

  "Organisation Already In Group Page" should {

    val view = createView()

    "pass accessibility checks without error" in {
      view must passAccessibilityChecks
    }
  }
}
