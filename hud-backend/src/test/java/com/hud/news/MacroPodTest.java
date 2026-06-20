package com.hud.news;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MacroPodTest {
    @Test
    void testGettersAndSettersAndEquals() {
        MacroPodMetric metric = new MacroPodMetric("ticker", "label", 1.0, 50.0, 0.5);
        MacroPod pod1 = new MacroPod("id", "title", "sentiment", "edu", "link", List.of(metric));
        MacroPod pod2 = new MacroPod("id", "title", "sentiment", "edu", "link", List.of(metric));
        
        assertEquals(pod1, pod2);
        assertEquals(pod1.hashCode(), pod2.hashCode());
        assertNotNull(pod1.toString());
        
        pod1.setId("id2");
        assertNotEquals(pod1, pod2);
        assertEquals("id2", pod1.getId());
        
        pod1.setTitle("title2");
        assertEquals("title2", pod1.getTitle());
        
        pod1.setSentimentNarrative("narrative2");
        assertEquals("narrative2", pod1.getSentimentNarrative());
        
        pod1.setEducationalDescription("edu2");
        assertEquals("edu2", pod1.getEducationalDescription());
        
        pod1.setLearnMoreLink("link2");
        assertEquals("link2", pod1.getLearnMoreLink());
        
        MacroPod empty = new MacroPod();
        assertNull(empty.getId());
    }
}
