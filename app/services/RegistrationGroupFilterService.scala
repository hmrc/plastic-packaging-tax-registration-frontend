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

package services

import models.registration.Registration

class RegistrationGroupFilterService {

  def removePartialGroupMembers(registration: Registration): Registration =
    registration.copy(groupDetail = registration.groupDetail.map(gd => gd.copy(members = gd.members.filter(_.isValid))))

  def removeGroupDetails(registration: Registration): Registration =
    registration.copy(groupDetail = None)

}
