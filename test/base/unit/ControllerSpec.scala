/*
 * Copyright 2024 HM Revenue & Customs
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

package base.unit

import base.PptTestData
import base.PptTestData.nrsCredentials
import config.AppConfig
import controllers.actions.JourneyAction
import controllers.actions.auth.{AmendAuthAction, BasicAuthAction, RegistrationAuthAction}
import models.SignedInUser
import models.registration.Registration
import models.request.AuthenticatedRequest.{PPTEnrolledRequest, RegistrationRequest}
import models.request.{AuthenticatedRequest, IdentityData, JourneyRequest}
import org.mockito.Mockito.spy
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Play.materializer
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.test.Helpers.contentAsString
import play.api.test.{DefaultAwaitTimeout, FakeRequest, Injecting}
import play.twirl.api.Html
import spec.PptTestData
import utils.FakeRequestCSRFSupport._

import java.lang.reflect.Field
import scala.concurrent.{ExecutionContext, Future}

trait ControllerSpec
    extends AnyWordSpecLike
    with MockRegistrationConnector
    with MockSubscriptionConnector
    with MockitoSugar
    with Matchers
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach
    with DefaultAwaitTimeout
    with MockConnectors
    with PptTestData
    with Injecting {

  implicit val ec: ExecutionContext = ExecutionContext.global
  def getAuthenticatedRequest[A](request: Request[A]): RegistrationRequest[A] =
    RegistrationRequest[A](request, IdentityData(Some("Internal-ID"), Some(PptTestData.nrsCredentials)))
  val spyJourneyAction: FakeJourneyAction.type = spy(FakeJourneyAction)

  object FakeBasicAuthAction extends BasicAuthAction {
    override def parser: BodyParser[AnyContent] = PlayBodyParsers().default

    override def invokeBlock[A](
      request: Request[A],
      block: AuthenticatedRequest.RegistrationRequest[A] => Future[Result]
    ): Future[Result] =
      block(getAuthenticatedRequest(request))

    override protected def executionContext: ExecutionContext = ec
  }

  object FakeAmendAuthAction extends AmendAuthAction {
    override def parser: BodyParser[AnyContent] = PlayBodyParsers().default

    override def invokeBlock[A](
      request: Request[A],
      block: AuthenticatedRequest.PPTEnrolledRequest[A] => Future[Result]
    ): Future[Result] =
      block(
        PPTEnrolledRequest(
          request = request,
          identityData = IdentityData(Some("Internal-ID"), Some(PptTestData.nrsCredentials)),
          pptReference = "XMPPT0000000123"
        )
      )

    override protected def executionContext: ExecutionContext = ec
  }

  object FakeRegistrationAuthAction extends RegistrationAuthAction {
    override def parser: BodyParser[AnyContent] = PlayBodyParsers().default

    override def invokeBlock[A](request: Request[A], block: RegistrationRequest[A] => Future[Result]): Future[Result] =
      block(getAuthenticatedRequest(request))

    override protected def executionContext: ExecutionContext = ec
  }

  object FakeJourneyAction extends JourneyAction {

    var registration: Option[Registration] = None
    def setReg(reg: Registration): Unit    = registration = Some(reg)
    def reset(): Unit                      = registration = None

    private def ab =
      new ActionBuilder[JourneyRequest, AnyContent] {
        override def parser: BodyParser[AnyContent] = PlayBodyParsers().default

        override def invokeBlock[A](request: Request[A], block: JourneyRequest[A] => Future[Result]): Future[Result] =
          block(
            JourneyRequest(
              getAuthenticatedRequest(request),
              registration.getOrElse(fail("registration is not set in FakeJourneyAction"))
            )
          )

        override protected def executionContext: ExecutionContext = ec
      }

    override def register: ActionBuilder[JourneyRequest, AnyContent] = ab
    override def amend: ActionBuilder[JourneyRequest, AnyContent]    = ab
  }

  implicit val config: AppConfig = mock[AppConfig]

  def authRequest(
    headers: Headers = Headers(),
    user: SignedInUser = PptTestData.newUser("123")
  ): AuthenticatedRequest[AnyContentAsEmpty.type] =
    authRequest(FakeRequest().withHeaders(headers))

  def authRequest[A](fakeRequest: FakeRequest[A]) =
    RegistrationRequest(fakeRequest, IdentityData(Some("internalId"), Some(nrsCredentials)))

  protected def viewOf(result: Future[Result]): Html = Html(contentAsString(result))

  protected def postRequest(body: JsValue): Request[AnyContentAsJson] =
    postRequest
      .withJsonBody(body)
      .withHeaders(testUserHeaders.toSeq: _*)
      .withCSRFToken

  protected def postRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("POST", "")

  protected def postRequestEncoded(form: AnyRef, sessionId: String = "123"): Request[AnyContentAsFormUrlEncoded] =
    postRequestTuplesEncoded(getTuples(form), sessionId)

  // This function exists because getTuples used in the above may not always encode values correctly
  protected def postRequestTuplesEncoded(
    formTuples: Seq[(String, String)],
    sessionId: String = "123"
  ): Request[AnyContentAsFormUrlEncoded] =
    postRequest
      .withFormUrlEncodedBody(formTuples: _*)
      .withCSRFToken

  protected def getTuples(cc: AnyRef): Seq[(String, String)] =
    cc.getClass.getDeclaredFields.foldLeft(Map.empty[String, String]) { (a, f) =>
      f.setAccessible(true)
      a + (f.getName -> getValue(f, cc))
    }.toList

  private def getValue(field: Field, cc: AnyRef): String =
    field.get(cc) match {
      case c: Option[_] if c.isDefined =>
        c.get match {
          case _: Boolean =>
            throw new RuntimeException(
              "This test helper function was probably about to bypass fromForm and return 'true' rather than 'yes' and is not able to properly encode boolean forms where the controller expects yes"
            )
          case v => v.toString
        }
      case c: String => c
      case _         => ""
    }

  protected def postJsonRequestEncoded(body: (String, String)*): Request[AnyContentAsFormUrlEncoded] =
    postRequest
      .withFormUrlEncodedBody(body: _*)
      .withCSRFToken

  protected def postJsonRequestEncodedFormAction(
    body: Seq[(String, String)],
    sessionId: String = "123"
  ): Request[AnyContentAsFormUrlEncoded] =
    postRequestTuplesEncoded(body, sessionId)

}
