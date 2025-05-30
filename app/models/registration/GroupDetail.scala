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

package models.registration

import play.api.libs.json.{Json, OFormat}
import forms.organisation.OrgType.OrgType
import models.registration.group.{GroupError, GroupMember}
import views.viewmodels.TaskStatus

case class GroupDetail(
  membersUnderGroupControl: Option[Boolean] = None,
  members: Seq[GroupMember] = Seq.empty,
  currentMemberOrganisationType: Option[OrgType] = None,
  groupError: Option[GroupError] = None
) {

  def status: TaskStatus =
    if (members.isEmpty)
      TaskStatus.NotStarted
    else if (members.exists(member => !member.isValid))
      TaskStatus.InProgress
    else TaskStatus.Completed

  def businessName(memberId: String): Option[String] = findGroupMember(Some(memberId), None).map(_.businessName)

  def withUpdatedOrNewMember(member: GroupMember): GroupDetail =
    this.copy(members = updateOrAddMember(member))

  private def updateOrAddMember(member: GroupMember): Seq[GroupMember] = {
    val updatedMembers = members.map {
      case existingMember if existingMember.id == member.id => member
      case existingMember                                   => existingMember
    }

    if (!updatedMembers.exists(_.id == member.id))
      updatedMembers :+ member
    else
      updatedMembers
  }

  def findGroupMember(memberId: Option[String], crn: Option[String]): Option[GroupMember] =
    (memberId, crn) match {
      case (Some(id), _)            => members.find(m => m.id == id)
      case (_, Some(companyNumber)) => members.find(m => m.customerIdentification1 == companyNumber)
      case _                        => None
    }

  lazy val latestMember: Option[GroupMember] = members.lastOption
}

object GroupDetail {
  implicit val format: OFormat[GroupDetail] = Json.format[GroupDetail]
}
