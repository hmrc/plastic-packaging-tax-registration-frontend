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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers

import base.unit.ControllerSpec
import org.mockito.AdditionalAnswers.returnsFirstArg
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.`given`
import org.mockito.Mockito.{reset, verify, when}
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.JsObject
import play.api.test.Helpers.{await, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType.{PARTNERSHIP, SOLE_TRADER, UK_COMPANY}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.IncorporationDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.nrs.NrsDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration._
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.GroupMember
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.SubscriptionStatus.NOT_SUBSCRIBED
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.{EisError, SubscriptionCreateOrUpdateResponseFailure}
import uk.gov.hmrc.plasticpackagingtax.registration.services.RegistrationGroupFilterService
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.{duplicate_subscription_page, review_registration_page}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import java.util.UUID

class ReviewTaskListControllerSpec extends ControllerSpec with TableDrivenPropertyChecks {
  private val mockReviewRegistrationPage    = mock[review_registration_page]
  private val mockDuplicateSubscriptionPage = mock[duplicate_subscription_page]
  private val mcc                           = stubMessagesControllerComponents()
  private val mockRegistrationFilterService = mock[RegistrationGroupFilterService]

  private val controller =
    new ReviewRegistrationController(authenticate = mockAuthAction,
                                     mockJourneyAction,
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
    authorizedUser()
    given(mockReviewRegistrationPage.apply(any())(any(), any())).willReturn(HtmlFormat.empty)
    given(mockDuplicateSubscriptionPage.apply()(any(), any())).willReturn(HtmlFormat.empty)
    when(mockRegistrationFilterService.removeGroupDetails(any())).then(returnsFirstArg())
  }

  override protected def afterEach(): Unit = {
    reset(mockReviewRegistrationPage, mockRegistrationFilterService)
    super.afterEach()
  }

  "Review registration controller" should {

    "return 200" when {

      "user is authorised and display page method is invoked with uk company" in {
        val registration: Registration = aRegistration(
          withOrganisationDetails(
            OrganisationDetails(organisationType = Some(UK_COMPANY),
                                incorporationDetails =
                                  Some(
                                    IncorporationDetails(companyNumber = "123456",
                                                         companyName = "NewPlastics",
                                                         ctutr = "1890894",
                                                         companyAddress = testCompanyAddress,
                                                         registration =
                                                           Some(registrationDetails)
                                    )
                                  ),
                                subscriptionStatus = Some(NOT_SUBSCRIBED)
            )
          )
        )
        mockRegistrationFind(registration)
        mockRegistrationUpdate()

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
        verify(mockReviewRegistrationPage).apply(registration = ArgumentMatchers.eq(registration))(
          any(),
          any()
        )
      }

      "user is authorised and registration contains a partial group member" in {

        val registrationWitPartialGroupMembers: Registration =
          createRegistrationWithGroupMember(groupMember,
                                            groupMember.copy(customerIdentification1 = "")
          )

        val expectedReg: Registration = registrationWitPartialGroupMembers.copy(groupDetail =
          Some(GroupDetail(membersUnderGroupControl = Some(true), members = List(groupMember)))
        )

        mockRegistrationFind(registrationWitPartialGroupMembers)
        when(mockRegistrationFilterService.removePartialGroupMembers(any())).thenReturn(expectedReg)
        mockRegistrationUpdate()

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
        verify(mockReviewRegistrationPage).apply(registration = ArgumentMatchers.eq(expectedReg))(
          any(),
          any()
        )

        verify(mockRegistrationConnector).update(
          ArgumentMatchers.eq(
            expectedReg.copy(metaData =
              registrationWitPartialGroupMembers.metaData.copy(registrationReviewed = true)
            )
          )
        )(any())
      }

      "user is authorised and display page method is invoked with sole trader" in {
        val registration = aRegistration(
          withOrganisationDetails(
            OrganisationDetails(organisationType = Some(SOLE_TRADER),
                                soleTraderDetails = Some(soleTraderDetails),
                                subscriptionStatus = Some(NOT_SUBSCRIBED)
            )
          )
        )
        mockRegistrationFind(registration)
        mockRegistrationUpdate()

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

      "user is authorised and display page method is invoked with partnership" in {
        val registration = aRegistration(
          withOrganisationDetails(
            OrganisationDetails(organisationType = Some(PARTNERSHIP),
                                partnershipDetails = Some(
                                  generalPartnershipDetails.copy(partners =
                                    Seq(aLimitedCompanyPartner(), aSoleTraderPartner())
                                  )
                                ),
                                subscriptionStatus = Some(NOT_SUBSCRIBED)
            )
          )
        )
        mockRegistrationFind(registration)
        mockRegistrationUpdate()
        mockGetPartnershipBusinessDetails(partnershipBusinessDetails)

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }
    }

    "return 303" when {
      "form is submitted" in {

        val registrations = Table("Registration",
                                  aCompletedUkCompanyRegistration,
                                  aCompletedSoleTraderRegistration,
                                  aCompletedGeneralPartnershipRegistration,
                                  aCompletedScottishPartnershipRegistration
        )

        var submissionCount = 0

        forAll(registrations) { registration =>
          val completedRegistration =
            registration.copy(incorpJourneyId = Some(UUID.randomUUID().toString))
          mockRegistrationFind(completedRegistration)
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

      "display page and registration not in ready state" in {
        val unreadyRegistration =
          aCompletedGeneralPartnershipRegistration
            .copy(incorpJourneyId = Some(UUID.randomUUID().toString))
            .copy(organisationDetails =
              aCompletedGeneralPartnershipRegistration.organisationDetails.copy(organisationType =
                None
              )
            )
        mockRegistrationFind(unreadyRegistration)

        val result = controller.displayPage()(postRequest(JsObject.empty))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.TaskListController.displayPage().url)
      }

      "display task list page when all groupMember are removed" in {
        val registrationRequest: Registration =
          createRegistrationWithGroupMember(groupMember.copy(customerIdentification1 = ""))

        val sanitisedReg = registrationRequest.copy(groupDetail =
          registrationRequest.groupDetail.map(_.copy(members = Seq.empty))
        );

        when(mockRegistrationFilterService.removePartialGroupMembers(any()))
          .thenReturn(sanitisedReg)
        mockRegistrationFind(registrationRequest)
        mockRegistrationUpdate

        val result = controller.displayPage()(postRequest(JsObject.empty))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.TaskListController.displayPage().url)

        verify(mockRegistrationConnector).update(
          ArgumentMatchers.eq(
            sanitisedReg.copy(metaData =
              registrationRequest.metaData.copy(registrationReviewed = false,
                                                registrationCompleted = false
              )
            )
          )
        )(any())
      }
    }

    "throw exceptions" when {
      "display page and get details fails for uk company" in {
        mockRegistrationFindFailure()

        val result = controller.displayPage()(getRequest())

        intercept[Exception](status(result))
      }

      "display page and get details fails for sole trader" in {
        mockRegistrationFindFailure()

        val result = controller.displayPage()(getRequest())

        intercept[Exception](status(result))
      }

      "display page and get details fails for partnership" in {
        mockRegistrationFindFailure()

        val result = controller.displayPage()(getRequest())

        intercept[Exception](status(result))
      }

      "display page and update cache fail" in {
        val registrationRequest: Registration =
          createRegistrationWithGroupMember(groupMember.copy(customerIdentification1 = ""))
        mockRegistrationFind(registrationRequest)

        val sanitisedReg = registrationRequest.copy(groupDetail =
          registrationRequest.groupDetail.map(_.copy(members = Seq.empty))
        );

        when(mockRegistrationFilterService.removePartialGroupMembers(any()))
          .thenReturn(sanitisedReg)

        mockRegistrationException()

        val result = controller.displayPage()(getRequest())

        intercept[Exception](status(result))
      }

      "submit registration with missing business partner id" in {
        val registrationWithMissingBusinessPartnerId =
          aCompletedUkCompanyRegistration
            .copy(incorpJourneyId = Some(UUID.randomUUID().toString))
            .copy(organisationDetails =
              aCompletedUkCompanyRegistration.organisationDetails.copy(incorporationDetails =
                Some(
                  aCompletedUkCompanyRegistration.organisationDetails.incorporationDetails.get.copy(
                    registration =
                      aCompletedUkCompanyRegistration.organisationDetails.incorporationDetails.get.registration.map {
                        reg => reg.copy(registeredBusinessPartnerId = None)
                      }
                  )
                )
              )
            )
        mockRegistrationFind(registrationWithMissingBusinessPartnerId)
        mockSubscriptionSubmit(subscriptionCreateOrUpdate)

        val result = controller.submit()(postRequest(JsObject.empty))

        intercept[IllegalStateException](status(result))
      }
    }

    "send audit event" when {
      "submission of audit event" in {

        val completedRegistration = aCompletedUkCompanyRegistration
        mockRegistrationFind(completedRegistration)
        mockSubscriptionSubmit(subscriptionCreateOrUpdate)

        await(controller.submit()(postRequest(JsObject.empty)))

        val completedRegistrationWithNrsDetail = completedRegistration.copy(metaData =
          aCompletedUkCompanyRegistration.metaData.copy(nrsDetails =
            Some(
              NrsDetails(nrSubsmissionId = subscriptionCreateOrUpdate.nrsSubmissionId,
                         failureReason = None
              )
            )
          )
        )

        verify(mockAuditor, Mockito.atLeast(1)).registrationSubmitted(
          ArgumentMatchers.eq(completedRegistrationWithNrsDetail),
          ArgumentMatchers.eq(Some("XXPPTP123456789")),
          any()
        )(any(), any())
      }
    }

    "return an error" when {

      "user is not authorised" in {
        unAuthorizedUser()
        val result = controller.displayPage()(getRequest())

        intercept[RuntimeException](status(result))
      }
    }

    "redirect to the error page" when {

      "user submits form and the submission fails with an exception" in {
        mockRegistrationFind(aCompletedUkCompanyRegistration)
        mockSubscriptionSubmitFailure(new IllegalStateException("BANG!"))
        val result =
          controller.submit()(postRequest(JsObject.empty))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          routes.NotableErrorController.subscriptionFailure().url
        )

        metricsMock.defaultRegistry.counter(
          "ppt.registration.failed.submission.counter"
        ).getCount mustBe 1
      }

      "user submits form and the submission fails with an error response" in {
        val registration = aCompletedUkCompanyRegistration
        mockRegistrationFind(registration)
        mockSubscriptionSubmitFailure(
          SubscriptionCreateOrUpdateResponseFailure(
            List(
              EisError("INVALID_SAFEID",
                       "The remote endpoint has indicated that the SAFEID provided is invalid."
              )
            )
          )
        )

        val result = controller.submit()(postRequest(JsObject.empty))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          routes.NotableErrorController.subscriptionFailure().url
        )
      }

      "user submits form and the enrolment initiation fails" in {
        val registration = aCompletedUkCompanyRegistration
        mockRegistrationFind(registration)
        mockSubscriptionSubmit(subscriptionCreateWithEnrolmentFailure)

        val result = controller.submit()(postRequest(JsObject.empty))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.NotableErrorController.enrolmentFailure().url)
      }

      "duplicate subscription detected at the back end" in {
        val registration = aCompletedUkCompanyRegistration
        mockRegistrationFind(registration)
        mockSubscriptionSubmitFailure(
          SubscriptionCreateOrUpdateResponseFailure(
            List(
              EisError("ACTIVE_SUBSCRIPTION_EXISTS",
                       "The remote endpoint has indicated that Business Partner already has active subscription for this regime."
              )
            )
          )
        )

        val result = controller.submit()(postRequest(JsObject.empty))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          routes.NotableErrorController.duplicateRegistration().url
        )
      }
    }
  }

  private def aCompletedUkCompanyRegistration =
    aRegistration(withOrganisationDetails(registeredUkCompanyOrgDetails()),
                  withLiabilityDetails(liabilityDetails),
                  withPrimaryContactDetails(primaryContactDetails),
                  withMetaData(MetaData(registrationCompleted = true))
    )

  private def aCompletedSoleTraderRegistration =
    aRegistration(withOrganisationDetails(registeredSoleTraderOrgDetails()),
                  withLiabilityDetails(liabilityDetails),
                  withPrimaryContactDetails(primaryContactDetails),
                  withMetaData(MetaData(registrationCompleted = true))
    )

  private def aCompletedGeneralPartnershipRegistration =
    aRegistration(withOrganisationDetails(registeredGeneralPartnershipOrgDetails()),
                  withLiabilityDetails(liabilityDetails),
                  withPrimaryContactDetails(primaryContactDetails),
                  withMetaData(MetaData(registrationCompleted = true))
    )

  private def aCompletedScottishPartnershipRegistration =
    aRegistration(withOrganisationDetails(registeredScottishPartnershipOrgDetails()),
                  withLiabilityDetails(liabilityDetails),
                  withPrimaryContactDetails(primaryContactDetails),
                  withMetaData(MetaData(registrationCompleted = true))
    )

  private val liabilityDetails =
    LiabilityDetails(startDate = None)

  private val primaryContactDetails = PrimaryContactDetails(name = Some("Jack Gatsby"),
                                                            jobTitle = Some("Developer"),
                                                            phoneNumber = Some("0203 4567 890"),
                                                            email = Some("test@test.com"),
                                                            address = Some(
                                                              Address(addressLine1 =
                                                                        "2 Scala Street",
                                                                      addressLine2 = Some("Soho"),
                                                                      addressLine3 = None,
                                                                      townOrCity = "London",
                                                                      maybePostcode = Some("W1T 2HN"),
                                                                      countryCode = "GB"
                                                              )
                                                            )
  )

  def createRegistrationWithGroupMember(members: GroupMember*): Registration =
    aRegistration(
      withOrganisationDetails(
        OrganisationDetails(organisationType = Some(UK_COMPANY),
                            incorporationDetails =
                              Some(
                                IncorporationDetails(companyNumber = "123456",
                                                     companyName = "NewPlastics",
                                                     ctutr = "1890894",
                                                     companyAddress = testCompanyAddress,
                                                     registration =
                                                       Some(registrationDetails)
                                )
                              ),
                            subscriptionStatus = Some(NOT_SUBSCRIBED)
        )
      ),
      withGroupDetail(Some(GroupDetail(membersUnderGroupControl = Some(true), members = members)))
    )

}
