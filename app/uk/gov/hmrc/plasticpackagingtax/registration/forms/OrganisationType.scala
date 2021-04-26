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
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.OrgType
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.OrgType.{
  OrgType,
  SOLE_TRADER,
  UK_LIMITED
}

case class OrganisationType(organisationType: OrgType)

object OrganisationType extends CommonFormValidators {

  lazy val emptyError = "primaryContactDetails.confirmAddress.empty.error"

  def form(): Form[OrganisationType] =
    Form(
      mapping(
        "organisationType" -> text()
          .verifying(emptyError,
                     contains(Seq(OrgType.UK_LIMITED.toString, OrgType.SOLE_TRADER.toString))
          )
      )(OrganisationType.apply)(OrganisationType.unapply)
    )

  def apply(value: String): OrganisationType =
    if (value == UK_LIMITED.toString)
      OrganisationType(UK_LIMITED)
    else OrganisationType(SOLE_TRADER)

  def unapply(organisationType: OrganisationType): Option[String] =
    organisationType.organisationType match {
      case UK_LIMITED  => Some(UK_LIMITED.toString)
      case SOLE_TRADER => Some(SOLE_TRADER.toString)
    }

}
