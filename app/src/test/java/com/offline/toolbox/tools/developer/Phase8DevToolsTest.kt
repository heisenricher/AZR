package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase8DevToolsTest {

    private val supernetTool = SubnetSupernetCalculatorTool()
    private val dnsTool = DnsRecordParserTool()
    private val dockerTool = DockerComposeValidatorTool()
    private val sqlDialectTool = SqlDialectConverterTool()

    @Test
    fun testSupernet_aggregatesContiguousSubnets() = runTest {
        val input = """
            192.168.0.0/24
            192.168.1.0/24
            192.168.2.0/24
            192.168.3.0/24
        """.trimIndent()
        val result = supernetTool.execute(SubnetSupernetInput(subnetList = input))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(4, data.totalSubnetsInput)
        assertEquals(1, data.summarizedCidrs.size)
        assertEquals("192.168.0.0/22", data.summarizedCidrs[0])
        assertEquals(1024L, data.inputTotalAddresses)
        assertEquals(100.0, data.utilizationPercentage, 0.01)
    }

    @Test
    fun testSupernet_invalidCidrFails() = runTest {
        val result = supernetTool.execute(SubnetSupernetInput(subnetList = "invalid_subnet/99"))
        assertTrue(result is ToolResult.Failure)
    }

    @Test
    fun testDnsParser_extractsResourceRecords() = runTest {
        val zone = """
            example.com.   3600  IN  A      93.184.216.34
            example.com.   3600  IN  AAAA   2606:2800:220:1:248:1893:25c8:1946
            example.com.   1800  IN  MX     10 mail.example.com.
        """.trimIndent()
        val result = dnsTool.execute(DnsParserInput(zoneContent = zone))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(3, data.totalRecords)
        assertEquals(3, data.recordTypeCounts.size)
        assertEquals(1, data.recordTypeCounts["A"])
        assertEquals(1, data.recordTypeCounts["AAAA"])
        assertEquals(1, data.recordTypeCounts["MX"])
    }

    @Test
    fun testDockerCompose_detectsPortConflicts() = runTest {
        val yaml = """
            version: '3'
            services:
              web1:
                image: nginx
                ports:
                  - "8080:80"
              web2:
                image: apache
                ports:
                  - "8080:80"
        """.trimIndent()
        val result = dockerTool.execute(DockerComposeInput(yamlContent = yaml))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(2, data.serviceCount)
        assertEquals(1, data.portConflicts.size)
        assertTrue(data.portConflicts[0].contains("8080"))
    }

    @Test
    fun testSqlDialect_postgresToMysql() = runTest {
        val sql = "CREATE TABLE test (id SERIAL PRIMARY KEY, active BOOLEAN);"
        val result = sqlDialectTool.execute(SqlDialectInput(
            sqlContent = sql,
            sourceDialect = SqlDialect.POSTGRESQL,
            targetDialect = SqlDialect.MYSQL
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue(data.convertedSql.contains("INT AUTO_INCREMENT PRIMARY KEY"))
        assertTrue(data.convertedSql.contains("TINYINT(1)"))
    }
}
