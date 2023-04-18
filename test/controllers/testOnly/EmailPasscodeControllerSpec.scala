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

package controllers.testOnly

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.OK
import play.api.test.Helpers.{status, stubMessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import connectors.testOnly.EmailTestOnlyPasscodeConnector
import connectors.{
  DownstreamServiceError,
  FailedToFetchTestOnlyPasscode
}

import scala.concurrent.Future

class EmailPasscodeControllerSpec extends ControllerSpec {
  private val mcc = stubMessagesControllerComponents()

  val mockEmailTestOnlyPasscodeConnector: EmailTestOnlyPasscodeConnector =
    mock[EmailTestOnlyPasscodeConnector]

  private val controller =
    new EmailPasscodeController(authenticate = FakeBasicAuthAction,
                                mcc = mcc,
                                emailTestOnlyPasscodeConnector = mockEmailTestOnlyPasscodeConnector
    )

  "EmailPasscode controller" should {

    "return 200" when {

      "passcode is returned successfully" in {

        when(
          mockEmailTestOnlyPasscodeConnector.getTestOnlyPasscode()(any[HeaderCarrier])
        ).thenReturn(Future.successful(Right("passcodes")))
        val result = controller.testOnlyGetPasscodes()(getRequest())
        status(result) mustBe OK
      }
    }

    "return an error" when {

      "user is not authorised" in {

        val result = controller.testOnlyGetPasscodes()(getRequest())

        intercept[RuntimeException](status(result))
      }

      "connector throws an exception" in {

        when(
          mockEmailTestOnlyPasscodeConnector.getTestOnlyPasscode()(any[HeaderCarrier])
        ).thenReturn(
          Future.successful(
            Left(
              DownstreamServiceError(
                "Some exception",
                FailedToFetchTestOnlyPasscode("Failed to fetch test only passcode")
              )
            )
          )
        )
        val result = controller.testOnlyGetPasscodes()(getRequest())

        intercept[DownstreamServiceError](status(result))
      }
    }
  }

}
