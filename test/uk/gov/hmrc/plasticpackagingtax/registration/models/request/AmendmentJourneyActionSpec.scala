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

package uk.gov.hmrc.plasticpackagingtax.registration.models.request

import base.PptTestData.newUser
import base.unit.MockAmendmentJourneyAction
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.OK
import play.api.mvc.{AnyContent, Result, Results}
import play.api.test.Helpers.status
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments, InsufficientEnrolments}
import uk.gov.hmrc.plasticpackagingtax.registration.models.enrolment.PptEnrolment

import scala.concurrent.Future

class AmendmentJourneyActionSpec extends MockAmendmentJourneyAction with DefaultAwaitTimeout {

  private val responseGenerator = mock[JourneyRequest[_] => Future[Result]]

  private val requestCaptor: ArgumentCaptor[JourneyRequest[AnyContent]] =
    ArgumentCaptor.forClass(classOf[JourneyRequest[AnyContent]])

  when(responseGenerator.apply(requestCaptor.capture())).thenReturn(Future.successful(Results.Ok))

  private val user = newUser()

  "Amendment Journey Action" should {

    "returns a JourneyRequest with a registration" in {
      val request = new AuthenticatedRequest(
        FakeRequest(),
        user.copy(enrolments =
          Enrolments(
            Set(
              new Enrolment(PptEnrolment.Identifier,
                            Seq(EnrolmentIdentifier(PptEnrolment.Key, "XMPPT0000000123")),
                            "activated"
              )
            )
          )
        )
      )
      status(mockAmendmentJourneyAction.invokeBlock(request, responseGenerator)) mustBe OK
      requestCaptor.getValue.registration mustBe mockSubscription
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
  }
}
