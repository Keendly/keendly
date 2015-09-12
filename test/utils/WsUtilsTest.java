package utils;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class WsUtilsTest {

    @Test
    public void testAsFormData(){
        // given
        Map<String, String> input = new HashMap<>();
        input.put("key1", "value1");
        input.put("key2", "value2");

        // when
        String formData = WsUtils.asFormData(input);

        // then
        assertEquals("key1=value1&key2=value2", formData);
    }

    @Test
    public void testAsFormData_emptyForm(){
        // when
        String formData = WsUtils.asFormData(new HashMap<>());

        // then
        assertEquals(StringUtils.EMPTY, formData);
    }
}
