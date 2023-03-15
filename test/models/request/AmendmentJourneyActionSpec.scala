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

package models.request

import base.PptTestData.newUser
import base.unit.MockAmendmentJourneyAction
import models.SignedInUser
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.OK
import play.api.mvc.{AnyContent, Result, Results}
import play.api.test.Helpers.status
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import spec.PptTestData
import uk.gov.hmrc.auth.core._
import models.enrolment.PptEnrolment

import scala.concurrent.Future

class AmendmentJourneyActionSpec
    extends MockAmendmentJourneyAction with DefaultAwaitTimeout with BeforeAndAfterEach
    with PptTestData {

  private val responseGenerator = mock[JourneyRequest[_] => Future[Result]]

  private val requestCaptor: ArgumentCaptor[JourneyRequest[AnyContent]] =
    ArgumentCaptor.forClass(classOf[JourneyRequest[AnyContent]])

  when(responseGenerator.apply(requestCaptor.capture())).thenReturn(Future.successful(Results.Ok))

  private val user: SignedInUser = newUser()

  private val enrolledUser = user.copy(enrolments =
    Enrolments(
      Set(
        new Enrolment(PptEnrolment.IdentifierName,
                      Seq(EnrolmentIdentifier(PptEnrolment.Key, "XMPPT0000000123")),
                      "activated"
        )
      )
    )
  )

  private val registration = aRegistration()

  override protected def beforeEach(): Unit = {
    inMemoryRegistrationAmendmentRepository.reset()
    simulateGetSubscriptionSuccess(registration)
  }

  "Amendment Journey Action" should {

    "return a JourneyRequest populated with a registration obtained via the Subscription Connector" when {

      "no registration is cached against the user's session" in {
        val request = new AuthenticatedRequest(
          FakeRequest().withSession((AmendmentJourneyAction.SessionId, "123")),
          enrolledUser,
          pptReferenceFromUsersEnrolments(enrolledUser)
        )

        status(mockAmendmentJourneyAction.invokeBlock(request, responseGenerator)) mustBe OK
        requestCaptor.getValue.registration mustBe registration
      }

    }

    "return a JourneyRequest populated with a cached registration" when {

      "a registration is cached on the user's session" in {
        val cachedRegistration = aRegistration().copy(id = "3453456")
        inMemoryRegistrationAmendmentRepository.put("123", cachedRegistration)
        val request = new AuthenticatedRequest(
          FakeRequest().withSession((AmendmentJourneyAction.SessionId, "123")),
          enrolledUser,
          pptReferenceFromUsersEnrolments(enrolledUser)
        )

        status(mockAmendmentJourneyAction.invokeBlock(request, responseGenerator)) mustBe OK
        requestCaptor.getValue.registration mustBe cachedRegistration
      }

    }

    "throw InsufficientEnrolments exception" when {

      "user does not have an internal id" in {
        val request = new AuthenticatedRequest(
          FakeRequest(),
          user.copy(identityData = user.identityData.copy(internalId = None))
        )

        intercept[InsufficientEnrolments] {
          mockAmendmentJourneyAction.invokeBlock(request, responseGenerator)
        }
      }

      "user does not have a ppt enrolment" in {
        val request = new AuthenticatedRequest(FakeRequest(), user)

        intercept[InsufficientEnrolments] {
          mockAmendmentJourneyAction.invokeBlock(request, responseGenerator)
        }
      }

    }

    "throw SessionRecordNotFound" when {

      "no active session present" in {
        val request = new AuthenticatedRequest(FakeRequest(),
                                               enrolledUser,
                                               pptReferenceFromUsersEnrolments(enrolledUser)
        )

        intercept[SessionRecordNotFound] {
          mockAmendmentJourneyAction.invokeBlock(request, responseGenerator)
        }
      }

    }
  }

}
