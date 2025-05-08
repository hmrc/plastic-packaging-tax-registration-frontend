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

package controllers.group

import base.PptTestData.nrsCredentials
import base.unit.MockConnectors
import controllers.actions.JourneyAction
import controllers.amendment.group.{AddGroupMemberGrsController, routes => amendRoutes}
import forms.organisation.OrgType
import models.registration.{AmendRegistrationUpdateService, Registration}
import models.request.AuthenticatedRequest.RegistrationRequest
import models.request.{IdentityData, JourneyRequest}
import models.subscriptions.SubscriptionStatus.{NOT_SUBSCRIBED, SUBSCRIBED}
import models.subscriptions.SubscriptionStatusResponse
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{Action, ActionBuilder, AnyContent, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import spec.PptTestData
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GAddGroupMemberGrsControllerSpec extends PlaySpec with MockConnectors with PptTestData with BeforeAndAfterEach {

  private val reg =
    aRegistration(
      withOrganisationDetails(registeredUkCompanyOrgDetails()),
      withGroupDetail(Some(groupDetails.copy(currentMemberOrganisationType = Some(OrgType.UK_COMPANY))))
    )

  private val journeyAction       = mock[JourneyAction]
  private val registrationUpdater = mock[AmendRegistrationUpdateService]
  private val mcc                 = stubMessagesControllerComponents()
  private val actionBuilder       = mock[ActionBuilder[JourneyRequest, AnyContent]]

  type RequestAsyncFunction = JourneyRequest[AnyContent] => Future[Result]

  private lazy val sut = new AddGroupMemberGrsController(
    journeyAction,
    mockUkCompanyGrsConnector,
    mockSubscriptionsConnector,
    mockPartnershipGrsConnector,
    registrationUpdater,
    addressConversionUtils,
    mcc
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(journeyAction.amend).thenReturn(actionBuilder)
    when(actionBuilder.async(any[RequestAsyncFunction])) thenAnswer byConvertingFunctionArgumentsToFutureAction

    val detailsFromGrs =
      incorporationDetails.copy(companyNumber = "22222", companyName = "Existing Company")
    mockGetUkCompanyDetails(detailsFromGrs)
    when(registrationUpdater.updateRegistration(any)(any, any)).thenReturn(Future.successful(reg))

  }

  "grsCallbackAmendAddGroupMember" should {
    "redirect to the amend-add-group-member-confirm-address page when status is ok" in {

      mockGetSubscriptionStatus(SubscriptionStatusResponse(NOT_SUBSCRIBED, Some("XDPPT1234567890")))

      val result: Future[Result] = sut.grsCallbackNewMember("123")(createRequest(reg))

      status(result) mustBe SEE_OTHER
      assertResult(
        amendRoutes.AddGroupMemberConfirmBusinessAddressController.displayPage("uuid").url,
        redirectLocation(result).value
      )
    }

    "redirect to the /group-organisation-already-registered page when user is alrready subscribed" in {
      mockGetSubscriptionStatus(SubscriptionStatusResponse(SUBSCRIBED, Some("XDPPT1234567890")))

      val result: Future[Result] = sut.grsCallbackNewMember("123")(createRequest(reg))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.NotableErrorController.groupMemberAlreadyRegistered().url)
    }
  }

  private def assertResult(expectedLocation: String, actualLocation: String) = {
    val regexp      = expectedLocation.substring(0, expectedLocation.lastIndexOf('/'))
    val uuid        = actualLocation.substring(actualLocation.lastIndexOf('/') + 1, actualLocation.length)
    val uuidPattern = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".r

    assert(uuidPattern.matches(uuid))
    assert(s"^$regexp.*".r.matches(actualLocation))
  }

  private def authRequest[A](fakeRequest: FakeRequest[A]) =
    RegistrationRequest(fakeRequest, IdentityData(Some("internalId"), Some(nrsCredentials)))

  private def byConvertingFunctionArgumentsToFutureAction: (RequestAsyncFunction) => Action[AnyContent] =
    (function: RequestAsyncFunction) =>
      when(mock[Action[AnyContent]].apply(any))
        .thenAnswer((request: JourneyRequest[AnyContent]) => function(request))
        .getMock[Action[AnyContent]]

  private def createRequest(reg: Registration): JourneyRequest[AnyContent] =
    JourneyRequest(authRequest(FakeRequest()), reg)

}
