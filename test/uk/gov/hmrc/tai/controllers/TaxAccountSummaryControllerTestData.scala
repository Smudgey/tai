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

import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.json.Json.parse
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.model.tai.TaxYear
import uk.gov.hmrc.tai.service.{EmploymentService, IncomeService, TaxAccountSummaryService, TrackingService}

import scala.util.Random

trait TaxAccountSummaryControllerTestData extends MockitoSugar{
  val taxAccountSummaryService: TaxAccountSummaryService = mock[TaxAccountSummaryService]
  val incomeService: IncomeService = mock[IncomeService]
  val employmentService: EmploymentService = mock[EmploymentService]
  val trackingService: TrackingService = mock[TrackingService]

  val taxYear: TaxYear = TaxYear().next
  val nino: Nino = new Generator(new Random).nextNino

  private implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("TEST")))

  val taxAccountSummary = TaxAccountSummary(1111,0, 12.34, 0, 0, 0, 0)
  val taxAccountSummaryForYearCY1 = TaxAccountSummary(2222,1, 56.78, 100.00, 43.22, 200, 100)

  val expectedTaxAccountSummaryForYearJson: JsObject = Json.obj(
    "data" -> Json.obj(
      "totalEstimatedTax" -> 2222,
      "taxFreeAmount" -> 1,
      "totalInYearAdjustmentIntoCY" -> 56.78,
      "totalInYearAdjustment" -> 100.00,
      "totalInYearAdjustmentIntoCYPlusOne" -> 43.22,
      "totalEstimatedIncome" -> 200,
      "taxFreeAllowance" -> 100
    ),
    "links" -> Json.arr())


  val now: LocalDate = LocalDate.now()

  val fullyPopulatedTaxCodeIncomes: Seq[TaxCodeIncome] = Seq(
    TaxCodeIncome(
      EmploymentIncome,
      Some(123),
      20000.00,
      "description",
      "1100L",
      "name",
      Week1Month1BasisOperation,
      Live,
      0,
      0,
      0,
      Some(ManualTelephone),
      Some(now),
      Some(now))
  )

  val sparselyPopulatedTaxCodeIncomes: Seq[TaxCodeIncome] = Seq(
    TaxCodeIncome(
      EmploymentIncome,
      None,
      20000.00,
      "description",
      "1100L",
      "name",
      Week1Month1BasisOperation,
      Live,
      0,
      0,
      0,
      None,
      None,
      None)
  )

  val fullyPopulatedBankAccount = BankAccount(345, Some("account"), Some("sortCode"), Some("bank"), 100, Some("source"), Some(1))

  val fullyPopulatedIncomes: Incomes =
    Incomes(
      fullyPopulatedTaxCodeIncomes,
      NonTaxCodeIncome(
        Some(UntaxedInterest(UntaxedInterestIncome, Some(234), 100, "interest", Seq(fullyPopulatedBankAccount))),
        Seq(OtherNonTaxCodeIncome(Tips, Some(456), 500, "tips"))))

  val minimallyPopulatedIncomes: Incomes = Incomes(fullyPopulatedTaxCodeIncomes, NonTaxCodeIncome(None, Seq.empty))

  val payment = Payment(now, 2000, 3000, 4000, 5000, 6000, 7000, Weekly)
  val adjustment = Adjustment(NationalInsuranceAdjustment, 100)
  val update = EndOfTaxYearUpdate(now, Seq(adjustment))
  val annualAccount = AnnualAccount("key", taxYear, Available, Seq(payment), Seq(update))

  val fullyPopulatedEmployments = Seq(
    Employment(
      "name",
      Some("payrollNumber"),
      now,
      Some(now),
      Seq(annualAccount),
      "taxDistrictNumber",
      "payeNumber",
      999,
      Some(1000),
      hasPayrolledBenefit = true,
      receivingOccupationalPension = true)
  )

  val sparselyPopulatedEmployments = Seq(
    Employment(
      "name",
      None,
      now,
      None,
      Seq.empty,
      "taxDistrictNumber",
      "payeNumber",
      999,
      None,
      hasPayrolledBenefit = true,
      receivingOccupationalPension = true)
  )

  val minimalyPopulatedFullTaxSummaryForYearJson: JsValue =
    parse(
      s"""{
        |  "data": {
        |    "taxCodeIncome": [],
        |    "employments": [],
        |    "taxAccountSummary": {
        |      "totalEstimatedTax": 2222,
        |      "taxFreeAmount": 1,
        |      "totalInYearAdjustmentIntoCY": 56.78,
        |      "totalInYearAdjustment": 100,
        |      "totalInYearAdjustmentIntoCYPlusOne": 43.22,
        |      "totalEstimatedIncome": 200,
        |      "taxFreeAllowance": 100
        |    },
        |    "isAnyFormInProgress": true,
        |    "nonTaxCodeIncome": {
        |      "taxCodeIncomes": [
        |        {
        |          "componentType": "EmploymentIncome",
        |          "employmentId": 123,
        |          "amount": 20000,
        |          "description": "description",
        |          "taxCode": "1100L",
        |          "name": "name",
        |          "basisOperation": "Week1Month1BasisOperation",
        |          "status": "Live",
        |          "inYearAdjustmentIntoCY": 0,
        |          "totalInYearAdjustment": 0,
        |          "inYearAdjustmentIntoCYPlusOne": 0,
        |          "iabdUpdateSource": "ManualTelephone",
        |          "updateNotificationDate": "$now",
        |          "updateActionDate": "$now"
        |        }
        |      ],
        |      "nonTaxCodeIncomes": {
        |        "otherNonTaxCodeIncomes": []
        |      }
        |    }
        |  },
        |  "links": []
        |}""".stripMargin
    )

  val sparselyPopulatedFullTaxSummaryForYearJson: JsValue =
    parse(
      s"""{
        |  "data": {
        |    "taxCodeIncome": [
        |      {
        |        "componentType": "EmploymentIncome",
        |        "amount": 20000,
        |        "description": "description",
        |        "taxCode": "1100L",
        |        "name": "name",
        |        "basisOperation": "Week1Month1BasisOperation",
        |        "status": "Live",
        |        "inYearAdjustmentIntoCY": 0,
        |        "totalInYearAdjustment": 0,
        |        "inYearAdjustmentIntoCYPlusOne": 0
        |      }
        |    ],
        |    "employments": [
        |      {
        |        "name": "name",
        |        "startDate": "$now",
        |        "annualAccounts": [],
        |        "taxDistrictNumber": "taxDistrictNumber",
        |        "payeNumber": "payeNumber",
        |        "sequenceNumber": 999,
        |        "hasPayrolledBenefit": true,
        |        "receivingOccupationalPension": true
        |      }
        |    ],
        |    "taxAccountSummary": {
        |      "totalEstimatedTax": 2222,
        |      "taxFreeAmount": 1,
        |      "totalInYearAdjustmentIntoCY": 56.78,
        |      "totalInYearAdjustment": 100,
        |      "totalInYearAdjustmentIntoCYPlusOne": 43.22,
        |      "totalEstimatedIncome": 200,
        |      "taxFreeAllowance": 100
        |    },
        |    "isAnyFormInProgress": true,
        |    "nonTaxCodeIncome": {
        |      "taxCodeIncomes": [
        |        {
        |          "componentType": "EmploymentIncome",
        |          "employmentId": 123,
        |          "amount": 20000,
        |          "description": "description",
        |          "taxCode": "1100L",
        |          "name": "name",
        |          "basisOperation": "Week1Month1BasisOperation",
        |          "status": "Live",
        |          "inYearAdjustmentIntoCY": 0,
        |          "totalInYearAdjustment": 0,
        |          "inYearAdjustmentIntoCYPlusOne": 0,
        |          "iabdUpdateSource": "ManualTelephone",
        |          "updateNotificationDate": "$now",
        |          "updateActionDate": "$now"
        |        }
        |      ],
        |      "nonTaxCodeIncomes": {
        |        "otherNonTaxCodeIncomes": []
        |      }
        |    }
        |  },
        |  "links": []
        |}""".stripMargin
  )

  val fullyPopulatedFullTaxSummaryForYearJson: JsValue =
    parse(
      s"""
        |{
        |  "data": {
        |    "taxCodeIncome": [
        |      {
        |        "componentType": "EmploymentIncome",
        |        "employmentId": 123,
        |        "amount": 20000,
        |        "description": "description",
        |        "taxCode": "1100L",
        |        "name": "name",
        |        "basisOperation": "Week1Month1BasisOperation",
        |        "status": "Live",
        |        "inYearAdjustmentIntoCY": 0,
        |        "totalInYearAdjustment": 0,
        |        "inYearAdjustmentIntoCYPlusOne": 0,
        |        "iabdUpdateSource": "ManualTelephone",
        |        "updateNotificationDate": "$now",
        |        "updateActionDate": "$now"
        |      }
        |    ],
        |    "employments": [
        |      {
        |        "name": "name",
        |        "payrollNumber": "payrollNumber",
        |        "startDate": "$now",
        |        "endDate": "$now",
        |        "annualAccounts": [
        |          {
        |            "key": "key",
        |            "taxYear": 2019,
        |            "realTimeStatus": "Available",
        |            "payments": [
        |              {
        |                "date": "$now",
        |                "amountYearToDate": 2000,
        |                "taxAmountYearToDate": 3000,
        |                "nationalInsuranceAmountYearToDate": 4000,
        |                "amount": 5000,
        |                "taxAmount": 6000,
        |                "nationalInsuranceAmount": 7000,
        |                "payFrequency": "Weekly"
        |              }
        |            ],
        |            "endOfTaxYearUpdates": [
        |              {
        |                "date": "$now",
        |                "adjustments": [
        |                  {
        |                    "type": "NationalInsuranceAdjustment",
        |                    "amount": 100
        |                  }
        |                ]
        |              }
        |            ]
        |          }
        |        ],
        |        "taxDistrictNumber": "taxDistrictNumber",
        |        "payeNumber": "payeNumber",
        |        "sequenceNumber": 999,
        |        "cessationPay": 1000,
        |        "hasPayrolledBenefit": true,
        |        "receivingOccupationalPension": true
        |      }
        |    ],
        |    "taxAccountSummary": {
        |      "totalEstimatedTax": 2222,
        |      "taxFreeAmount": 1,
        |      "totalInYearAdjustmentIntoCY": 56.78,
        |      "totalInYearAdjustment": 100,
        |      "totalInYearAdjustmentIntoCYPlusOne": 43.22,
        |      "totalEstimatedIncome": 200,
        |      "taxFreeAllowance": 100
        |    },
        |    "isAnyFormInProgress": true,
        |    "nonTaxCodeIncome": {
        |      "taxCodeIncomes": [
        |        {
        |          "componentType": "EmploymentIncome",
        |          "employmentId": 123,
        |          "amount": 20000,
        |          "description": "description",
        |          "taxCode": "1100L",
        |          "name": "name",
        |          "basisOperation": "Week1Month1BasisOperation",
        |          "status": "Live",
        |          "inYearAdjustmentIntoCY": 0,
        |          "totalInYearAdjustment": 0,
        |          "inYearAdjustmentIntoCYPlusOne": 0,
        |          "iabdUpdateSource": "ManualTelephone",
        |          "updateNotificationDate": "$now",
        |          "updateActionDate": "$now"
        |        }
        |      ],
        |      "nonTaxCodeIncomes": {
        |        "untaxedInterest": {
        |          "incomeComponentType": "UntaxedInterestIncome",
        |          "employmentId": 234,
        |          "amount": 100,
        |          "description": "interest",
        |          "bankAccounts": [
        |            {
        |              "id": 345,
        |              "accountNumber": "account",
        |              "sortCode": "sortCode",
        |              "bankName": "bank",
        |              "grossInterest": 100,
        |              "source": "source",
        |              "numberOfAccountHolders": 1
        |            }
        |          ]
        |        },
        |        "otherNonTaxCodeIncomes": [
        |          {
        |            "incomeComponentType": "Tips",
        |            "employmentId": 456,
        |            "amount": 500,
        |            "description": "tips"
        |          }
        |        ]
        |      }
        |    }
        |  },
        |  "links": []
        |}
      """.stripMargin
    )

}
