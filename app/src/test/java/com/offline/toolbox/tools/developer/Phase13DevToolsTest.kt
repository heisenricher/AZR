package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase13DevToolsTest {

    private val sqlAdvisor = SqlIndexAdvisorTool()
    private val terraformTool = TerraformHclValidatorTool()
    private val semverTool = SemverRangeSolverTool()
    private val envLinter = EnvFileSecurityLinterTool()

    // 1. SQL Query Index Advisor Tests
    @Test
    fun testSqlIndexAdvisor_suggestsCompositeBTree() = runTest {
        val query = """
            SELECT u.id, u.email, o.order_date, o.total_amount
            FROM users u
            INNER JOIN orders o ON u.id = o.user_id
            WHERE u.status = 'active'
              AND o.order_date >= '2026-01-01'
            ORDER BY o.order_date DESC;
        """.trimIndent()

        val res = sqlAdvisor.execute(SqlIndexAdvisorInput(query, "SQLITE"))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.suggestedIndexes.isNotEmpty())
        assertTrue(data.detectedTables.contains("users"))
        assertTrue(data.detectedTables.contains("orders"))
        assertTrue(data.whereFilterColumns.contains("status"))
        assertTrue(data.suggestedIndexes.any { it.ddlStatement.contains("CREATE INDEX") })
    }

    @Test
    fun testSqlIndexAdvisor_emptyQueryFails() = runTest {
        val res = sqlAdvisor.execute(SqlIndexAdvisorInput("   ", "SQLITE"))
        assertTrue(res is ToolResult.Failure)
    }

    // 2. Terraform HCL Validator Tests
    @Test
    fun testTerraformHcl_validConfig() = runTest {
        val hcl = """
            terraform {
              required_version = ">= 1.5.0"
              required_providers {
                aws = {
                  source  = "hashicorp/aws"
                  version = "~> 5.0"
                }
              }
            }

            resource "aws_s3_bucket" "test_bucket" {
              bucket = "my-secure-bucket"
            }
        """.trimIndent()

        val res = terraformTool.execute(TerraformHclInput(hcl))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.isValidSyntax)
        assertTrue(data.totalBlocks >= 2)
        assertTrue(data.findings.none { it.severity == "ERROR" })
    }

    @Test
    fun testTerraformHcl_detectsHardcodedSecretAndUnpinnedProvider() = runTest {
        val badHcl = """
            provider "aws" {
              region = "us-east-1"
              access_key = "AKIA1111111111111111"
              secret_key = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
            }
        """.trimIndent()

        val res = terraformTool.execute(TerraformHclInput(badHcl))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.findings.any { it.rule == "HARDCODED_AWS_KEY" || it.message.contains("AWS") })
    }

    // 3. SemVer Range Solver Tests
    @Test
    fun testSemverRangeSolver_caretAndTildeMatching() = runTest {
        val input = SemverRangeInput(
            rangeExpression = "^1.2.0",
            candidateVersions = "1.1.0, 1.2.0, 1.2.4, 1.3.0, 2.0.0"
        )

        val res = semverTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("1.3.0", data.bestResolvedVersion)
        assertEquals("1.2.0", data.lowestResolvedVersion)
        assertTrue(data.satisfiedVersions.contains("1.2.4"))
        assertFalse(data.satisfiedVersions.contains("2.0.0"))
        assertFalse(data.satisfiedVersions.contains("1.1.0"))
    }

    @Test
    fun testSemverRangeSolver_orLogicalRange() = runTest {
        val input = SemverRangeInput(
            rangeExpression = "^1.0.0 || >=3.0.0",
            candidateVersions = "1.5.0, 2.1.0, 3.2.1"
        )

        val res = semverTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("3.2.1", data.bestResolvedVersion)
        assertTrue(data.satisfiedVersions.contains("1.5.0"))
        assertFalse(data.satisfiedVersions.contains("2.1.0"))
    }

    // 4. Dotenv Security Linter Tests
    @Test
    fun testEnvFileLinter_detectsSecretsAndDuplicates() = runTest {
        val env = """
            PORT=8080
            DATABASE_URL=postgres://root:s3cr3tPassword@localhost:5432/app
            AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
            PORT=3000
        """.trimIndent()

        val res = envLinter.execute(EnvFileLinterInput(env))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.criticalLeakCount >= 1)
        assertTrue(data.duplicateKeys.contains("PORT"))
        assertTrue(data.sanitizedExampleContent.contains("PORT="))
        assertTrue(data.sanitizedExampleContent.contains("your_"))
    }

    @Test
    fun testEnvFileLinter_emptyInputFails() = runTest {
        val res = envLinter.execute(EnvFileLinterInput("   "))
        assertTrue(res is ToolResult.Failure)
    }
}
