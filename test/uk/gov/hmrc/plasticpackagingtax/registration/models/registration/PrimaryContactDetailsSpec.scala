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

package uk.gov.hmrc.plasticpackagingtax.registration.models.registration

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.views.models.TaskStatus

class PrimaryContactDetailsSpec extends AnyWordSpec with Matchers {

  val emailVerified: String => Boolean    = _ => true
  val emailNotVerified: String => Boolean = _ => false

  "Primary Contact Details TaskStatus" should {

    "be NOT_STARTED " when {
      "primary contact details is empty" in {
        val contactDetails = PrimaryContactDetails()
        contactDetails.status(emailVerified) mustBe TaskStatus.NotStarted
      }
    }

    "be IN_PROGRESS " when {
      "primary contact details when only 'FullName' is complete" in {
        val contactDetails =
          PrimaryContactDetails(name = Some("firstName lastName"))
        contactDetails.status(emailVerified) mustBe TaskStatus.InProgress
      }

      "primary contact details when only 'FullName' and 'JobTitle' are complete" in {
        val contactDetails = PrimaryContactDetails(name =
                                                     Some("firstName lastName"),
                                                   jobTitle = Some("Dev")
        )
        contactDetails.status(emailVerified) mustBe TaskStatus.InProgress
      }

      "primary contact details when only 'FullName', 'JobTitle' and 'Email' are complete" in {
        val contactDetails = PrimaryContactDetails(name =
                                                     Some("firstName lastName"),
                                                   jobTitle = Some("Dev"),
                                                   email = Some("test@test.com")
        )
        contactDetails.status(emailVerified) mustBe TaskStatus.InProgress
      }

      "primary contact details when only 'FullName', 'JobTitle', 'Email' and 'phoneNumber' are complete" in {
        val contactDetails = PrimaryContactDetails(name =
                                                     Some("firstName lastName"),
                                                   jobTitle = Some("Dev"),
                                                   email = Some("test@test.com"),
                                                   phoneNumber = Some("0203 12345 678")
        )
        contactDetails.status(emailVerified) mustBe TaskStatus.InProgress
      }

      "primary contact details are all filled in but email not verified" in {
        val contactDetails =
          PrimaryContactDetails(name = Some("FirstName LastName"),
                                jobTitle = Some("Developer"),
                                email = Some("test@test.com"),
                                phoneNumber = Some("07712345678"),
                                address = Some(
                                  Address(addressLine1 = "first line",
                                          addressLine2 = Some("second line"),
                                          townOrCity = "Leeds",
                                          postCode = Some("LS1 8TY")
                                  )
                                )
          )
        contactDetails.status(emailNotVerified) mustBe TaskStatus.InProgress
      }
    }

    "be COMPLETED " when {
      "primary contact details are all filled in and email verified" in {
        val contactDetails =
          PrimaryContactDetails(name = Some("FirstName LastName"),
                                jobTitle = Some("Developer"),
                                email = Some("test@test.com"),
                                phoneNumber = Some("07712345678"),
                                address = Some(
                                  Address(addressLine1 = "first line",
                                          addressLine2 = Some("second line"),
                                          townOrCity = "Leeds",
                                          postCode = Some("LS1 8TY")
                                  )
                                )
          )
        contactDetails.status(emailVerified) mustBe TaskStatus.Completed
      }
    }
  }
}
