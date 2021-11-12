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

package uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.GroupDetail
import uk.gov.hmrc.plasticpackagingtax.registration.views.model.TaskStatus

class GroupDetailSpec extends AnyWordSpec with Matchers {

  "Group detail " should {

    "be NOT_STARTED " when {
      "members is empty" in {
        val groupDetail = GroupDetail()
        groupDetail.status mustBe TaskStatus.NotStarted
      }
    }

    "be COMPLETED " when {
      "members are present" in {
        val groupDetail = GroupDetail(members =
          Seq(
            GroupMember(customerIdentification1 = "id1",
                        customerIdentification2 = Some("id2"),
                        organisationDetails =
                          Some(OrganisationDetails("UkCompany", "Company Name", Some("121212121"))),
                        addressDetails = AddressDetails("line1",
                                                        "line2",
                                                        Some("line3"),
                                                        Some("line4"),
                                                        Some("AB12CD"),
                                                        "UK"
                        )
            )
          )
        )
        groupDetail.status mustBe TaskStatus.Completed
      }
    }
  }
}
