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

package uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration

import play.api.libs.json.{Json, OFormat}

case class OtherPartner(
  id: Option[String] = None,
  organisationName: Option[String] = None,
  firstName: Option[String] = None,
  lastName: Option[String] = None,
  emailAddress: Option[String] = None,
  phoneNumber: Option[String] = None
) {

  def contactName: Option[String] =
    for {
      firstName <- firstName
      lastName  <- lastName
    } yield Seq(firstName, lastName).mkString(" ")

}

object OtherPartner {
  implicit val format: OFormat[OtherPartner] = Json.format[OtherPartner]
}
