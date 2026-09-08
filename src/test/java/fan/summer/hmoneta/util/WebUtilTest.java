package fan.summer.hmoneta.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WebUtilTest {

    @Test
    void should_split_standard_subdomain() {
        Map<String, String> parts = WebUtil.extractParts("www.example.com");
        assertNotNull(parts);
        assertEquals("example.com", parts.get("host"));
        assertEquals("www", parts.get("sub"));
    }

    @Test
    void should_support_root_domain_with_null_sub() {
        Map<String, String> parts = WebUtil.extractParts("example.com");
        assertNotNull(parts);
        assertEquals("example.com", parts.get("host"));
        assertNull(parts.get("sub"));
    }

    @Test
    void should_support_multi_level_subdomain() {
        Map<String, String> parts = WebUtil.extractParts("a.b.example.com");
        assertNotNull(parts);
        assertEquals("example.com", parts.get("host"));
        assertEquals("a.b", parts.get("sub"));
    }

    @Test
    void should_reject_invalid_formats() {
        assertNull(WebUtil.extractParts("example"));
        assertNull(WebUtil.extractParts("https://example.com"));
        assertNull(WebUtil.extractParts("http://example.com"));
        assertNull(WebUtil.extractParts("-bad.example.com"));
        assertNull(WebUtil.extractParts("bad-.example.com"));
        assertNull(WebUtil.extractParts(null));
        assertNull(WebUtil.extractParts(" "));
    }
}
