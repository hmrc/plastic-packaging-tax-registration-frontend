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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers

import java.util.UUID
import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.`given`
import org.mockito.Mockito.{reset, verify}
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.JsObject
import play.api.test.Helpers.{await, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrgType.{
  PARTNERSHIP,
  SOLE_TRADER,
  UK_COMPANY
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.PartnershipTypeEnum.LIMITED_LIABILITY_PARTNERSHIP
import uk.gov.hmrc.plasticpackagingtax.registration.forms.{Address, LiabilityWeight}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.IncorporationDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.nrs.NrsDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration._
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.{
  EisError,
  SubscriptionCreateResponseFailure
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.{
  duplicate_subscription_page,
  review_registration_page
}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class ReviewRegistrationControllerSpec extends ControllerSpec with TableDrivenPropertyChecks {
  private val mockReviewRegistrationPage    = mock[review_registration_page]
  private val mockDuplicateSubscriptionPage = mock[duplicate_subscription_page]
  private val mcc                           = stubMessagesControllerComponents()

  private val controller =
    new ReviewRegistrationController(authenticate = mockAuthAction,
                                     mockJourneyAction,
                                     mcc = mcc,
                                     registrationConnector = mockRegistrationConnector,
                                     subscriptionsConnector = mockSubscriptionsConnector,
                                     auditor = mockAuditor,
                                     reviewRegistrationPage = mockReviewRegistrationPage,
                                     metrics = metricsMock,
                                     duplicateSubscriptionPage = mockDuplicateSubscriptionPage
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    val registration = aRegistration()

    mockRegistrationFind(registration)
    given(mockReviewRegistrationPage.apply(any(), any(), any(), any())(any(), any())).willReturn(
      HtmlFormat.empty
    )
    given(mockDuplicateSubscriptionPage.apply(any())(any(), any())).willReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(mockReviewRegistrationPage)
    super.afterEach()
  }

  "Review registration controller" should {

    "return 200" when {

      "user is authorised and display page method is invoked with uk company" in {
        val registration = aRegistration(
          withOrganisationDetails(
            OrganisationDetails(organisationType = Some(UK_COMPANY),
                                incorporationDetails =
                                  Some(
                                    IncorporationDetails(companyNumber = "123456",
                                                         companyName = "NewPlastics",
                                                         ctutr = "1890894",
                                                         businessVerificationStatus = "PASS",
                                                         companyAddress = testCompanyAddress,
                                                         registration =
                                                           incorporationRegistrationDetails
                                    )
                                  )
            )
          )
        )
        authorizedUser()
        mockRegistrationFind(registration)
        mockRegistrationUpdate(registration)

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
        verify(mockReviewRegistrationPage).apply(registration = ArgumentMatchers.eq(registration),
                                                 incorporationDetails = ArgumentMatchers.eq(
                                                   registration.organisationDetails.incorporationDetails
                                                 ),
                                                 soleTraderDetails = ArgumentMatchers.eq(null),
                                                 partnershipDetails = ArgumentMatchers.eq(null)
        )(any(), any())
      }

      "user is authorised and display page method is invoked with sole trader" in {
        val registration = aRegistration(
          withOrganisationDetails(
            OrganisationDetails(organisationType = Some(SOLE_TRADER),
                                soleTraderDetails = Some(soleTraderIncorporationDetails)
            )
          )
        )
        authorizedUser()
        mockRegistrationFind(registration)
        mockRegistrationUpdate(registration)

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

      "user is authorised and display page method is invoked with partnership" in {
        val registration = aRegistration(
          withOrganisationDetails(
            OrganisationDetails(organisationType = Some(PARTNERSHIP),
                                partnershipDetails = Some(partnershipDetails)
            )
          )
        )
        authorizedUser()
        mockRegistrationFind(registration)
        mockRegistrationUpdate(registration)
        mockGetGeneralPartnershipDetails(generalPartnershipDetails)

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
          authorizedUser()
          val completedRegistration =
            registration.copy(incorpJourneyId = Some(UUID.randomUUID().toString))
          mockRegistrationFind(completedRegistration)
          mockSubscriptionSubmit(subscriptionCreate)

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
        authorizedUser()
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
        redirectLocation(result) mustBe Some(routes.RegistrationController.displayPage().url)
      }

    }

    "throw exceptions" when {
      "display page and get details fails for uk company" in {
        authorizedUser()
        mockRegistrationFindFailure()

        val result = controller.displayPage()(getRequest())

        intercept[Exception](status(result))
      }

      "display page and get details fails for sole trader" in {
        authorizedUser()
        mockRegistrationFindFailure()

        val result = controller.displayPage()(getRequest())

        intercept[Exception](status(result))
      }

      "display page and get details fails for partnership" in {
        authorizedUser()
        mockRegistrationFindFailure()

        val result = controller.displayPage()(getRequest())

        intercept[Exception](status(result))
      }

      "submit unsupported partnership type" in {
        authorizedUser()
        val unsupportedPartnershipTypeRegistration =
          aCompletedGeneralPartnershipRegistration
            .copy(incorpJourneyId = Some(UUID.randomUUID().toString))
            .copy(organisationDetails =
              aCompletedGeneralPartnershipRegistration.organisationDetails.copy(partnershipDetails =
                Some(
                  aCompletedGeneralPartnershipRegistration.organisationDetails.partnershipDetails.get.copy(
                    partnershipType = LIMITED_LIABILITY_PARTNERSHIP
                  )
                )
              )
            )
        mockRegistrationFind(unsupportedPartnershipTypeRegistration)
        mockSubscriptionSubmit(subscriptionCreate)

        val result = controller.submit()(postRequest(JsObject.empty))

        intercept[IllegalStateException](status(result))
      }

      "submit registration with missing business partner id" in {
        authorizedUser()
        val registrationWithMissingBusinessPartnerId =
          aCompletedUkCompanyRegistration
            .copy(incorpJourneyId = Some(UUID.randomUUID().toString))
            .copy(organisationDetails =
              aCompletedUkCompanyRegistration.organisationDetails.copy(incorporationDetails =
                Some(
                  aCompletedUkCompanyRegistration.organisationDetails.incorporationDetails.get.copy(
                    registration =
                      aCompletedUkCompanyRegistration.organisationDetails.incorporationDetails.get.registration.copy(
                        registeredBusinessPartnerId = None
                      )
                  )
                )
              )
            )
        mockRegistrationFind(registrationWithMissingBusinessPartnerId)
        mockSubscriptionSubmit(subscriptionCreate)

        val result = controller.submit()(postRequest(JsObject.empty))

        intercept[IllegalStateException](status(result))
      }
    }

    "send audit event" when {
      "submission of audit event" in {
        authorizedUser()

        val completedRegistration = aCompletedUkCompanyRegistration
        mockRegistrationFind(completedRegistration)
        mockSubscriptionSubmit(subscriptionCreate)

        await(controller.submit()(postRequest(JsObject.empty)))

        val completedRegistrationWithNrsDetail = completedRegistration.copy(metaData =
          aCompletedUkCompanyRegistration.metaData.copy(nrsDetails =
            Some(
              NrsDetails(nrSubsmissionId = subscriptionCreate.nrsSubmissionId, failureReason = None)
            )
          )
        )

        verify(mockAuditor, Mockito.atLeast(1)).registrationSubmitted(
          ArgumentMatchers.eq(completedRegistrationWithNrsDetail),
          ArgumentMatchers.eq(Some("XXPPTP123456789"))
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
        authorizedUser()
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
        authorizedUser()
        mockRegistrationFind(registration)
        mockSubscriptionSubmitFailure(
          SubscriptionCreateResponseFailure(
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
        authorizedUser()
        mockRegistrationFind(registration)
        mockSubscriptionSubmit(subscriptionCreateWithEnrolmentFailure)

        val result = controller.submit()(postRequest(JsObject.empty))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.NotableErrorController.enrolmentFailure().url)
      }
    }

    "display the duplicate subscription page" when {
      "duplicate subscription detected at the back end" in {
        val registration = aCompletedUkCompanyRegistration
        authorizedUser()
        mockRegistrationFind(registration)
        mockSubscriptionSubmitFailure(
          SubscriptionCreateResponseFailure(
            List(
              EisError("ACTIVE_SUBSCRIPTION_EXISTS",
                       "The remote endpoint has indicated that Business Partner already has active subscription for this regime."
              )
            )
          )
        )

        val result = controller.submit()(postRequest(JsObject.empty))

        status(result) mustBe OK
        verify(mockDuplicateSubscriptionPage).apply(
          ArgumentMatchers.eq(registration.organisationDetails.businessName)
        )(any(), any())
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
    LiabilityDetails(weight = Some(LiabilityWeight(Some(1000))), startDate = None)

  private val primaryContactDetails = PrimaryContactDetails(name = Some("Jack Gatsby"),
                                                            jobTitle = Some("Developer"),
                                                            phoneNumber = Some("0203 4567 890"),
                                                            email = Some("test@test.com"),
                                                            address = Some(
                                                              Address(addressLine1 =
                                                                        "2 Scala Street",
                                                                      addressLine2 = Some("Soho"),
                                                                      townOrCity = "London",
                                                                      postCode = "W1T 2HN"
                                                              )
                                                            )
  )

}
