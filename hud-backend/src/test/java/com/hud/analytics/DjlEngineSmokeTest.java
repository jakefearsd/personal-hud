package com.hud.analytics;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DjlEngineSmokeTest {

    @Test
    void shouldInitializeDjlAndPerformBasicOperation() {
        try (NDManager manager = NDManager.newBaseManager()) {
            assertNotNull(manager, "NDManager should be initialized");
            
            NDArray array = manager.create(new float[]{1.0f, 2.0f, 3.0f});
            assertNotNull(array);
            assertEquals(3, array.size());
            
            NDArray result = array.add(1.0f);
            assertEquals(2.0f, result.getFloat(0));
        }
    }
}
