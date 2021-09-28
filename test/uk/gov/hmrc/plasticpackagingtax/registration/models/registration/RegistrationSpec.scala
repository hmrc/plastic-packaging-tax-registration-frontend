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

package uk.gov.hmrc.plasticpackagingtax.registration.models.registration

import builders.RegistrationBuilder
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.plasticpackagingtax.registration.forms.LiabilityWeight
import uk.gov.hmrc.plasticpackagingtax.registration.views.model.TaskStatus

class RegistrationSpec
    extends AnyWordSpec with Matchers with MockitoSugar with RegistrationBuilder {

  "Registration status" should {
    "be 'Cannot Start Yet' " when {
      "Primary Contact details are incomplete" in {
        val incompleteRegistration =
          aRegistration(withPrimaryContactDetails(PrimaryContactDetails(jobTitle = Some("job"))))

        incompleteRegistration.isRegistrationComplete mustBe false
        incompleteRegistration.numberOfCompletedSections mustBe 2

        incompleteRegistration.isLiabilityDetailsComplete mustBe true
        incompleteRegistration.liabilityDetailsStatus mustBe TaskStatus.Completed

        incompleteRegistration.isCompanyDetailsComplete mustBe true
        incompleteRegistration.companyDetailsStatus mustBe TaskStatus.Completed

        incompleteRegistration.isPrimaryContactDetailsComplete mustBe false
        incompleteRegistration.primaryContactDetailsStatus mustBe TaskStatus.InProgress

        incompleteRegistration.isCheckAndSubmitReady mustBe false
        incompleteRegistration.checkAndSubmitStatus mustBe TaskStatus.CannotStartYet
      }

      "did not start any sections" in {
        val notStartedRegistration =
          aRegistration(withIncorpJourneyId(None), withNoLiabilityDetails())

        notStartedRegistration.isRegistrationComplete mustBe false
        notStartedRegistration.numberOfCompletedSections mustBe 0

        notStartedRegistration.isLiabilityDetailsComplete mustBe false
        notStartedRegistration.liabilityDetailsStatus mustBe TaskStatus.NotStarted

        notStartedRegistration.isCompanyDetailsComplete mustBe false
        notStartedRegistration.companyDetailsStatus mustBe TaskStatus.CannotStartYet

        notStartedRegistration.isPrimaryContactDetailsComplete mustBe false
        notStartedRegistration.primaryContactDetailsStatus mustBe TaskStatus.CannotStartYet

        notStartedRegistration.isCheckAndSubmitReady mustBe false
        notStartedRegistration.checkAndSubmitStatus mustBe TaskStatus.CannotStartYet
      }
    }

    "be 'In Progress' " when {
      "All sections are complete and the user is reviewing the registration" in {
        val reviewedRegistration =
          aRegistration(withMetaData(MetaData(registrationReviewed = true)))

        reviewedRegistration.isRegistrationComplete mustBe false
        reviewedRegistration.numberOfCompletedSections mustBe 3

        reviewedRegistration.isLiabilityDetailsComplete mustBe true
        reviewedRegistration.liabilityDetailsStatus mustBe TaskStatus.Completed

        reviewedRegistration.isCompanyDetailsComplete mustBe true
        reviewedRegistration.companyDetailsStatus mustBe TaskStatus.Completed

        reviewedRegistration.isPrimaryContactDetailsComplete mustBe true
        reviewedRegistration.primaryContactDetailsStatus mustBe TaskStatus.Completed

        reviewedRegistration.isCheckAndSubmitReady mustBe true
        reviewedRegistration.checkAndSubmitStatus mustBe TaskStatus.InProgress

      }
    }

    "be 'Completed' " when {
      "All sections are complete" in {
        val completeRegistration = aRegistration(
          withMetaData(MetaData(registrationReviewed = true, registrationCompleted = true))
        )

        completeRegistration.isRegistrationComplete mustBe true
        completeRegistration.numberOfCompletedSections mustBe 4

        completeRegistration.isLiabilityDetailsComplete mustBe true
        completeRegistration.liabilityDetailsStatus mustBe TaskStatus.Completed

        completeRegistration.isCompanyDetailsComplete mustBe true
        completeRegistration.companyDetailsStatus mustBe TaskStatus.Completed

        completeRegistration.isPrimaryContactDetailsComplete mustBe true
        completeRegistration.primaryContactDetailsStatus mustBe TaskStatus.Completed

        completeRegistration.isCheckAndSubmitReady mustBe true
        completeRegistration.checkAndSubmitStatus mustBe TaskStatus.Completed

      }
    }
  }

  "Registration" should {
    "be not started" when {
      "freshly created" in {
        Registration("123").isStarted mustBe false
      }
    }
    "be started" when {
      "liability weight captured" in {
        Registration(id = "123",
                     liabilityDetails =
                       LiabilityDetails(weight = Some(LiabilityWeight(Some(12000))))
        ).isStarted mustBe true
      }
    }
  }
}
