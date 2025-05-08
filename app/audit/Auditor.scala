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

package audit

import forms.organisation.OrgType.OrgType
import models.registration.Registration
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class Auditor @Inject() (auditConnector: AuditConnector) {

  def registrationSubmitted(
    registration: Registration,
    pptReference: Option[String] = None,
    internalId: Option[String]
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Unit =
    auditConnector.sendExplicitAudit(
      CreateRegistrationEvent.eventType,
      CreateRegistrationEvent(registration, pptReference, internalId)
    )

  def newRegistrationStarted(internalId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Unit =
    auditConnector.sendExplicitAudit(StartRegistrationEvent.eventType, StartRegistrationEvent(UserType.NEW, internalId))

  def orgTypeSelected(internalId: String, orgType: Option[OrgType])(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Unit =
    auditConnector.sendExplicitAudit(OrgTypeRegistrationEvent.eventType, OrgTypeRegistrationEvent(internalId, orgType))

}
