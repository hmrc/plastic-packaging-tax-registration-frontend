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

package uk.gov.hmrc.plasticpackagingtax.registration.services

import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.GroupMember
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{GroupDetail, Registration}

class RegistrationFilterService {

  def filterByGroupMembers(registration: Registration): Registration = {
    val groupDetail: Option[GroupDetail] = registration.groupDetail

    if (groupDetail.isEmpty) return registration
    else
      registration.copy(groupDetail =
        Some(groupDetail.get.copy(members = filterByValidGroupMember(groupDetail)))
      )
  }

  def filterByValidGroupMember(groupDetail: Option[GroupDetail]): Seq[GroupMember] =
    groupDetail.toSeq
      .flatMap(_.members)
      .filter(_.isValid())

}
