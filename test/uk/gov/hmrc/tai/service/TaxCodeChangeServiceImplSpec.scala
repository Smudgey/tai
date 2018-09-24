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

package uk.gov.hmrc.tai.service

import org.joda.time.LocalDate
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsResultException, Json}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.connectors.TaxCodeChangeConnector
import uk.gov.hmrc.tai.model.{TaxCodeHistory, TaxCodeRecord}
import uk.gov.hmrc.tai.model.api.{TaxCodeChange, TaxCodeChangeRecord}
import uk.gov.hmrc.tai.model.des._
import uk.gov.hmrc.tai.model.tai.TaxYear
import uk.gov.hmrc.tai.util.TaxCodeHistoryConstants
import uk.gov.hmrc.time.TaxYearResolver

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Random, Try}

class TaxCodeChangeServiceImplSpec extends PlaySpec with MockitoSugar with TaxCodeHistoryConstants {

  "hasTaxCodeChanged" should {

    "return true" when {

      "the tax code has been operated for single employments" when {

        "there has been a tax code change after Annual Coding" in {
          val newCodeDate = TaxYearResolver.startOfCurrentTaxYear.plusMonths(2)
          val previousCodeDate = TaxYearResolver.startOfCurrentTaxYear
          val testNino = randomNino

          val taxCodeHistory = TaxCodeHistory(
            nino = testNino.withoutSuffix,
            taxCodeRecord = Seq(
              taxCodeRecord(dateOfCalculation = newCodeDate),
              taxCodeRecord(dateOfCalculation = previousCodeDate)
            )
          )

          when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

          Await.result(createService().hasTaxCodeChanged(testNino), 5.seconds) mustEqual true
        }

        "there has been a tax code change after Annual Coding where Annual coding was before start of tax year" in {
          val newCodeDate = TaxYearResolver.startOfCurrentTaxYear.plusMonths(2)
          val previousCodeDate = TaxYearResolver.startOfCurrentTaxYear.minusMonths(1)
          val testNino = randomNino

          val taxCodeHistory = TaxCodeHistory(
            nino = testNino.withoutSuffix,
            taxCodeRecord = Seq(
              taxCodeRecord(dateOfCalculation = newCodeDate),
              taxCodeRecord(taxCode = "1080L", dateOfCalculation = previousCodeDate)
            )
          )

          when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

          Await.result(createService().hasTaxCodeChanged(testNino), 5.seconds) mustEqual true
        }

        "there has been a change in job with a different tax code after Annual Coding" in {
          val newCodeDate = TaxYearResolver.startOfCurrentTaxYear.plusMonths(2)
          val previousCodeDate = TaxYearResolver.startOfCurrentTaxYear
          val testNino = randomNino


          val taxCodeHistory = TaxCodeHistory(
            nino = testNino.withoutSuffix,
            taxCodeRecord = Seq(
              taxCodeRecord(taxCode ="1180L", dateOfCalculation = newCodeDate),
              taxCodeRecord(taxCode = "1080L", dateOfCalculation = previousCodeDate)
            )
          )

          when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

          Await.result(createService().hasTaxCodeChanged(testNino), 5.seconds) mustEqual true
        }

        "there has been more than one daily tax code change in the year" in {
          val newCodeDate = TaxYearResolver.startOfCurrentTaxYear.plusMonths(2)
          val previousCodeDate = TaxYearResolver.startOfCurrentTaxYear.plusMonths(1)
          val annualCodeDate = TaxYearResolver.startOfCurrentTaxYear.minusMonths(1)
          val testNino = randomNino
          

          val taxCodeHistory = TaxCodeHistory(
            nino = testNino.withoutSuffix,
            taxCodeRecord = Seq(
              taxCodeRecord(taxCode ="1180L", dateOfCalculation = newCodeDate),
              taxCodeRecord(taxCode ="1080L", dateOfCalculation = previousCodeDate),
              taxCodeRecord(taxCode ="1000L", dateOfCalculation = annualCodeDate)
            )
          )

          when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

          Await.result(createService().hasTaxCodeChanged(testNino), 5.seconds) mustEqual true
        }
      }

      "the tax code has been operated for multiple employments" when {

        "there has been more than one daily tax code change in the year for 2 employments to 2 employments" in {
          val newCodeDate = TaxYearResolver.startOfCurrentTaxYear.plusMonths(2)
          val previousCodeDate = TaxYearResolver.startOfCurrentTaxYear.plusMonths(1)
          val testNino = randomNino
          val payrollNumber1 = randomInt().toString
          val payrollNumber2 = randomInt().toString


          val taxCodeHistory = TaxCodeHistory(
            nino = testNino.withoutSuffix,
            taxCodeRecord = Seq(
              taxCodeRecord(dateOfCalculation = newCodeDate, payrollNumber = Some(payrollNumber1)),
              taxCodeRecord(dateOfCalculation = newCodeDate, payrollNumber = Some(payrollNumber2), employmentType = Secondary),
              taxCodeRecord(dateOfCalculation = previousCodeDate, payrollNumber = Some(payrollNumber1)),
              taxCodeRecord(dateOfCalculation = previousCodeDate, payrollNumber = Some(payrollNumber2), employmentType = Secondary)
            )
          )

          when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

          Await.result(createService().hasTaxCodeChanged(testNino), 5.seconds) mustEqual true
        }

        "there has been more than one daily tax code change in the year for 1 employment to 2 employments" in {
          val newCodeDate = TaxYearResolver.startOfCurrentTaxYear.plusMonths(2)
          val previousCodeDate = TaxYearResolver.startOfCurrentTaxYear.plusMonths(1)
          val testNino = randomNino
          val payrollNumber1 = randomInt().toString
          val payrollNumber2 = randomInt().toString

          val taxCodeRecords = Seq(
            taxCodeRecord(dateOfCalculation = newCodeDate, payrollNumber = Some(payrollNumber1)),
            taxCodeRecord(dateOfCalculation = newCodeDate, payrollNumber = Some(payrollNumber2), employmentType = Secondary),
            taxCodeRecord(dateOfCalculation = previousCodeDate, payrollNumber = Some(payrollNumber1))
          )

          val taxCodeHistory = TaxCodeHistory(nino = testNino.withoutSuffix, taxCodeRecord = taxCodeRecords)

          when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

          Await.result(createService().hasTaxCodeChanged(testNino), 5.seconds) mustEqual true
        }

        "there has been more than one daily tax code change in the year for 2 employments to 1 employment" in {
          val newCodeDate = TaxYearResolver.startOfCurrentTaxYear.plusMonths(2)
          val previousCodeDate = TaxYearResolver.startOfCurrentTaxYear.plusMonths(1)
          val testNino = randomNino
          val payrollNumber1 = randomInt().toString
          val payrollNumber2 = randomInt().toString

          val taxCodeRecords = Seq(
            taxCodeRecord(dateOfCalculation = newCodeDate, payrollNumber = Some(payrollNumber1)),
            taxCodeRecord(dateOfCalculation = previousCodeDate, payrollNumber = Some(payrollNumber1)),
            taxCodeRecord(dateOfCalculation = previousCodeDate, payrollNumber = Some(payrollNumber2), employmentType = Secondary)
          )

          val taxCodeHistory = TaxCodeHistory(nino = testNino.withoutSuffix, taxCodeRecord = taxCodeRecords)

          when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

          Await.result(createService().hasTaxCodeChanged(testNino), 5.seconds) mustEqual true
        }

        "there has been a tax code change after Annual Coding for 2 employments to 2 employments" in {
          val newCodeDate = TaxYearResolver.startOfCurrentTaxYear.plusMonths(2)
          val previousCodeDate = TaxYearResolver.startOfCurrentTaxYear.plusMonths(1)
          val testNino = randomNino
          val payrollNumber1 = randomInt().toString
          val payrollNumber2 = randomInt().toString

          val taxCodeRecords = Seq(
            taxCodeRecord(dateOfCalculation = newCodeDate, payrollNumber = Some(payrollNumber1)),
            taxCodeRecord(dateOfCalculation = newCodeDate, payrollNumber = Some(payrollNumber2), employmentType = Secondary),
            taxCodeRecord(dateOfCalculation = previousCodeDate, payrollNumber = Some(payrollNumber1)),
            taxCodeRecord(dateOfCalculation = previousCodeDate, payrollNumber = Some(payrollNumber2), employmentType = Secondary)
          )

          val taxCodeHistory = TaxCodeHistory(nino = testNino.withoutSuffix, taxCodeRecord = taxCodeRecords)

          when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

          Await.result(createService().hasTaxCodeChanged(testNino), 5.seconds) mustEqual true
        }
      }
    }

    "return false" when {

      "for single employments" when {

        "this is the first ever employment" in {
          val testNino = randomNino
          val newCodeDate = TaxYearResolver.startOfCurrentTaxYear.plusMonths(2)

          val taxCodeHistory = TaxCodeHistory(
            testNino.withoutSuffix,
            Seq(taxCodeRecord(dateOfCalculation = newCodeDate))
          )

          when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

          Await.result(createService().hasTaxCodeChanged(testNino), 5.seconds) mustEqual false
        }

        "there has been one tax code change in the year but it has not been operated" in {
          val newCodeDate = TaxYearResolver.startOfCurrentTaxYear.plusMonths(2)
          val previousCodeDate = TaxYearResolver.startOfCurrentTaxYear
          val testNino = randomNino

          val taxCodeHistory = TaxCodeHistory(
            nino = testNino.withoutSuffix,
            taxCodeRecord = Seq(
              taxCodeRecord(dateOfCalculation = newCodeDate, operatedTaxCode = false),
              taxCodeRecord(dateOfCalculation = previousCodeDate)
            )
          )

          when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

          Await.result(createService().hasTaxCodeChanged(testNino), 5.seconds) mustEqual false
        }

        "there has not been a tax code change in the year" in {
          val annualCodeDate = TaxYearResolver.startOfCurrentTaxYear
          val testNino = randomNino

          val taxCodeHistory = TaxCodeHistory(
            testNino.withoutSuffix,
            Seq(taxCodeRecord(dateOfCalculation = annualCodeDate))
          )

          when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

          Await.result(createService().hasTaxCodeChanged(testNino), 5.seconds) mustEqual false
        }

        "there has not been a tax code change in the year and annual coding was done before the start of the tax year" in {
          val annualCodeDate = TaxYearResolver.startOfCurrentTaxYear.minusMonths(1)
          val testNino = randomNino

          val taxCodeHistory = TaxCodeHistory(
            testNino.withoutSuffix,
            Seq(taxCodeRecord(dateOfCalculation = annualCodeDate))
          )

          when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

          Await.result(createService().hasTaxCodeChanged(testNino), 5.seconds) mustEqual false
        }
      }

      "Multiple tax code changes have been made prior to the start of the current tax year" in {

        val annualCodingDate = TaxYearResolver.startOfCurrentTaxYear.minusMonths(2)
        val dailyCodingDate = TaxYearResolver.startOfCurrentTaxYear.minusMonths(1)
        val testNino = randomNino

        val taxCodeHistory = TaxCodeHistory(
          testNino.withoutSuffix,
          Seq(
            taxCodeRecord(dateOfCalculation = dailyCodingDate),
            taxCodeRecord(dateOfCalculation = annualCodingDate)
          )
        )

        when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

        val service: TaxCodeChangeServiceImpl = createService()
        Await.result(service.hasTaxCodeChanged(testNino), 5.seconds) mustEqual false
      }

      "for multiple employments" when {

        "this is the first ever employment but 2 employments start at the same time" in {
          val dailyCodeDate = TaxYearResolver.startOfCurrentTaxYear.plusMonths(2)
          val testNino = randomNino

          val taxCodeHistory = TaxCodeHistory(
            testNino.withoutSuffix,
            Seq(
              taxCodeRecord(dateOfCalculation = dailyCodeDate),
              taxCodeRecord(dateOfCalculation = dailyCodeDate, employmentType = Secondary)
            )
          )

          when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

          Await.result(createService().hasTaxCodeChanged(testNino), 5.seconds) mustEqual false
        }

        "there has not been a tax code change in the year (2 employments at Annual Coding)" in {
          val annualCodeDate = TaxYearResolver.startOfCurrentTaxYear
          val testNino = randomNino

          val taxCodeHistory = TaxCodeHistory(
            testNino.withoutSuffix,
            Seq(
              taxCodeRecord(employerName = "Employer A", dateOfCalculation = annualCodeDate),
              taxCodeRecord(employerName = "Employer B", dateOfCalculation = annualCodeDate, employmentType = Secondary)
            )
          )

          when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

          Await.result(createService().hasTaxCodeChanged(testNino), 5.seconds) mustEqual false
        }
      }

      "an empty sequence of TaxCodeRecords is returned in TaxCodeHistory" in {
        val testNino = randomNino

        val taxCodeHistory = TaxCodeHistory(testNino.withoutSuffix, Seq.empty[TaxCodeRecord])

        when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

        Await.result(createService().hasTaxCodeChanged(testNino), 5.seconds) mustEqual false
      }

      "a JSExceptionResult is thrown by the connector" in {
        val testNino = randomNino

        when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.failed(JsResultException(Nil)))

        Await.result(createService().hasTaxCodeChanged(testNino), 5.seconds) mustEqual false
      }
    }
  }

  "taxCodeChange" should {

    "return a TaxCodeChangeRecord for single employments" when {

      "there has been a tax code change in the year after annual coding" in {
        val currentStartDate = TaxYearResolver.startOfCurrentTaxYear.plusDays(2)
        val currentEndDate = TaxYearResolver.endOfCurrentTaxYear
        val previousStartDate = TaxYearResolver.startOfCurrentTaxYear
        val previousEndDate = currentStartDate.minusDays(1)
        val previousStartDateInPrevYear = TaxYearResolver.startOfCurrentTaxYear.minusDays(2)
        val payrollNumberPrev = randomInt().toString
        val payrollNumberCurr = randomInt().toString

        val testNino = randomNino

        val expectedPreviousTaxCodeChange = TaxCodeChangeRecord("1185L", Cumulative, previousStartDate, previousEndDate, "Employer 1", Some(payrollNumberPrev), pensionIndicator = false, primary = true)
        val expectedCurrentTaxCodeChange = TaxCodeChangeRecord("1000L", Cumulative, currentStartDate, currentEndDate, "Employer 1", Some(payrollNumberCurr), pensionIndicator = false, primary = true)

        val previousTaxCodeRecord = TaxCodeRecord("1185L", Cumulative, "Employer 1", operatedTaxCode = true, previousStartDateInPrevYear, Some(payrollNumberPrev), pensionIndicator = false, Primary)
        val currentTaxCodeRecord = TaxCodeRecord("1000L", Cumulative, "Employer 1", operatedTaxCode = true, currentStartDate, Some(payrollNumberCurr), pensionIndicator = false, Primary)

        val taxCodeHistory = TaxCodeHistory(
          testNino.withoutSuffix,
          Seq(previousTaxCodeRecord, currentTaxCodeRecord)
        )

        when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

        val expectedResult = TaxCodeChange(Seq(expectedCurrentTaxCodeChange), Seq(expectedPreviousTaxCodeChange))

        Await.result(createService().taxCodeChange(testNino), 5.seconds) mustEqual expectedResult
      }

      "there has been more than one daily tax code change in the year" in {
        val currentStartDate = TaxYearResolver.startOfCurrentTaxYear.plusDays(2)
        val currentEndDate = TaxYearResolver.endOfCurrentTaxYear
        val previousStartDate = TaxYearResolver.startOfCurrentTaxYear.plusDays(1)
        val previousEndDate = currentStartDate.minusDays(1)
        val payrollNumberPrev = randomInt().toString
        val payrollNumberCurr = randomInt().toString

        val testNino = randomNino

        val expectedPreviousTaxCodeChange = TaxCodeChangeRecord("1185L", Cumulative, previousStartDate, previousEndDate, "Employer 1", Some(payrollNumberPrev), pensionIndicator = false, primary = true)
        val expectedCurrentTaxCodeChange = TaxCodeChangeRecord("1000L", Cumulative, currentStartDate, currentEndDate, "Employer 1", Some(payrollNumberCurr), pensionIndicator = false, primary = true)

        val previousTaxCodeRecord = TaxCodeRecord("1185L", Cumulative, "Employer 1", operatedTaxCode = true, previousStartDate, Some(payrollNumberPrev), pensionIndicator = false, Primary)
        val currentTaxCodeRecord = TaxCodeRecord("1000L", Cumulative, "Employer 1", operatedTaxCode = true, currentStartDate, Some(payrollNumberCurr), pensionIndicator = false, Primary)

        val taxCodeHistory = TaxCodeHistory(
          testNino.withoutSuffix,
          Seq(previousTaxCodeRecord, currentTaxCodeRecord)
        )

        when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

        val expectedResult = TaxCodeChange(Seq(expectedCurrentTaxCodeChange), Seq(expectedPreviousTaxCodeChange))

        Await.result(createService().taxCodeChange(testNino), 5.seconds) mustEqual expectedResult
      }

      "there has been three tax code changes in the year but the most recent tax code is not operated" in {
        val nonOperatedStartDate = TaxYearResolver.startOfCurrentTaxYear.plusDays(5)
        val currentStartDate = nonOperatedStartDate.minusDays(3)
        val previousStartDate = nonOperatedStartDate.minusDays(4)

        val currentEndDate = TaxYearResolver.endOfCurrentTaxYear
        val previousEndDate = currentStartDate.minusDays(1)
        val payrollNumberPrev = randomInt().toString
        val payrollNumberCurr = randomInt().toString
        val payrollNumberNotOp = randomInt().toString

        val testNino = randomNino

        val expectedPreviousTaxCodeChange = TaxCodeChangeRecord("1185L", Cumulative, previousStartDate, previousEndDate, "Employer 1", Some(payrollNumberPrev), pensionIndicator = false, primary = true)
        val expectedCurrentTaxCodeChange = TaxCodeChangeRecord("1000L", Cumulative, currentStartDate, currentEndDate, "Employer 1", Some(payrollNumberCurr), pensionIndicator = false, primary = true)

        val previousTaxCodeRecord = TaxCodeRecord("1185L", Cumulative, "Employer 1", operatedTaxCode = true, previousStartDate, Some(payrollNumberPrev), pensionIndicator = false, Primary)
        val currentTaxCodeRecord = TaxCodeRecord("1000L", Cumulative, "Employer 1", operatedTaxCode = true, currentStartDate, Some(payrollNumberCurr), pensionIndicator = false, Primary)
        val nonOperatedCode = TaxCodeRecord("1185L", Cumulative, "Employer 1", operatedTaxCode = false, nonOperatedStartDate, Some(payrollNumberNotOp), pensionIndicator = false, Primary)

        val taxCodeHistory = TaxCodeHistory(
          testNino.withoutSuffix,
          Seq(nonOperatedCode, previousTaxCodeRecord, currentTaxCodeRecord)
        )

        when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

        val expectedResult = TaxCodeChange(Seq(expectedCurrentTaxCodeChange), Seq(expectedPreviousTaxCodeChange))

        Await.result(createService().taxCodeChange(testNino), 5.seconds) mustEqual expectedResult
      }
    }

    "return a TaxCodeChange for multiple employments" when {

      "there has been a tax code change after Annual Coding for 2 employments to 2 employments" in {
        val currentStartDate = TaxYearResolver.startOfCurrentTaxYear.plusDays(2)
        val currentEndDate = TaxYearResolver.endOfCurrentTaxYear
        val previousStartDate = TaxYearResolver.startOfCurrentTaxYear
        val previousEndDate = currentStartDate.minusDays(1)
        val previousStartDateInPrevYear = TaxYearResolver.startOfCurrentTaxYear.minusDays(2)
        val payrollNumberPrev = randomInt().toString
        val payrollNumberCurr = randomInt().toString

        val testNino = randomNino

        val expectedPreviousTaxCodeChange1 = TaxCodeChangeRecord("1185L", Cumulative, previousStartDate, previousEndDate, "Employer 1", Some(payrollNumberPrev), pensionIndicator = false, primary = true)
        val expectedPreviousTaxCodeChange2 = TaxCodeChangeRecord("BR", Cumulative, previousStartDate, previousEndDate, "Employer 2", Some(payrollNumberPrev), pensionIndicator = false, primary = false)
        val expectedCurrentTaxCodeChange1 = TaxCodeChangeRecord("1000L", Cumulative, currentStartDate, currentEndDate, "Employer 1", Some(payrollNumberCurr), pensionIndicator = false, primary = true)
        val expectedCurrentTaxCodeChange2 = TaxCodeChangeRecord("185L", Cumulative, currentStartDate, currentEndDate, "Employer 2", Some(payrollNumberCurr), pensionIndicator = false, primary = false)

        val previousTaxCodeRecord1 = TaxCodeRecord("1185L", Cumulative, "Employer 1", operatedTaxCode = true, previousStartDateInPrevYear, Some(payrollNumberPrev), pensionIndicator = false, Primary)
        val previousTaxCodeRecord2 = TaxCodeRecord("BR", Cumulative, "Employer 2", operatedTaxCode = true, previousStartDateInPrevYear, Some(payrollNumberPrev), pensionIndicator = false, Secondary)
        val currentTaxCodeRecord1 = TaxCodeRecord("1000L", Cumulative, "Employer 1", operatedTaxCode = true, currentStartDate, Some(payrollNumberCurr), pensionIndicator = false, Primary)
        val currentTaxCodeRecord2 = TaxCodeRecord("185L", Cumulative, "Employer 2", operatedTaxCode = true, currentStartDate, Some(payrollNumberCurr), pensionIndicator = false, Secondary)

        val taxCodeHistory = TaxCodeHistory(
          testNino.withoutSuffix,
          Seq(previousTaxCodeRecord1, previousTaxCodeRecord2, currentTaxCodeRecord1, currentTaxCodeRecord2)
        )

        when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

        val expectedResult = TaxCodeChange(Seq(expectedCurrentTaxCodeChange1, expectedCurrentTaxCodeChange2), Seq(expectedPreviousTaxCodeChange1, expectedPreviousTaxCodeChange2))

        Await.result(createService().taxCodeChange(testNino), 5.seconds) mustEqual expectedResult
      }

      "there has been a tax code change after Annual Coding for 1 employment to 2 employments" in {
        val currentStartDate = TaxYearResolver.startOfCurrentTaxYear.plusDays(2)
        val currentEndDate = TaxYearResolver.endOfCurrentTaxYear
        val previousStartDate = TaxYearResolver.startOfCurrentTaxYear
        val previousEndDate = currentStartDate.minusDays(1)
        val previousStartDateInPrevYear = TaxYearResolver.startOfCurrentTaxYear.minusDays(2)
        val payrollNumberPrev = randomInt().toString
        val payrollNumberCurr = randomInt().toString

        val testNino = randomNino

        val expectedPreviousTaxCodeChange1 = TaxCodeChangeRecord("1185L", Cumulative, previousStartDate, previousEndDate, "Employer 1", Some(payrollNumberPrev), pensionIndicator = false, primary = true)
        val expectedCurrentTaxCodeChange1 = TaxCodeChangeRecord("1000L", Cumulative, currentStartDate, currentEndDate, "Employer 1", Some(payrollNumberCurr), pensionIndicator = false, primary = true)
        val expectedCurrentTaxCodeChange2 = TaxCodeChangeRecord("185L", Cumulative, currentStartDate, currentEndDate, "Employer 2", Some(payrollNumberCurr), pensionIndicator = false, primary = false)

        val previousTaxCodeRecord1 = TaxCodeRecord("1185L", Cumulative, "Employer 1", operatedTaxCode = true, previousStartDateInPrevYear, Some(payrollNumberPrev), pensionIndicator = false, Primary)
        val currentTaxCodeRecord1 = TaxCodeRecord("1000L", Cumulative, "Employer 1", operatedTaxCode = true, currentStartDate, Some(payrollNumberCurr), pensionIndicator = false, Primary)
        val currentTaxCodeRecord2 = TaxCodeRecord("185L", Cumulative, "Employer 2", operatedTaxCode = true, currentStartDate, Some(payrollNumberCurr), pensionIndicator = false, Secondary)

        val taxCodeHistory = TaxCodeHistory(
          testNino.withoutSuffix,
          Seq(previousTaxCodeRecord1, currentTaxCodeRecord1, currentTaxCodeRecord2)
        )

        when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

        val expectedResult = TaxCodeChange(Seq(expectedCurrentTaxCodeChange1, expectedCurrentTaxCodeChange2), Seq(expectedPreviousTaxCodeChange1))

        Await.result(createService().taxCodeChange(testNino), 5.seconds) mustEqual expectedResult
      }

      "there has been a tax code change after Annual Coding for 2 employments to 1 employment" in {
        val currentStartDate = TaxYearResolver.startOfCurrentTaxYear.plusDays(2)
        val currentEndDate = TaxYearResolver.endOfCurrentTaxYear
        val previousStartDate = TaxYearResolver.startOfCurrentTaxYear
        val previousEndDate = currentStartDate.minusDays(1)
        val previousStartDateInPrevYear = TaxYearResolver.startOfCurrentTaxYear.minusDays(2)
        val payrollNumberPrev = randomInt().toString
        val payrollNumberCurr = randomInt().toString

        val testNino = randomNino

        val expectedPreviousTaxCodeChange1 = TaxCodeChangeRecord("1185L", Cumulative, previousStartDate, previousEndDate, "Employer 1", Some(payrollNumberPrev), pensionIndicator = false, primary = true)
        val expectedPreviousTaxCodeChange2 = TaxCodeChangeRecord("BR", Cumulative, previousStartDate, previousEndDate, "Employer 2", Some(payrollNumberPrev), pensionIndicator = false, primary = false)
        val expectedCurrentTaxCodeChange1 = TaxCodeChangeRecord("1185L", Cumulative, currentStartDate, currentEndDate, "Employer 1", Some(payrollNumberCurr), pensionIndicator = false, primary = true)

        val previousTaxCodeRecord1 = TaxCodeRecord("1185L", Cumulative, "Employer 1", operatedTaxCode = true, previousStartDateInPrevYear, Some(payrollNumberPrev), pensionIndicator = false, Primary)
        val previousTaxCodeRecord2 = TaxCodeRecord("BR", Cumulative, "Employer 2", operatedTaxCode = true, previousStartDateInPrevYear, Some(payrollNumberPrev), pensionIndicator = false, Secondary)
        val currentTaxCodeRecord1 = TaxCodeRecord("1185L", Cumulative, "Employer 1", operatedTaxCode = true, currentStartDate, Some(payrollNumberCurr), pensionIndicator = false, Primary)

        val taxCodeHistory = TaxCodeHistory(
          testNino.withoutSuffix,
          Seq(previousTaxCodeRecord1, previousTaxCodeRecord2, currentTaxCodeRecord1)
        )

        when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

        val expectedResult = TaxCodeChange(Seq(expectedCurrentTaxCodeChange1), Seq(expectedPreviousTaxCodeChange1, expectedPreviousTaxCodeChange2))

        Await.result(createService().taxCodeChange(testNino), 5.seconds) mustEqual expectedResult
      }

      "there has been more than one daily tax code change in the year for 2 employments to 2 employments" in {
        val currentStartDate = TaxYearResolver.startOfCurrentTaxYear.plusDays(2)
        val currentEndDate = TaxYearResolver.endOfCurrentTaxYear
        val previousStartDate = TaxYearResolver.startOfCurrentTaxYear
        val previousEndDate = currentStartDate.minusDays(1)
        val previousStartDateInPrevYear = TaxYearResolver.startOfCurrentTaxYear.minusDays(2)
        val payrollNumberPrev = randomInt().toString
        val payrollNumberCurr = randomInt().toString

        val testNino = randomNino

        val expectedPreviousTaxCodeChange1 = TaxCodeChangeRecord("1185L", Cumulative, previousStartDate, previousEndDate, "Employer 1", Some(payrollNumberPrev), pensionIndicator = false, primary = true)
        val expectedPreviousTaxCodeChange2 = TaxCodeChangeRecord("BR", Cumulative, previousStartDate, previousEndDate, "Employer 2", Some(payrollNumberPrev), pensionIndicator = false, primary = false)
        val expectedCurrentTaxCodeChange1 = TaxCodeChangeRecord("1000L", Cumulative, currentStartDate, currentEndDate, "Employer 1", Some(payrollNumberCurr), pensionIndicator = false, primary = true)
        val expectedCurrentTaxCodeChange2 = TaxCodeChangeRecord("185L", Cumulative, currentStartDate, currentEndDate, "Employer 2", Some(payrollNumberCurr), pensionIndicator = false, primary = false)

        val previousTaxCodeRecord1 = TaxCodeRecord("1185L", Cumulative, "Employer 1", operatedTaxCode = true, previousStartDateInPrevYear, Some(payrollNumberPrev), pensionIndicator = false, Primary)
        val previousTaxCodeRecord2 = TaxCodeRecord("BR", Cumulative, "Employer 2", operatedTaxCode = true, previousStartDateInPrevYear, Some(payrollNumberPrev), pensionIndicator = false, Secondary)
        val currentTaxCodeRecord1 = TaxCodeRecord("1000L", Cumulative, "Employer 1", operatedTaxCode = true, currentStartDate, Some(payrollNumberCurr), pensionIndicator = false, Primary)
        val currentTaxCodeRecord2 = TaxCodeRecord("185L", Cumulative, "Employer 2", operatedTaxCode = true, currentStartDate, Some(payrollNumberCurr), pensionIndicator = false, Secondary)

        val taxCodeHistory = TaxCodeHistory(
          testNino.withoutSuffix,
          Seq(previousTaxCodeRecord1, previousTaxCodeRecord2, currentTaxCodeRecord1, currentTaxCodeRecord2)
        )

        when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))


        val expectedResult = TaxCodeChange(Seq(expectedCurrentTaxCodeChange1, expectedCurrentTaxCodeChange2),
          Seq(expectedPreviousTaxCodeChange1, expectedPreviousTaxCodeChange2))

        Await.result(createService().taxCodeChange(testNino), 5.seconds) mustEqual expectedResult
      }

      "there has been more than one daily tax code change in the year for 1 employment to 2 employments" in {
        val currentStartDate = TaxYearResolver.startOfCurrentTaxYear.plusDays(2)
        val currentEndDate = TaxYearResolver.endOfCurrentTaxYear
        val previousStartDate = TaxYearResolver.startOfCurrentTaxYear
        val previousEndDate = currentStartDate.minusDays(1)
        val previousStartDateInPrevYear = TaxYearResolver.startOfCurrentTaxYear.minusDays(2)
        val payrollNumberPrev = randomInt().toString
        val payrollNumberCurr = randomInt().toString

        val testNino = randomNino

        val expectedPreviousTaxCodeChange1 = TaxCodeChangeRecord("1185L", Cumulative, previousStartDate, previousEndDate, "Employer 1", Some(payrollNumberPrev), pensionIndicator = false, primary = true)
        val expectedCurrentTaxCodeChange1 = TaxCodeChangeRecord("1000L", Cumulative, currentStartDate, currentEndDate, "Employer 1", Some(payrollNumberCurr), pensionIndicator = false, primary = true)
        val expectedCurrentTaxCodeChange2 = TaxCodeChangeRecord("185L", Cumulative, currentStartDate, currentEndDate, "Employer 2", Some(payrollNumberCurr), pensionIndicator = false, primary = false)

        val previousTaxCodeRecord1 = TaxCodeRecord("1185L", Cumulative, "Employer 1", operatedTaxCode = true, previousStartDateInPrevYear, Some(payrollNumberPrev), pensionIndicator = false, Primary)
        val currentTaxCodeRecord1 = TaxCodeRecord("1000L", Cumulative, "Employer 1", operatedTaxCode = true, currentStartDate, Some(payrollNumberCurr), pensionIndicator = false, Primary)
        val currentTaxCodeRecord2 = TaxCodeRecord("185L", Cumulative, "Employer 2", operatedTaxCode = true, currentStartDate, Some(payrollNumberCurr), pensionIndicator = false, Secondary)

        val taxCodeHistory = TaxCodeHistory(
          testNino.withoutSuffix,
          Seq(previousTaxCodeRecord1, currentTaxCodeRecord1, currentTaxCodeRecord2)
        )

        when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

        val expectedResult = TaxCodeChange(Seq(expectedCurrentTaxCodeChange1, expectedCurrentTaxCodeChange2), Seq(expectedPreviousTaxCodeChange1))

        Await.result(createService().taxCodeChange(testNino), 5.seconds) mustEqual expectedResult
      }

      "there has been more than one daily tax code change in the year for 2 employments to 1 employment" in {
        val currentStartDate = TaxYearResolver.startOfCurrentTaxYear.plusDays(2)
        val currentEndDate = TaxYearResolver.endOfCurrentTaxYear
        val previousStartDate = TaxYearResolver.startOfCurrentTaxYear
        val previousEndDate = currentStartDate.minusDays(1)
        val previousStartDateInPrevYear = TaxYearResolver.startOfCurrentTaxYear.minusDays(2)
        val payrollNumberPrev = randomInt().toString
        val payrollNumberCurr = randomInt().toString

        val testNino = randomNino

        val expectedPreviousTaxCodeChange1 = TaxCodeChangeRecord("1185L", Cumulative, previousStartDate, previousEndDate, "Employer 1", Some(payrollNumberPrev), pensionIndicator = false, primary = true)
        val expectedPreviousTaxCodeChange2 = TaxCodeChangeRecord("BR", Cumulative, previousStartDate, previousEndDate, "Employer 2", Some(payrollNumberPrev), pensionIndicator = false, primary = false)
        val expectedCurrentTaxCodeChange1 = TaxCodeChangeRecord("1185L", Cumulative, currentStartDate, currentEndDate, "Employer 1", Some(payrollNumberCurr), pensionIndicator = false, primary = true)

        val previousTaxCodeRecord1 = TaxCodeRecord("1185L", Cumulative, "Employer 1", operatedTaxCode = true, previousStartDateInPrevYear, Some(payrollNumberPrev), pensionIndicator = false, Primary)
        val previousTaxCodeRecord2 = TaxCodeRecord("BR", Cumulative, "Employer 2", operatedTaxCode = true, previousStartDateInPrevYear, Some(payrollNumberPrev), pensionIndicator = false, Secondary)
        val currentTaxCodeRecord1 = TaxCodeRecord("1185L", Cumulative, "Employer 1", operatedTaxCode = true, currentStartDate, Some(payrollNumberCurr), pensionIndicator = false, Primary)

        val taxCodeHistory = TaxCodeHistory(
          testNino.withoutSuffix,
          Seq(previousTaxCodeRecord1, previousTaxCodeRecord2, currentTaxCodeRecord1)
        )

        when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

        val expectedResult = TaxCodeChange(Seq(expectedCurrentTaxCodeChange1), Seq(expectedPreviousTaxCodeChange1, expectedPreviousTaxCodeChange2))

        Await.result(createService().taxCodeChange(testNino), 5.seconds) mustEqual expectedResult
      }
    }

    "return an empty TaxCodeChange for single employments" when {

      "there has not been a tax code change in the year" in {
        val previousStartDateInPrevYear = TaxYearResolver.startOfCurrentTaxYear.minusDays(2)
        val payrollNumberPrev = randomInt().toString

        val testNino = randomNino


        val previousTaxCodeRecord = TaxCodeRecord("1185L", Cumulative, "Employer 1", operatedTaxCode = true, previousStartDateInPrevYear, Some(payrollNumberPrev), pensionIndicator = false, Primary)

        val taxCodeHistory = TaxCodeHistory(
          testNino.withoutSuffix,
          Seq(previousTaxCodeRecord)
        )

        when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

        val expectedResult = TaxCodeChange(Seq.empty[TaxCodeChangeRecord], Seq.empty[TaxCodeChangeRecord])

        Await.result(createService().taxCodeChange(testNino), 5.seconds) mustEqual expectedResult
      }

      "this is the first ever employment" in {
        val currentStartDate = TaxYearResolver.startOfCurrentTaxYear.plusMonths(2)
        val payrollNumberCurr = randomInt().toString

        val testNino = randomNino

        val currentTaxCodeRecord = TaxCodeRecord("1185L", Cumulative, "Employer 1", operatedTaxCode = true, currentStartDate, Some(payrollNumberCurr), pensionIndicator = false, Primary)

        val taxCodeHistory = TaxCodeHistory(
          testNino.withoutSuffix,
          Seq(currentTaxCodeRecord)
        )

        when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

        val expectedResult = TaxCodeChange(Seq.empty[TaxCodeChangeRecord], Seq.empty[TaxCodeChangeRecord])

        Await.result(createService().taxCodeChange(testNino), 5.seconds) mustEqual expectedResult
      }
    }

    "return an empty TaxCodeChange for multiple employments" when {

      "there has not been a tax code change in the year" in {
        val previousStartDateInPrevYear = TaxYearResolver.startOfCurrentTaxYear.minusDays(2)
        val payrollNumberPrev = randomInt().toString

        val testNino = randomNino


        val previousTaxCodeRecord1 = TaxCodeRecord("1000L", Cumulative, "Employer 1", operatedTaxCode = true, previousStartDateInPrevYear, Some(payrollNumberPrev), pensionIndicator = false, Primary)
        val previousTaxCodeRecord2 = TaxCodeRecord("185L", Cumulative, "Employer 2", operatedTaxCode = true, previousStartDateInPrevYear, Some(payrollNumberPrev), pensionIndicator = false, Secondary)

        val taxCodeHistory = TaxCodeHistory(
          testNino.withoutSuffix,
          Seq(previousTaxCodeRecord1, previousTaxCodeRecord2)
        )

        when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

        val expectedResult = TaxCodeChange(Seq.empty[TaxCodeChangeRecord], Seq.empty[TaxCodeChangeRecord])

        Await.result(createService().taxCodeChange(testNino), 5.seconds) mustEqual expectedResult
      }

      "this is the first ever employment (2 employments starting together)" in {
        val currentStartDate = TaxYearResolver.startOfCurrentTaxYear.plusMonths(2)
        val payrollNumberCurr1 = randomInt().toString
        val payrollNumberCurr2 = randomInt().toString

        val testNino = randomNino

        val currentTaxCodeRecord1 = TaxCodeRecord("1000L", Cumulative, "Employer 1", operatedTaxCode = true, currentStartDate, Some(payrollNumberCurr1), pensionIndicator = false, Primary)
        val currentTaxCodeRecord2 = TaxCodeRecord("185L", Cumulative, "Employer 2", operatedTaxCode = true, currentStartDate, Some(payrollNumberCurr2), pensionIndicator = false, Secondary)

        val taxCodeHistory = TaxCodeHistory(
          testNino.withoutSuffix,
          Seq(currentTaxCodeRecord1, currentTaxCodeRecord2)
        )

        when(defaultMockConnector.taxCodeHistory(any(), any(), any())).thenReturn(Future.successful(taxCodeHistory))

        val expectedResult = TaxCodeChange(Seq.empty[TaxCodeChangeRecord], Seq.empty[TaxCodeChangeRecord])

        Await.result(createService().taxCodeChange(testNino), 5.seconds) mustEqual expectedResult
      }
    }
  }

  "taxCodeChangeIabds" should {
    "return a list of Iabd for a nino" in {
      val testNino = randomNino
      val taxAccountId = randomInt(3)

      val typeId = randomInt(3)
      val amount = randomInt()
      val employmentId = randomInt()

      val iabdSummaryAllowance = IabdSummary(amount, typeId,Some("Personal Allowance (PA)"), Some(employmentId), None, None)
      val iabdSummaryDeduction = IabdSummary(105, 18,Some("deduction"), Some(2), None, None)
      val connectorResponse = {
        val allowance = Allowance("personal allowance", 8105, 11, List(iabdSummaryAllowance), 8105)


        val deduction = Deduction("personal allowance", 105, 18, List(iabdSummaryDeduction), 105)

        val income = IncomeSource(3,1,1,754, "employmentPayeRef", false, false, false, "incomeSourceName", "1035L", 1,
          None, 0, 0, 0, 0, 0, List(allowance), List(deduction), Json.obj())

        TaxAccountDetails(taxAccountId, "02/08/2018", testNino, false, TaxYear(), randomInt(3), randomInt(3), None, None, randomInt(5), randomInt(1), randomInt(1), List(income))
      }


      when(defaultMockConnector.taxAccountHistory(testNino, taxAccountId)).thenReturn(Future.successful(Try(connectorResponse)))

      val expectedIabds: (Seq[IabdSummary], Seq[IabdSummary]) = (Seq(iabdSummaryAllowance), Seq(iabdSummaryDeduction))


      Await.result(createService().taxCodeChangeIabds(testNino, taxAccountId),5.seconds) mustBe expectedIabds

    }
  }

  private def taxCodeRecord(taxCode: String = "1185L",
                            employerName: String = "Employer 1",
                            operatedTaxCode: Boolean = true,
                            dateOfCalculation: LocalDate,
                            payrollNumber: Option[String] = Some(randomInt().toString),
                            pensionIndicator: Boolean = false,
                            employmentType: String = Primary): TaxCodeRecord = {
    
    TaxCodeRecord(taxCode, Cumulative, employerName, operatedTaxCode, dateOfCalculation, payrollNumber, pensionIndicator, employmentType)
  }

  val defaultMockConnector: TaxCodeChangeConnector = mock[TaxCodeChangeConnector]

  private def createService(mockConnector: TaxCodeChangeConnector = defaultMockConnector) = {
    new TaxCodeChangeServiceImpl(mockConnector)
  }

  private def randomNino: Nino = new Generator(new Random).nextNino

  private def randomInt(maxDigits: Int = 5) = {
    import scala.math.pow
    val random = new Random
    random.nextInt(pow(10, maxDigits).toInt)
  }
}