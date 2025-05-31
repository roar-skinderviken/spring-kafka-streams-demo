package no.roar.kafka.logging

import org.slf4j.MDC

/**
 * Execute [body] with the given [pairs] as MDC metadata.
 *
 * @param T The return type of [body].
 * @param pairs MDC metadata to be added to the MDC context.
 * @param body The code to be executed.
 * @return The result of [body].
 * @throws Any exception thrown by [body] with the MDC metadata added to the exception.
 */
inline fun <T> withLoggingContext(
    vararg pairs: Pair<String, String?>,
    body: () -> T
): T = pairs
    .filter { it.second != null }
    .let { pairsForMDC ->
        try {
            pairsForMDC.forEach { MDC.put(it.first, it.second) }
            body()
        } catch (thrown: Throwable) {
            thrown.addSuppressed(MdcMetadataException(MDC.getCopyOfContextMap().toString()))
            throw thrown
        } finally {
            pairsForMDC.forEach { MDC.remove(it.first) }
        }
    }
