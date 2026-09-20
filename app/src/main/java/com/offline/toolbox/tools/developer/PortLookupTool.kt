package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class PortInfo(
    val portNumber: Int,
    val serviceName: String,
    val protocol: String,
    val description: String,
    val securityNotes: String
)

data class PortLookupInput(
    val query: String = "80"
)

data class PortLookupOutput(
    val query: String,
    val matchedCount: Int,
    val ports: List<PortInfo>,
    val formattedReport: String,
    val summary: String
)

class PortLookupTool : Tool<PortLookupInput, PortLookupOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "port_lookup_tool",
        name = "IANA Network Port & Protocol Directory",
        description = "Offline database of standard IANA network ports, transport protocols (TCP/UDP), and security risk notes.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("port", "network", "iana", "tcp", "udp", "service", "firewall", "protocol", "security"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Router"
    )

    private val portDatabase = listOf(
        PortInfo(20, "FTP-DATA", "TCP", "File Transfer Protocol (Data Channel)", "Plaintext; vulnerable to packet sniffing"),
        PortInfo(21, "FTP", "TCP", "File Transfer Protocol (Control Channel)", "Cleartext credentials; use SFTP (22) or FTPS instead"),
        PortInfo(22, "SSH / SFTP", "TCP", "Secure Shell & Secure File Transfer", "High brute-force target; disable password root login"),
        PortInfo(23, "TELNET", "TCP", "Unencrypted text communications", "CRITICAL RISK: Cleartext login; replace immediately with SSH"),
        PortInfo(25, "SMTP", "TCP", "Simple Mail Transfer Protocol (MTA)", "Unencrypted relay; use STARTTLS or port 587"),
        PortInfo(53, "DNS", "TCP/UDP", "Domain Name System", "UDP amplification & DNS cache poisoning risks"),
        PortInfo(67, "DHCP-SERVER", "UDP", "Dynamic Host Configuration Protocol (Server)", "Rogue DHCP server spoofing risk"),
        PortInfo(68, "DHCP-CLIENT", "UDP", "Dynamic Host Configuration Protocol (Client)", "Broadcast DHCP discovery"),
        PortInfo(69, "TFTP", "UDP", "Trivial File Transfer Protocol", "No authentication; insecure by design"),
        PortInfo(80, "HTTP", "TCP", "Hypertext Transfer Protocol (Web)", "Plaintext HTTP; redirect to HTTPS (443)"),
        PortInfo(88, "KERBEROS", "TCP/UDP", "Kerberos Network Authentication", "Critical enterprise auth; protect against ticket forging"),
        PortInfo(110, "POP3", "TCP", "Post Office Protocol v3", "Cleartext email retrieval; use POP3S (995)"),
        PortInfo(123, "NTP", "UDP", "Network Time Protocol", "NTP monlist amplification DDoS risk"),
        PortInfo(135, "MS-RPC", "TCP/UDP", "Microsoft Remote Procedure Call", "Frequent exploit target (WannaCry, Blaster)"),
        PortInfo(137, "NETBIOS-NS", "UDP", "NetBIOS Name Service", "Information disclosure on local subnets"),
        PortInfo(138, "NETBIOS-DGM", "UDP", "NetBIOS Datagram Service", "Legacy Windows protocol; block at perimeter"),
        PortInfo(139, "NETBIOS-SSN", "TCP", "NetBIOS Session Service", "SMB over NetBIOS; high lateral movement target"),
        PortInfo(143, "IMAP", "TCP", "Internet Message Access Protocol", "Plaintext email syncing; use IMAPS (993)"),
        PortInfo(161, "SNMP", "UDP", "Simple Network Management Protocol", "SNMP v1/v2c use cleartext community strings"),
        PortInfo(162, "SNMP-TRAP", "UDP", "SNMP Trap Receiver", "System monitoring alarms"),
        PortInfo(389, "LDAP", "TCP/UDP", "Lightweight Directory Access Protocol", "Plaintext directory access; use LDAPS (636)"),
        PortInfo(443, "HTTPS", "TCP", "HTTP over TLS/SSL (Encrypted Web)", "Modern secure standard for web traffic"),
        PortInfo(445, "SMB", "TCP", "Server Message Block (Windows File Sharing)", "Extreme exploit target (EternalBlue, ransomware)"),
        PortInfo(465, "SMTPS", "TCP", "SMTP over SSL/TLS", "Encrypted email submission"),
        PortInfo(514, "SYSLOG", "UDP", "System Logging Protocol", "Unauthenticated UDP logging"),
        PortInfo(587, "SUBMISSION", "TCP", "Email Message Submission (STARTTLS)", "Standard modern SMTP client submission"),
        PortInfo(636, "LDAPS", "TCP", "LDAP over TLS/SSL", "Encrypted Active Directory & directory queries"),
        PortInfo(993, "IMAPS", "TCP", "IMAP over TLS/SSL", "Secure encrypted email synchronization"),
        PortInfo(995, "POP3S", "TCP", "POP3 over TLS/SSL", "Secure encrypted email retrieval"),
        PortInfo(1194, "OPENVPN", "TCP/UDP", "OpenVPN Tunneling", "Secure encrypted VPN tunnel"),
        PortInfo(1433, "MSSQL", "TCP", "Microsoft SQL Server Database", "Frequent target of SQL brute-force attacks"),
        PortInfo(1521, "ORACLE", "TCP", "Oracle Database Listener", "Enterprise database listener"),
        PortInfo(1883, "MQTT", "TCP", "Message Queuing Telemetry Transport", "IoT telemetry; use MQTTS (8883) with TLS"),
        PortInfo(2049, "NFS", "TCP/UDP", "Network File System", "Verify export permissions and IP whitelists"),
        PortInfo(3306, "MYSQL", "TCP", "MySQL / MariaDB Database", "Do not expose directly to public internet"),
        PortInfo(3389, "RDP", "TCP/UDP", "Remote Desktop Protocol (Windows)", "High-risk ransomware & brute force entry vector"),
        PortInfo(5432, "POSTGRESQL", "TCP", "PostgreSQL Relational Database", "Secure SQL database; restrict access via pg_hba.conf"),
        PortInfo(5900, "VNC", "TCP", "Virtual Network Computing (Remote GUI)", "Ensure strong authentication & TLS tunneling"),
        PortInfo(6379, "REDIS", "TCP", "Redis In-Memory Key-Value Store", "CRITICAL: Often deployed with no password; bind to localhost"),
        PortInfo(8080, "HTTP-ALT", "TCP", "HTTP Alternate / Web Proxy / Tomcat", "Common dev web server port"),
        PortInfo(8443, "HTTPS-ALT", "TCP", "HTTPS Alternate / Admin Console", "Common encrypted management web interface"),
        PortInfo(8888, "JUPYTER", "TCP", "Jupyter Notebook Server", "Ensure token/password protection"),
        PortInfo(9090, "PROMETHEUS", "TCP", "Prometheus Metrics Server", "Monitoring metrics; secure access control"),
        PortInfo(9200, "ELASTICSEARCH", "TCP", "Elasticsearch REST API", "Verify security plugin (Shield/SearchGuard) is active"),
        PortInfo(27017, "MONGODB", "TCP", "MongoDB NoSQL Database", "Ensure auth is enabled; avoid binding to 0.0.0.0")
    )

    override suspend fun execute(input: PortLookupInput): ToolResult<PortLookupOutput> {
        val startTime = System.currentTimeMillis()
        val q = input.query.trim()

        if (q.isEmpty()) {
            return ToolResult.Failure("Query cannot be empty. Enter a port number (e.g. 443) or service name (e.g. SSH).")
        }

        val portNum = q.toIntOrNull()
        val matches = if (portNum != null) {
            portDatabase.filter { it.portNumber == portNum }
        } else {
            val qLower = q.lowercase(Locale.ROOT)
            portDatabase.filter {
                it.serviceName.lowercase(Locale.ROOT).contains(qLower) ||
                it.description.lowercase(Locale.ROOT).contains(qLower) ||
                it.protocol.lowercase(Locale.ROOT).contains(qLower)
            }
        }

        val report = buildString {
            appendLine("IANA NETWORK PORT DIRECTORY AUDIT")
            appendLine("--------------------------------------------------")
            appendLine("Query: \"$q\" | Matches Found: ${matches.size}")
            appendLine()
            if (matches.isEmpty()) {
                appendLine("No standard port found matching '$q'.")
                appendLine("Tip: Ports 1-1023 are Well-Known, 1024-49151 are Registered, and 49152-65535 are Dynamic/Ephemeral.")
            } else {
                matches.forEach { p ->
                    appendLine("• Port ${p.portNumber} [${p.serviceName}] (${p.protocol})")
                    appendLine("  Description:    ${p.description}")
                    appendLine("  Security Notes: ${p.securityNotes}")
                    appendLine()
                }
            }
        }

        val summary = if (matches.isNotEmpty()) {
            "Found ${matches.size} port match(es) for '$q' (${matches.first().serviceName})"
        } else {
            "No port match found for '$q'"
        }

        return ToolResult.Success(
            data = PortLookupOutput(
                query = q,
                matchedCount = matches.size,
                ports = matches,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
