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

package base.unit

import builders.RegistrationBuilder
import org.scalatestplus.mockito.MockitoSugar
import repositories.DeregistrationDetailRepositoryImpl

import scala.concurrent.ExecutionContext

trait MockDeregistrationDetailRepository extends RegistrationBuilder with MockitoSugar {

  protected val inMemoryDeregistrationDetailRepository =
    new DeregistrationDetailRepositoryImpl(new InMemoryUserDetailsRepository()(ExecutionContext.global))(
      ExecutionContext.global
    )

}
