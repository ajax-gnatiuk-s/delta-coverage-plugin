package io.github.surpsg.deltacoverage.report.intellij.verifier

import com.intellij.rt.coverage.verify.TargetProcessor
import com.intellij.rt.coverage.verify.Verifier
import io.github.surpsg.deltacoverage.config.CoverageEntity
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal

internal class CoverageViolationsCollector(
    private val rule: CoverageRuleWithThreshold
) : TargetProcessor.Consumer {

    private val foundViolations: MutableList<CoverageVerifier.Violation> = mutableListOf()

    val violations: List<CoverageVerifier.Violation> = foundViolations

    override fun consume(name: String, coverage: Verifier.CollectedCoverage) {
        val counter: Verifier.CollectedCoverage.Counter = rule.coverageEntity.toVerifierType().getCounter(coverage)

        val violationResolveContext: ViolationResolveContext = buildViolationResolveContext(
            counter,
            rule.coverageEntity
        )
        if (violationResolveContext.isIgnoredByThreshold()) {
            log.info(
                "Coverage violation of {} was ignored because threshold={} but total={}",
                violationResolveContext.coverageEntity,
                violationResolveContext.thresholdCount,
                violationResolveContext.totalCount
            )
        } else {
            val actualValue: BigDecimal = rule.valueType.getValue(counter) ?: 0.0.toBigDecimal()
            foundViolations += CoverageVerifier.Violation(
                coverageTrackType = rule.coverageEntity.name,
                expectedMinValue = rule.min.toDouble(),
                actualValue = actualValue.toDouble()
            )
        }
    }

    private fun buildViolationResolveContext(
        collectedCoverageCounter: Verifier.CollectedCoverage.Counter,
        coverageEntity: CoverageEntity,
    ): ViolationResolveContext {
        return rule.threshold
            ?.let { threshold ->
                ViolationResolveContext(
                    coverageEntity,
                    threshold,
                    collectedCoverageCounter.calculateTotal()
                )
            }
            ?: ViolationResolveContext.NO_IGNORE_VIOLATION_CONTEXT
    }

    private fun Verifier.CollectedCoverage.Counter.calculateTotal(): Long = missed + covered

    private fun CoverageEntity.toVerifierType(): Verifier.Counter = when (this) {
        CoverageEntity.INSTRUCTION -> Verifier.Counter.INSTRUCTION
        CoverageEntity.BRANCH -> Verifier.Counter.BRANCH
        CoverageEntity.LINE -> Verifier.Counter.LINE
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(CoverageViolationsCollector::class.java)
    }
}
