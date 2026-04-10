/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.deregistration

import base.unit.{ControllerSpec, MockDeregistrationDetailRepository}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}

import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.test.Helpers.{await, contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import spec.PptTestData
import models.deregistration.DeregistrationDetails
import play.api.test.FakeRequest
import views.html.deregistration.deregister_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class DeregisterControllerSpec extends ControllerSpec with MockDeregistrationDetailRepository with PptTestData {

  private val mcc      = stubMessagesControllerComponents()
  private val mockPage = mock[deregister_page]

  private val formCaptor: ArgumentCaptor[Form[Boolean]] =
    ArgumentCaptor.forClass(classOf[Form[Boolean]])

  private val deregisterController =
    new DeregisterController(FakeAmendAuthAction, mcc, inMemoryDeregistrationDetailRepository, config, mockPage)

  override protected def beforeEach(): Unit = {
    when(mockPage.apply(formCaptor.capture())(any(), any())).thenReturn(HtmlFormat.raw("Deregister?"))
    when(config.pptAccountUrl).thenReturn("/account")
  }

  override protected def afterEach(): Unit = {
    reset(mockPage)
    inMemoryDeregistrationDetailRepository.reset()
  }

  "Deregistration Controller" should {
    "display deregister page" when {
      "no previous detail held in repository" in {
        deregisterPageDisplayedAsExpected()

        formCaptor.getValue.value shouldBe None
      }
      "previous detail held in repository" in {
        inMemoryDeregistrationDetailRepository.put(DeregistrationDetails(Some(true), None))

        deregisterPageDisplayedAsExpected()

        formCaptor.getValue.value shouldBe Some(true)
      }
    }

    "redirect to the deregistration reason page and update repository" when {
      "user suggests they would like to deregister" in {

        val correctForm = Seq("deregister" -> "yes")
        val resp        = await(deregisterController.submit()(postJsonRequestEncoded(correctForm: _*)))

        redirectLocation(Future.successful(resp)) shouldBe Some(routes.DeregisterReasonController.displayPage().url)

        inMemoryDeregistrationDetailRepository.get().map { deregistrationDetail =>
          deregistrationDetail.deregister shouldBe Some(true)
        }
      }
    }

    "redirect to the PPT account page" when {
      "user suggests they do not want to deregister" in {

        val correctForm = Seq("deregister" -> "no")
        val resp        = deregisterController.submit()(postJsonRequestEncoded(correctForm: _*))

        redirectLocation(resp) shouldBe Some("/account")
      }
    }

    "redisplay the deregister page" when {
      "user does not select an answer" in {

        val correctForm = Seq("deregister" -> "")
        val resp        = deregisterController.submit()(postJsonRequestEncoded(correctForm: _*))

        status(resp) shouldBe BAD_REQUEST
        contentAsString(resp) shouldBe "Deregister?"
      }
    }
  }

  private def deregisterPageDisplayedAsExpected() = {

    val resp = deregisterController.displayPage()(FakeRequest())

    status(resp) shouldBe OK
    contentAsString(resp) shouldBe "Deregister?"
  }

}
