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

import connectors.DownstreamServiceError
import controllers.contact.RegistrationException
import play.api.mvc.{Request, WrappedRequest}

sealed abstract class AuthenticatedRequest[+A](request: Request[A]) extends WrappedRequest[A](request) {
  val identityData: IdentityData
  val internalID: String = identityData.internalId.getOrElse(throw new RuntimeException("Internal ID not found."))
  def cacheId: String
  val credId: String = identityData.credentials.map(_.providerId).getOrElse(
    throw DownstreamServiceError("Cannot find user credentials id",
      RegistrationException("Cannot find user credentials id")
    )
  )
}

object AuthenticatedRequest {

  final case class RegistrationRequest[+A](
                                      request: Request[A],
                                      identityData: IdentityData
                                    ) extends AuthenticatedRequest[A](request) {
    override def cacheId: String = internalID //todo make use of.
  }

  final case class PPTEnrolledRequest[+A](
                                     request: Request[A],
                                     identityData: IdentityData,
                                     pptReference: String
                                   ) extends AuthenticatedRequest[A](request) {
    override def cacheId: String = s"$internalID-$pptReference"
  }

}

