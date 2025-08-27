/*
 * Copyright 2025-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ai.mcp.plus.core.context;

import org.springframework.ai.mcp.plus.core.session.McpPlusSession;
import org.springframework.ai.mcp.plus.core.session.impl.DefaultMcpPlusSession;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for McpPlusSession implementations.
 * 
 * @author Spring AI MCP Plus Team
 */
class McpPlusSessionTest {
    
    @Test
    void testDefaultSession() {
        McpPlusSession session = new DefaultMcpPlusSession();
        
        assertNotNull(session.getId());
        assertNotNull(session.getCreatedAt());
        assertNotNull(session.getLastAccessedAt());
        assertEquals(Duration.ofMinutes(30), session.getMaxInactiveInterval());
        assertFalse(session.isExpired());
        assertTrue(session.getAllData().isEmpty());
        assertFalse(session.getUnderlyingMcpServerSession().isPresent());
    }
    
    @Test
    void testSessionWithCustomInterval() {
        Duration customInterval = Duration.ofMinutes(60);
        McpPlusSession session = new DefaultMcpPlusSession(customInterval);
        
        assertEquals(customInterval, session.getMaxInactiveInterval());
    }
    
    @Test
    void testSessionData() {
        McpPlusSession session = new DefaultMcpPlusSession();
        
        // Test setting and getting data
        session.setData("key1", "value1");
        session.setData("key2", 42);
        session.setData("key3", null); // Should be equivalent to remove
        
        assertEquals("value1", session.getData("key1", String.class).orElse(null));
        assertEquals(Integer.valueOf(42), session.getData("key2", Integer.class).orElse(null));
        assertFalse(session.getData("key3", String.class).isPresent());
        
        // Test wrong type
        assertFalse(session.getData("key1", Integer.class).isPresent());
        
        // Test null key
        assertFalse(session.getData(null, String.class).isPresent());
        
        // Test all data
        Map<String, Object> allData = session.getAllData();
        assertEquals(2, allData.size());
        assertTrue(allData.containsKey("key1"));
        assertTrue(allData.containsKey("key2"));
        
        // Test immutable map
        assertThrows(UnsupportedOperationException.class, () -> allData.put("test", "value"));
    }
    
    @Test
    void testSessionDataRemoval() {
        McpPlusSession session = new DefaultMcpPlusSession();
        
        session.setData("key1", "value1");
        session.setData("key2", "value2");
        
        assertEquals(2, session.getAllData().size());
        
        session.removeData("key1");
        assertFalse(session.getData("key1", String.class).isPresent());
        assertEquals(1, session.getAllData().size());
        
        // Test removing null key
        session.removeData(null);
        assertEquals(1, session.getAllData().size());
    }
    
    @Test
    void testSessionTouch() throws InterruptedException {
        McpPlusSession session = new DefaultMcpPlusSession();
        Instant initialLastAccess = session.getLastAccessedAt();
        
        // Wait a bit to ensure time difference
        Thread.sleep(10);
        
        session.touch();
        Instant afterTouch = session.getLastAccessedAt();
        
        assertTrue(afterTouch.isAfter(initialLastAccess));
    }
    
    @Test
    void testSessionExpiration() throws InterruptedException {
        Duration shortInterval = Duration.ofMillis(50);
        McpPlusSession session = new DefaultMcpPlusSession(shortInterval);
        
        assertFalse(session.isExpired());
        
        // Wait for expiration
        Thread.sleep(100);
        
        assertTrue(session.isExpired());
        
        // Accessing expired session should throw exception
        assertThrows(IllegalStateException.class, () -> session.getData("key", String.class));
        assertThrows(IllegalStateException.class, () -> session.setData("key", "value"));
        assertThrows(IllegalStateException.class, () -> session.getAllData());
    }
    
    @Test
    void testSessionInvalidation() {
        McpPlusSession session = new DefaultMcpPlusSession();
        
        session.setData("key1", "value1");
        session.setData("key2", "value2");
        
        assertFalse(session.isExpired());
        assertEquals(2, session.getAllData().size());
        
        session.invalidate();
        
        assertTrue(session.isExpired());
        // After invalidation, data should be cleared but getAllData() should not throw exception
        // Let's verify this by checking that data access throws exception
        assertThrows(IllegalStateException.class, () -> session.getAllData());
        
        // Touch should not work after invalidation
        session.touch();
        assertTrue(session.isExpired());
    }
    
    @Test
    void testSessionThreadSafety() throws InterruptedException {
        McpPlusSession session = new DefaultMcpPlusSession();
        int threadCount = 10;
        int operationsPerThread = 100;
        
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    String key = "thread" + threadId + "_key" + j;
                    String value = "thread" + threadId + "_value" + j;
                    
                    session.setData(key, value);
                    session.getData(key, String.class);
                    session.touch();
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify final state
        assertFalse(session.isExpired());
        assertEquals(threadCount * operationsPerThread, session.getAllData().size());
    }
}
