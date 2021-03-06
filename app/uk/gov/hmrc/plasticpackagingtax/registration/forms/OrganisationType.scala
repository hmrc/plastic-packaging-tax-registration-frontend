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

package uk.gov.hmrc.plasticpackagingtax.registration.forms

import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.libs.json.{Format, Reads, Writes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrgType.OrgType

object OrgType extends Enumeration {
  type OrgType = Value
  val UK_COMPANY: Value                        = Value("UK company")
  val SOLE_TRADER: Value                       = Value("Sole trader")
  val PARTNERSHIP: Value                       = Value("Partnership")
  val CHARITY_OR_NOT_FOR_PROFIT: Value         = Value("Charity or Not for Profit")
  def withNameOpt(name: String): Option[Value] = values.find(_.toString == name)

  implicit val format: Format[OrgType] =
    Format(Reads.enumNameReads(OrgType), Writes.enumNameWrites)

}

case class OrganisationType(answer: Option[OrgType])

object OrganisationType extends CommonFormValidators {
  lazy val emptyError = "organisationDetails.type.empty.error"

  def form(): Form[OrganisationType] =
    Form(
      mapping(
        "answer" -> text()
          .verifying(emptyError, contains(OrgType.values.toSeq.map(_.toString)))
      )(OrganisationType.apply)(OrganisationType.unapply)
    )

  def apply(value: String): OrganisationType = OrganisationType(OrgType.withNameOpt(value))

  def unapply(organisationType: OrganisationType): Option[String] =
    organisationType.answer.map(_.toString)

}
