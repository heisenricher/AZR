package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

enum class ConstantCategory(val displayName: String) {
    ALL("All Constants"),
    UNIVERSAL("Universal"),
    ELECTROMAGNETIC("Electromagnetic"),
    ATOMIC("Atomic & Nuclear"),
    PHYSICO_CHEMICAL("Physico-Chemical"),
    ASTRONOMICAL("Astronomical")
}

data class PhysicalConstant(
    val name: String,
    val symbol: String,
    val valueStr: String,
    val unit: String,
    val category: ConstantCategory,
    val description: String
)

data class ConstantsInput(
    val query: String = "",
    val category: ConstantCategory = ConstantCategory.ALL
)

data class ConstantsOutput(
    val totalMatched: Int,
    val constants: List<PhysicalConstant>,
    val formattedReport: String,
    val summary: String
)

class ScientificConstantsTool : Tool<ConstantsInput, ConstantsOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "scientific_constants_tool",
        name = "CODATA Fundamental Scientific Constants",
        description = "Offline reference directory of fundamental physical, chemical, electromagnetic, and astronomical constants with SI units.",
        category = ToolCategory.MATH,
        tags = listOf("constant", "physics", "codata", "planck", "speed of light", "gravity", "avogadro", "boltzmann", "astronomy", "science"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Science"
    )

    private val constantsList = listOf(
        PhysicalConstant("Speed of Light in Vacuum", "c", "299,792,458", "m s⁻¹", ConstantCategory.UNIVERSAL, "Exact speed of electromagnetic radiation in free space"),
        PhysicalConstant("Planck Constant", "h", "6.62607015 × 10⁻³⁴", "J s", ConstantCategory.UNIVERSAL, "Fundamental quantum of action"),
        PhysicalConstant("Reduced Planck Constant", "ħ", "1.054571817 × 10⁻³⁴", "J s", ConstantCategory.UNIVERSAL, "Dirac constant h / (2π)"),
        PhysicalConstant("Newtonian Constant of Gravitation", "G", "6.67430 × 10⁻¹¹", "m³ kg⁻¹ s⁻²", ConstantCategory.UNIVERSAL, "Empirical gravitational constant"),
        PhysicalConstant("Elementary Charge", "e", "1.602176634 × 10⁻¹⁹", "C", ConstantCategory.ELECTROMAGNETIC, "Electric charge carried by a single proton"),
        PhysicalConstant("Magnetic Permittivity of Vacuum", "ε₀", "8.8541878128 × 10⁻¹²", "F m⁻¹", ConstantCategory.ELECTROMAGNETIC, "Electric constant"),
        PhysicalConstant("Magnetic Permeability of Vacuum", "μ₀", "1.25663706212 × 10⁻⁶", "N A⁻²", ConstantCategory.ELECTROMAGNETIC, "Magnetic constant"),
        PhysicalConstant("Characteristic Impedance of Vacuum", "Z₀", "376.730313668", "Ω", ConstantCategory.ELECTROMAGNETIC, "Wave impedance in free space"),
        PhysicalConstant("Coulomb Constant", "k_e", "8.9875517923 × 10⁹", "N m² C⁻²", ConstantCategory.ELECTROMAGNETIC, "Electrostatic force constant 1/(4πε₀)"),
        PhysicalConstant("Electron Mass", "m_e", "9.1093837015 × 10⁻³¹", "kg", ConstantCategory.ATOMIC, "Rest mass of the electron"),
        PhysicalConstant("Proton Mass", "m_p", "1.67262192369 × 10⁻²⁷", "kg", ConstantCategory.ATOMIC, "Rest mass of the proton"),
        PhysicalConstant("Neutron Mass", "m_n", "1.67492749804 × 10⁻²⁷", "kg", ConstantCategory.ATOMIC, "Rest mass of the neutron"),
        PhysicalConstant("Bohr Radius", "a₀", "5.29177210903 × 10⁻¹¹", "m", ConstantCategory.ATOMIC, "Most probable distance between nucleus and electron in hydrogen"),
        PhysicalConstant("Rydberg Constant", "R_∞", "10,973,731.568160", "m⁻¹", ConstantCategory.ATOMIC, "Limiting value of highest wavenumber of photon emitted from atom"),
        PhysicalConstant("Avogadro Constant", "N_A", "6.02214076 × 10²³", "mol⁻¹", ConstantCategory.PHYSICO_CHEMICAL, "Number of constituent particles in one mole"),
        PhysicalConstant("Boltzmann Constant", "k_B", "1.380649 × 10⁻²³", "J K⁻¹", ConstantCategory.PHYSICO_CHEMICAL, "Relates average relative kinetic energy of gas particles with temperature"),
        PhysicalConstant("Molar Gas Constant", "R", "8.314462618", "J mol⁻¹ K⁻¹", ConstantCategory.PHYSICO_CHEMICAL, "Universal gas constant N_A * k_B"),
        PhysicalConstant("Faraday Constant", "F", "96,485.33212", "C mol⁻¹", ConstantCategory.PHYSICO_CHEMICAL, "Magnitude of electric charge per mole of electrons"),
        PhysicalConstant("Stefan-Boltzmann Constant", "σ", "5.670374419 × 10⁻⁸", "W m⁻² K⁻⁴", ConstantCategory.PHYSICO_CHEMICAL, "Constant of proportionality in blackbody radiation law"),
        PhysicalConstant("Astronomical Unit", "au", "149,597,870,700", "m", ConstantCategory.ASTRONOMICAL, "Mean distance from the Earth to the Sun"),
        PhysicalConstant("Light Year", "ly", "9.4607304725808 × 10¹⁵", "m", ConstantCategory.ASTRONOMICAL, "Distance that light travels in vacuum in one Julian year"),
        PhysicalConstant("Parsec", "pc", "3.08567758149137 × 10¹⁶", "m", ConstantCategory.ASTRONOMICAL, "Distance at which 1 AU subtends an angle of one arcsecond"),
        PhysicalConstant("Standard Gravitational Acceleration", "g₀", "9.80665", "m s⁻²", ConstantCategory.ASTRONOMICAL, "Nominal gravitational acceleration on Earth surface")
    )

    override suspend fun execute(input: ConstantsInput): ToolResult<ConstantsOutput> {
        val startTime = System.currentTimeMillis()
        val q = input.query.trim().lowercase(Locale.ROOT)

        val filtered = constantsList.filter { c ->
            (input.category == ConstantCategory.ALL || c.category == input.category) &&
                (q.isEmpty() || c.name.lowercase(Locale.ROOT).contains(q) || c.symbol.lowercase(Locale.ROOT).contains(q) || c.description.lowercase(Locale.ROOT).contains(q))
        }

        val report = buildString {
            appendLine("FUNDAMENTAL SCIENTIFIC CONSTANTS (CODATA)")
            appendLine("--------------------------------------------------")
            appendLine("Filter Category:     ${input.category.displayName}")
            if (q.isNotEmpty()) appendLine("Search Filter:       \"$q\"")
            appendLine("Constants Matched:   ${filtered.size} of ${constantsList.size}")
            appendLine()
            filtered.forEach { c ->
                appendLine("• ${c.name} (${c.symbol}):")
                appendLine("    Value:       ${c.valueStr} ${c.unit}")
                appendLine("    Category:    ${c.category.displayName}")
                appendLine("    Description: ${c.description}")
            }
        }

        val summary = "${filtered.size} constant(s) matching \"${if (q.isNotEmpty()) q else input.category.name}\""

        return ToolResult.Success(
            data = ConstantsOutput(
                totalMatched = filtered.size,
                constants = filtered,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
