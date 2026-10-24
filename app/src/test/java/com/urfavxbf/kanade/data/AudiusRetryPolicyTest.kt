package com.urfavxbf.kanade.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudiusRetryPolicyTest {
    @Test fun transientStatusOnly() {
        assertTrue(AudiusRetryPolicy.retryable(429, 0))
        assertTrue(AudiusRetryPolicy.retryable(503, 1))
        assertFalse(AudiusRetryPolicy.retryable(500, 2))
        assertFalse(AudiusRetryPolicy.retryable(401, 0))
        assertFalse(AudiusRetryPolicy.retryable(404, 0))
    }
}
