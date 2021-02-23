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
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
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

        incompleteRegistration.isCompanyDetailsComplete mustBe true
        incompleteRegistration.companyDetailsStatus mustBe TaskStatus.Completed

        incompleteRegistration.isLiabilityDetailsComplete mustBe true
        incompleteRegistration.liabilityDetailsStatus mustBe TaskStatus.Completed

        incompleteRegistration.isPrimaryContactDetailsComplete mustBe false
        incompleteRegistration.primaryContactDetailsStatus mustBe TaskStatus.InProgress

        incompleteRegistration.isCheckAndSubmitComplete mustBe false
        incompleteRegistration.checkAndSubmitStatus mustBe TaskStatus.CannotStartYet
      }

      "did not start any sections" in {
        val notStartedRegistration =
          aRegistration(withIncorpJourneyId(None), withNoLiabilityDetails())

        notStartedRegistration.isRegistrationComplete mustBe false
        notStartedRegistration.numberOfCompletedSections mustBe 0

        notStartedRegistration.isCompanyDetailsComplete mustBe false
        notStartedRegistration.companyDetailsStatus mustBe TaskStatus.NotStarted

        notStartedRegistration.isLiabilityDetailsComplete mustBe false
        notStartedRegistration.liabilityDetailsStatus mustBe TaskStatus.CannotStartYet

        notStartedRegistration.isPrimaryContactDetailsComplete mustBe false
        notStartedRegistration.primaryContactDetailsStatus mustBe TaskStatus.CannotStartYet

        notStartedRegistration.isCheckAndSubmitComplete mustBe false
        notStartedRegistration.checkAndSubmitStatus mustBe TaskStatus.CannotStartYet
      }
    }

    "be 'Completed' " when {
      "All sections are complete" in {
        val spyCompleteRegistration = Mockito.spy(aRegistration())
        when(spyCompleteRegistration.isPrimaryContactDetailsComplete).thenReturn(true)
        when(spyCompleteRegistration.primaryContactDetailsStatus).thenReturn(TaskStatus.Completed)
        when(spyCompleteRegistration.isCheckAndSubmitComplete).thenReturn(true)
        when(spyCompleteRegistration.checkAndSubmitStatus).thenReturn(TaskStatus.Completed)

        spyCompleteRegistration.isRegistrationComplete mustBe true
        spyCompleteRegistration.numberOfCompletedSections mustBe 3

        spyCompleteRegistration.isCompanyDetailsComplete mustBe true
        spyCompleteRegistration.companyDetailsStatus mustBe TaskStatus.Completed

        spyCompleteRegistration.isLiabilityDetailsComplete mustBe true
        spyCompleteRegistration.liabilityDetailsStatus mustBe TaskStatus.Completed

        spyCompleteRegistration.isPrimaryContactDetailsComplete mustBe true
        spyCompleteRegistration.primaryContactDetailsStatus mustBe TaskStatus.Completed

        spyCompleteRegistration.isCheckAndSubmitComplete mustBe true
        spyCompleteRegistration.checkAndSubmitStatus mustBe TaskStatus.Completed

      }
    }
  }
}
