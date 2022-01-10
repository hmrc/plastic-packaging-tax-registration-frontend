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

import builders.RegistrationBuilder
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import spec.PptTestData
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Date
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.RegType.{GROUP, SINGLE_ENTITY}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.{
  LiabilityExpectedWeight,
  LiabilityWeight
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType
import uk.gov.hmrc.plasticpackagingtax.registration.views.model.TaskStatus

class RegistrationSpec
    extends AnyWordSpec with Matchers with MockitoSugar with RegistrationBuilder with PptTestData {

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
        val registrationReviewedMetaData =
          aRegistration().metaData.copy(registrationReviewed = true)
        val reviewedRegistration =
          aRegistration(withMetaData(registrationReviewedMetaData))

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
        val registrationCompletedMetaData =
          aRegistration().metaData.copy(registrationReviewed = true, registrationCompleted = true)
        val completeRegistration = aRegistration(withMetaData(registrationCompletedMetaData))

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

  "Registration liability status" should {

    val completedLiabilityDetails =
      LiabilityDetails(startDate =
                         Some(Date(Some(1), Some(5), Some(2022))),
                       expectedWeight =
                         Some(LiabilityExpectedWeight(Some(true), Some(10000))),
                       isLiable = Some(true)
      )
    completedLiabilityDetails.isCompleted mustBe true

    "be complete for single organisation registration with completed liability details" in {
      Registration(id = "123",
                   liabilityDetails =
                     completedLiabilityDetails
      ).liabilityDetailsStatus mustBe TaskStatus.Completed
    }

    "be in progress for single organisation registration with incomplete liability details" in {
      Registration(id = "123",
                   liabilityDetails =
                     completedLiabilityDetails.copy(expectedWeight = None)
      ).liabilityDetailsStatus mustBe TaskStatus.InProgress
    }

    "be complete for group registration with under group control set to 'true'" in {
      Registration(id = "123",
                   liabilityDetails =
                     completedLiabilityDetails,
                   registrationType = Some(GROUP),
                   groupDetail = Some(GroupDetail(membersUnderGroupControl = Some(true)))
      ).liabilityDetailsStatus mustBe TaskStatus.Completed
    }

    "be in progress for group registration with under group control set to 'false'" in {
      Registration(id = "123",
                   liabilityDetails =
                     completedLiabilityDetails,
                   registrationType = Some(GROUP),
                   groupDetail = Some(GroupDetail(membersUnderGroupControl = Some(false)))
      ).liabilityDetailsStatus mustBe TaskStatus.InProgress
    }

    "be in progress for group registration with under group control un-answered" in {
      Registration(id = "123",
                   liabilityDetails =
                     completedLiabilityDetails,
                   registrationType = Some(GROUP),
                   groupDetail = Some(GroupDetail(membersUnderGroupControl = None))
      ).liabilityDetailsStatus mustBe TaskStatus.InProgress
    }
  }

  "Registration for single organisation number of completed sections" should {
    "have the correct value " when {

      "registration is complete" in {
        val registrationCompletedMetaData =
          aRegistration().metaData.copy(registrationReviewed = true, registrationCompleted = true)
        aRegistration(
          withMetaData(registrationCompletedMetaData)
        ).numberOfCompletedSections mustBe 4
      }
      "registration has not been completed" in {
        aRegistration().numberOfCompletedSections mustBe 3
      }
      "registration does not have complete contact details" in {
        aRegistration(
          withPrimaryContactDetails(PrimaryContactDetails())
        ).numberOfCompletedSections mustBe 2
      }
      "registration does not have complete contact or organisation details" in {
        aRegistration(withPrimaryContactDetails(PrimaryContactDetails()),
                      withOrganisationDetails(OrganisationDetails())
        ).numberOfCompletedSections mustBe 1
      }
      "registration not started" in {
        Registration("123").numberOfCompletedSections mustBe 0
      }
    }

  }

  "Registration for group organisation number of completed sections" should {
    "have the correct value " when {

      val groupDetails =
        GroupDetail(membersUnderGroupControl = Some(true), members = Seq(groupMember))

      "registration is complete" in {
        val registrationCompletedMetaData =
          aRegistration().metaData.copy(registrationReviewed = true, registrationCompleted = true)
        aRegistration(withRegistrationType(Some(GROUP)),
                      withMetaData(registrationCompletedMetaData),
                      withGroupDetail(Some(groupDetails))
        ).numberOfCompletedSections mustBe 5
      }
      "registration has not been completed" in {
        aRegistration(withRegistrationType(Some(GROUP)),
                      withGroupDetail(Some(groupDetails))
        ).numberOfCompletedSections mustBe 4
      }
      "registration does not have complete group members" in {
        aRegistration(withRegistrationType(Some(GROUP)),
                      withGroupDetail(Some(groupDetails.copy(members = Seq.empty)))
        ).numberOfCompletedSections mustBe 3
      }
      "registration does not have complete group members or contact details" in {
        aRegistration(withRegistrationType(Some(GROUP)),
                      withGroupDetail(Some(groupDetails.copy(members = Seq.empty))),
                      withPrimaryContactDetails(PrimaryContactDetails())
        ).numberOfCompletedSections mustBe 2
      }
      "registration does not have complete group members, contact details or nominated organisation details" in {
        aRegistration(withRegistrationType(Some(GROUP)),
                      withGroupDetail(Some(groupDetails.copy(members = Seq.empty))),
                      withPrimaryContactDetails(PrimaryContactDetails()),
                      withOrganisationDetails(OrganisationDetails())
        ).numberOfCompletedSections mustBe 1
      }
      "registration not started" in {
        Registration("123", registrationType = Some(GROUP)).numberOfCompletedSections mustBe 0
      }
    }

  }

  "Registration for partnership organisation" should {

    "identify partnerships from registration organisation type" in {
      val aPartnershipRegistration = aRegistration(
        withOrganisationDetails(OrganisationDetails(organisationType = Some(OrgType.PARTNERSHIP)))
      )
      aPartnershipRegistration.isPartnership mustBe true
    }

  }

}
