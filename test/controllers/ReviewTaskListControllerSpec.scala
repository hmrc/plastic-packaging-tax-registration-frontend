/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers

import audit.Auditor
import base.MetricsMocks
import base.unit.ControllerSpec
import forms.contact.Address
import forms.liability.LiabilityWeight
import forms.organisation.OrgType.{PARTNERSHIP, SOLE_TRADER, UK_COMPANY}
import forms.{Date, OldDate}
import models.addresslookup.CountryCode.GB
import models.genericregistration.IncorporationDetails
import models.nrs.NrsDetails
import models.registration._
import models.registration.group.GroupMember
import models.subscriptions.SubscriptionStatus.NOT_SUBSCRIBED
import models.subscriptions.{EisError, SubscriptionCreateOrUpdateResponseFailure}
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.`given`
import org.mockito.MockitoSugar.{reset, verify, when}
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.JsObject
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.RegistrationGroupFilterService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.{duplicate_subscription_page, review_registration_page}

import java.time.LocalDate
import java.util.UUID

class ReviewTaskListControllerSpec extends ControllerSpec with TableDrivenPropertyChecks with MetricsMocks {
  private val mockReviewRegistrationPage    = mock[review_registration_page]
  private val mockDuplicateSubscriptionPage = mock[duplicate_subscription_page]
  private val mcc                           = stubMessagesControllerComponents()
  private val mockRegistrationFilterService = mock[RegistrationGroupFilterService]
  val mockAuditor: Auditor                  = mock[Auditor]

  private val controller =
    new ReviewRegistrationController(
      appConfig = config,
      journeyAction = spyJourneyAction,
      mcc = mcc,
      registrationConnector = mockRegistrationConnector,
      subscriptionsConnector = mockSubscriptionsConnector,
      auditor = mockAuditor,
      reviewRegistrationPage = mockReviewRegistrationPage,
      metrics = metricsMock,
      registrationFilterService = mockRegistrationFilterService
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    given(mockReviewRegistrationPage.apply(any())(any(), any())).willReturn(HtmlFormat.empty)
    given(mockDuplicateSubscriptionPage.apply()(any(), any())).willReturn(HtmlFormat.empty)
    when(mockRegistrationFilterService.removeGroupDetails(any[Registration]))
      .thenAnswer((reg: Registration) => reg)
  }

  override protected def afterEach(): Unit = {
    reset(mockReviewRegistrationPage, mockRegistrationFilterService)
    super.afterEach()
  }

  private val completedLiabilityDetails = LiabilityDetails(
    exceededThresholdWeight = Some(true),
    dateExceededThresholdWeight = Some(Date(LocalDate.ofEpochDay(0))),
    expectToExceedThresholdWeight = Some(true),
    dateRealisedExpectedToExceedThresholdWeight = Some(Date(LocalDate.ofEpochDay(0))),
    expectedWeightNext12m = Some(LiabilityWeight(Some(1))),
    startDate = Some(OldDate.of(2022, 4, 1)),
    isLiable = Some(true),
    newLiabilityFinished = Some(NewLiability),
    newLiabilityStarted = Some(NewLiability)
  )

  "Review registration controller" should {

    "show the review registration page to an authorised user" when {

      ".displayPage is invoked with a uk company" in {
        val registration: Registration = aRegistration(
          withOrganisationDetails(
            OrganisationDetails(
              organisationType = Some(UK_COMPANY),
              incorporationDetails = Some(
                IncorporationDetails(
                  companyNumber = "123456",
                  companyName = "NewPlastics",
                  ctutr = Some("1890894"),
                  companyAddress = testCompanyAddress,
                  registration = Some(registrationDetails)
                )
              ),
              subscriptionStatus = Some(NOT_SUBSCRIBED)
            )
          )
        )
        spyJourneyAction.setReg(registration)
        mockRegistrationUpdate()

        val result = controller.displayPage()(FakeRequest())

        status(result) mustBe OK
        verify(mockReviewRegistrationPage).apply(ArgumentMatchers.eq(registration))(any(), any())
      }

      ".displayPage is invoked with a sole trader" in {
        val registration = aRegistration(
          withOrganisationDetails(
            OrganisationDetails(
              organisationType = Some(SOLE_TRADER),
              soleTraderDetails = Some(soleTraderDetails),
              subscriptionStatus = Some(NOT_SUBSCRIBED)
            )
          )
        )
        spyJourneyAction.setReg(registration)
        mockRegistrationUpdate()

        val result = controller.displayPage()(FakeRequest())

        status(result) mustBe OK
      }

      ".displayPage is invoked with a partnership" in {
        val registration = aRegistration(
          withOrganisationDetails(
            OrganisationDetails(
              organisationType = Some(PARTNERSHIP),
              partnershipDetails = Some(
                generalPartnershipDetails.copy(partners = Seq(aLimitedCompanyPartner, aSoleTraderPartner))
              ),
              subscriptionStatus = Some(NOT_SUBSCRIBED)
            )
          )
        )
        spyJourneyAction.setReg(registration)
        mockRegistrationUpdate()
        mockGetPartnershipBusinessDetails(partnershipBusinessDetails)

        val result = controller.displayPage()(FakeRequest())

        status(result) mustBe OK
      }
    }

    "redirect to the task-list for an authorised user" when {

      "the registration contains a partial group member" in {

        val registrationWitPartialGroupMembers: Registration =
          createRegistrationWithGroupMember(groupMember, groupMember.copy(customerIdentification1 = ""))

        val expectedReg: Registration = registrationWitPartialGroupMembers.copy(groupDetail =
          Some(GroupDetail(membersUnderGroupControl = Some(true), members = List(groupMember)))
        )

        spyJourneyAction.setReg(registrationWitPartialGroupMembers)
        when(mockRegistrationFilterService.removePartialGroupMembers(any())).thenReturn(expectedReg)
        mockRegistrationUpdate()

        val result = controller.displayPage()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.TaskListController.displayPage().url)
      }

      "all group members have been removed" in {
        val registrationRequest: Registration =
          createRegistrationWithGroupMember(groupMember.copy(customerIdentification1 = ""))

        val sanitisedReg =
          registrationRequest.copy(groupDetail = registrationRequest.groupDetail.map(_.copy(members = Seq.empty)))

        when(mockRegistrationFilterService.removePartialGroupMembers(any()))
          .thenReturn(sanitisedReg)
        spyJourneyAction.setReg(registrationRequest)
        mockRegistrationUpdate()

        val result = controller.displayPage()(postRequest(JsObject.empty))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.TaskListController.displayPage().url)
      }

      "the registration is not in ready state" in {
        val unreadyRegistration =
          aCompletedGeneralPartnershipRegistration
            .copy(incorpJourneyId = Some(UUID.randomUUID().toString))
            .copy(organisationDetails =
              aCompletedGeneralPartnershipRegistration.organisationDetails.copy(organisationType = None)
            )
        spyJourneyAction.setReg(unreadyRegistration)

        val result = controller.displayPage()(postRequest(JsObject.empty))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.TaskListController.displayPage().url)
      }

      "the user has not answered the new liability questions" in {
        val oldLiability = completedLiabilityDetails.copy(newLiabilityFinished = None, newLiabilityStarted = None)
        spyJourneyAction.setReg(aCompletedUkCompanyRegistration.copy(liabilityDetails = oldLiability))
        mockRegistrationUpdate()

        val result = controller.displayPage()(postRequest(JsObject.empty))
        await(result) mustBe Redirect(routes.TaskListController.displayPage())
      }

    }

    "redirect to the confirmation page" when {

      "the review registration details form is submitted" in {

        val registrations = Table(
          "Registration",
          aCompletedUkCompanyRegistration,
          aCompletedSoleTraderRegistration,
          aCompletedGeneralPartnershipRegistration,
          aCompletedScottishPartnershipRegistration
        )

        var submissionCount = 0

        forAll(registrations) { registration =>
          val completedRegistration =
            registration.copy(incorpJourneyId = Some(UUID.randomUUID().toString))
          spyJourneyAction.setReg(completedRegistration)
          mockSubscriptionSubmit(subscriptionCreateOrUpdate)

          val result = controller.submit()(postRequest(JsObject.empty))
          submissionCount += 1

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.ConfirmationController.displayPage().url)
          verify(mockSubscriptionsConnector).submitSubscription(
            ArgumentMatchers.eq(safeNumber),
            ArgumentMatchers.eq(completedRegistration.copy(userHeaders = Some(testUserHeaders)))
          )(any[HeaderCarrier])

          metricsMock.defaultRegistry.counter(
            "ppt.registration.success.submission.counter"
          ).getCount mustBe submissionCount
        }
      }

      "the user has not answered the new liability questions" in {
        val oldLiability = completedLiabilityDetails.copy(newLiabilityFinished = None, newLiabilityStarted = None)
        spyJourneyAction.setReg(aCompletedUkCompanyRegistration.copy(liabilityDetails = oldLiability))
        mockRegistrationUpdate()

        val result = controller.submit()(postRequest(JsObject.empty))
        await(result) mustBe Redirect(routes.TaskListController.displayPage())
      }

    }

    "throw exceptions" when {
      "submit registration with missing business partner id" in {
        val registrationWithMissingBusinessPartnerId =
          aCompletedUkCompanyRegistration
            .copy(incorpJourneyId = Some(UUID.randomUUID().toString))
            .copy(organisationDetails =
              aCompletedUkCompanyRegistration.organisationDetails.copy(incorporationDetails =
                Some(
                  aCompletedUkCompanyRegistration.organisationDetails.incorporationDetails.get.copy(registration =
                    aCompletedUkCompanyRegistration.organisationDetails.incorporationDetails.get.registration.map {
                      reg => reg.copy(registeredBusinessPartnerId = None)
                    }
                  )
                )
              )
            )
        spyJourneyAction.setReg(registrationWithMissingBusinessPartnerId)
        mockSubscriptionSubmit(subscriptionCreateOrUpdate)

        intercept[IllegalStateException](status(controller.submit()(postRequest(JsObject.empty))))
      }
    }

    "send audit event" when {
      "submission of audit event" in {

        val completedRegistration = aCompletedUkCompanyRegistration
        spyJourneyAction.setReg(completedRegistration)
        mockSubscriptionSubmit(subscriptionCreateOrUpdate)

        await(controller.submit()(postRequest(JsObject.empty)))

        val completedRegistrationWithNrsDetail = completedRegistration.copy(metaData =
          aCompletedUkCompanyRegistration.metaData.copy(nrsDetails =
            Some(NrsDetails(nrSubsmissionId = subscriptionCreateOrUpdate.nrsSubmissionId, failureReason = None))
          )
        )

        verify(mockAuditor, Mockito.atLeast(1)).registrationSubmitted(
          ArgumentMatchers.eq(completedRegistrationWithNrsDetail),
          ArgumentMatchers.eq(Some("XXPPTP123456789")),
          any()
        )(
          any(),
          any()
        )
      }
    }

    "redirect to the error page" when {

      "user submits form and the submission fails with an exception" in {
        spyJourneyAction.setReg(aCompletedUkCompanyRegistration)
        mockSubscriptionSubmitFailure(new IllegalStateException("BANG!"))
        val result =
          controller.submit()(postRequest(JsObject.empty))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.NotableErrorController.subscriptionFailure().url)

        metricsMock.defaultRegistry.counter("ppt.registration.failed.submission.counter").getCount mustBe 1
      }

      "user submits form and the submission fails with an error response" in {
        val registration = aCompletedUkCompanyRegistration
        spyJourneyAction.setReg(registration)
        mockSubscriptionSubmitFailure(
          SubscriptionCreateOrUpdateResponseFailure(
            List(EisError("INVALID_SAFEID", "The remote endpoint has indicated that the SAFEID provided is invalid."))
          )
        )

        val result = controller.submit()(postRequest(JsObject.empty))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.NotableErrorController.subscriptionFailure().url)
      }

      "user submits form and the enrolment initiation fails" in {
        val registration = aCompletedUkCompanyRegistration
        spyJourneyAction.setReg(registration)
        mockSubscriptionSubmit(subscriptionCreateWithEnrolmentFailure)

        val result = controller.submit()(postRequest(JsObject.empty))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.NotableErrorController.enrolmentFailure().url)
      }

      "duplicate subscription detected at the back end" in {
        val registration = aCompletedUkCompanyRegistration
        spyJourneyAction.setReg(registration)
        mockSubscriptionSubmitFailure(
          SubscriptionCreateOrUpdateResponseFailure(
            List(
              EisError(
                "ACTIVE_SUBSCRIPTION_EXISTS",
                "The remote endpoint has indicated that Business Partner already has active subscription for this regime."
              )
            )
          )
        )

        val result = controller.submit()(postRequest(JsObject.empty))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.NotableErrorController.duplicateRegistration().url)
      }
    }
  }

  private def aCompletedUkCompanyRegistration =
    aRegistration(
      withOrganisationDetails(registeredUkCompanyOrgDetails()),
      withLiabilityDetails(completedLiabilityDetails),
      withPrimaryContactDetails(primaryContactDetails),
      withMetaData(MetaData(registrationCompleted = true))
    )

  private def aCompletedSoleTraderRegistration =
    aRegistration(
      withOrganisationDetails(registeredSoleTraderOrgDetails()),
      withLiabilityDetails(completedLiabilityDetails),
      withPrimaryContactDetails(primaryContactDetails),
      withMetaData(MetaData(registrationCompleted = true))
    )

  private def aCompletedGeneralPartnershipRegistration =
    aRegistration(
      withOrganisationDetails(registeredGeneralPartnershipOrgDetails()),
      withLiabilityDetails(completedLiabilityDetails),
      withPrimaryContactDetails(primaryContactDetails),
      withMetaData(MetaData(registrationCompleted = true))
    )

  private def aCompletedScottishPartnershipRegistration =
    aRegistration(
      withOrganisationDetails(registeredScottishPartnershipOrgDetails()),
      withLiabilityDetails(completedLiabilityDetails),
      withPrimaryContactDetails(primaryContactDetails),
      withMetaData(MetaData(registrationCompleted = true))
    )

  private val primaryContactDetails = PrimaryContactDetails(
    name = Some("Jack Gatsby"),
    jobTitle = Some("Developer"),
    phoneNumber = Some("0203 4567 890"),
    email = Some("test@test.com"),
    address = Some(
      Address(
        addressLine1 = "2 Scala Street",
        addressLine2 = Some("Soho"),
        addressLine3 = None,
        townOrCity = "London",
        maybePostcode = Some("W1T 2HN"),
        countryCode = GB
      )
    )
  )

  def createRegistrationWithGroupMember(members: GroupMember*): Registration =
    aRegistration(
      withOrganisationDetails(
        OrganisationDetails(
          organisationType = Some(UK_COMPANY),
          incorporationDetails = Some(
            IncorporationDetails(
              companyNumber = "123456",
              companyName = "NewPlastics",
              ctutr = Some("1890894"),
              companyAddress = testCompanyAddress,
              registration = Some(registrationDetails)
            )
          ),
          subscriptionStatus = Some(NOT_SUBSCRIBED)
        )
      ),
      withGroupDetail(Some(GroupDetail(membersUnderGroupControl = Some(true), members = members)))
    )

}
