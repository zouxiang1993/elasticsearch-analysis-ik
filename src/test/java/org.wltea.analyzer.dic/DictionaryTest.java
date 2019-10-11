package org.wltea.analyzer.dic;

import org.junit.Test;

import java.util.List;

/**
 * Author: zouxiang
 * Date: 2019/10/11
 * Description: No Description
 */
public class DictionaryTest {

    @Test
    public void testGetRemoteWordsUnprivileged() {
        final String location = "http://localhost:8080/seoEsConfig/get?domain=mall&type=2";
        List<String> words = Dictionary.getRemoteWordsUnprivileged(location);
        for (String word : words){
            System.out.println(word);
        }
    }
}
