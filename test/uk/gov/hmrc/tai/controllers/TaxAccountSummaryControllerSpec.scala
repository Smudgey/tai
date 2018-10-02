/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.tai.controllers

import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatestplus.play.PlaySpec
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.MissingBearerToken
import uk.gov.hmrc.http._
import uk.gov.hmrc.tai.controllers.predicates.AuthenticationPredicate
import uk.gov.hmrc.tai.mocks.MockAuthenticationPredicate
import uk.gov.hmrc.tai.model.tai.TaxYear
import uk.gov.hmrc.tai.util.NpsExceptions

import scala.concurrent.Future

class TaxAccountSummaryControllerSpec extends PlaySpec
  with NpsExceptions
  with MockAuthenticationPredicate
  with TaxAccountSummaryControllerTestData{

  val controller: TaxAccountSummaryController = createController()
  val notLoggedInController: TaxAccountSummaryController = createController(notLoggedInAuthenticationPredicate)

  private def createController(authentication: AuthenticationPredicate = loggedInAuthenticationPredicate) =
    new TaxAccountSummaryController(taxAccountSummaryService, authentication, incomeService, employmentService, trackingService)

  "taxAccountSummaryForYear" must {
    "return the tax summary for the given year" when {
      "tax year is CY+1" in {
        when(taxAccountSummaryService.taxAccountSummary(Matchers.eq(nino),Matchers.eq(taxYear))(any()))
          .thenReturn(Future.successful(taxAccountSummaryForYearCY1))

        val result = controller.taxAccountSummaryForYear(nino, taxYear)(FakeRequest())

        status(result) mustBe OK
        contentAsJson(result) mustBe expectedTaxAccountSummaryForYearJson
      }
    }

    "return NOT AUTHORISED" when {
      "the user is not logged in" in {
        intercept[MissingBearerToken]{
          await(notLoggedInController.taxAccountSummaryForYear(nino, TaxYear().next)(FakeRequest()))
        }
      }
    }

    "return Locked exception" when {
      "nps throws locked exception" in {
        when(taxAccountSummaryService.taxAccountSummary(Matchers.eq(nino),Matchers.eq(TaxYear()))(any()))
          .thenReturn(Future.failed(new LockedException("Account is locked")))

        intercept[LockedException]{
          await(controller.taxAccountSummaryForYear(nino, TaxYear())(FakeRequest()))
        }
      }
    }
  }

  "fullTaxSummaryForYear" must {
    "return a fully populated tax summary for the given year" when {
      "tax year is CY+1 and the user has no tax codes, incomes, employments or live journeys" in {
        when(taxAccountSummaryService.taxAccountSummary(Matchers.eq(nino),Matchers.eq(taxYear))(any())).thenReturn(
          Future.successful(taxAccountSummaryForYearCY1))
        when(incomeService.taxCodeIncomes(Matchers.eq(nino), Matchers.eq(taxYear))(any())).thenReturn(
          Future successful fullyPopulatedTaxCodeIncomes)
        when(incomeService.incomes(Matchers.eq(nino), Matchers.eq(taxYear))(any())).thenReturn(
          Future successful fullyPopulatedIncomes)
        when(employmentService.employments(Matchers.eq(nino), Matchers.eq(taxYear))(any())).thenReturn(
          Future successful fullyPopulatedEmployments)
        when(trackingService.isAnyIFormInProgress(Matchers.eq(nino.nino))(any())).thenReturn(Future successful true)

        val result = controller.fullTaxSummaryForYear(nino, taxYear)(FakeRequest())
        status(result) mustBe OK

        contentAsJson(result) mustBe fullyPopulatedFullTaxSummaryForYearJson
      }
    }

    "return a sparsely populated tax summary for the given year" when {
      "tax year is CY+1 and the user has no tax codes, incomes, employments or live journeys" in {
        when(taxAccountSummaryService.taxAccountSummary(Matchers.eq(nino),Matchers.eq(taxYear))(any())).thenReturn(
          Future.successful(taxAccountSummaryForYearCY1))
        when(incomeService.taxCodeIncomes(Matchers.eq(nino), Matchers.eq(taxYear))(any())).thenReturn(
          Future successful sparselyPopulatedTaxCodeIncomes)
        when(incomeService.incomes(Matchers.eq(nino), Matchers.eq(taxYear))(any())).thenReturn(
          Future successful minimallyPopulatedIncomes)
        when(employmentService.employments(Matchers.eq(nino), Matchers.eq(taxYear))(any())).thenReturn(
          Future successful sparselyPopulatedEmployments)
        when(trackingService.isAnyIFormInProgress(Matchers.eq(nino.nino))(any())).thenReturn(Future successful true)

        val result = controller.fullTaxSummaryForYear(nino, taxYear)(FakeRequest())
        status(result) mustBe OK

        contentAsJson(result) mustBe sparselyPopulatedFullTaxSummaryForYearJson
      }
    }

    "return a minimal tax summary for the given year" when {
      "tax year is CY+1 and the user has no tax codes, incomes, employments or live journeys" in {
        when(taxAccountSummaryService.taxAccountSummary(Matchers.eq(nino),Matchers.eq(taxYear))(any())).thenReturn(
          Future.successful(taxAccountSummaryForYearCY1))
        when(incomeService.taxCodeIncomes(Matchers.eq(nino), Matchers.eq(taxYear))(any())).thenReturn(
          Future successful Seq.empty)
        when(incomeService.incomes(Matchers.eq(nino), Matchers.eq(taxYear))(any())).thenReturn(
          Future successful minimallyPopulatedIncomes)
        when(employmentService.employments(Matchers.eq(nino), Matchers.eq(taxYear))(any())).thenReturn(
          Future successful Seq.empty)
        when(trackingService.isAnyIFormInProgress(Matchers.eq(nino.nino))(any())).thenReturn(Future successful true)

        val result = controller.fullTaxSummaryForYear(nino, taxYear)(FakeRequest())
        status(result) mustBe OK

        contentAsJson(result) mustBe minimalyPopulatedFullTaxSummaryForYearJson
      }
    }

    "return NOT AUTHORISED" when {
      "the user is not logged in" in {
        intercept[MissingBearerToken]{
          await(notLoggedInController.fullTaxSummaryForYear(nino, TaxYear().next)(FakeRequest()))
        }
      }
    }

    "return Locked exception" when {
      "nps throws locked exception" in {
        when(taxAccountSummaryService.taxAccountSummary(Matchers.eq(nino),Matchers.eq(TaxYear()))(any()))
          .thenReturn(Future.failed(new LockedException("Account is locked")))

        intercept[LockedException]{
          await(controller.fullTaxSummaryForYear(nino, TaxYear())(FakeRequest()))
        }
      }
    }
  }

}