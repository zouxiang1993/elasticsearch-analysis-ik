package org.elasticsearch.plugin.analysis.ik;

import org.apache.commons.io.IOUtils;
import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeRequest;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.wltea.analyzer.dic.Dictionary;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;

/**
 * Author: zouxiang
 * Date: 2019/10/19
 * Description: No Description
 */
public class AnalysisIkPluginTest {
    private ElasticsearchClusterRunner runner;

    @Before
    public void setUp() {
        // create runner instance
        runner = new ElasticsearchClusterRunner();
        // create ES nodes
        runner.build(newConfigs()
                .numOfNode(1) // Create a test node, default number of node is 3.
                .pluginTypes("org.elasticsearch.plugin.analysis.ik.AnalysisIkPlugin")
        );

        try{
            Field basePathField = ElasticsearchClusterRunner.class.getDeclaredField("basePath");
            basePathField.setAccessible(true);
            String basePath = (String) basePathField.get(runner);
            System.out.println("配置路径为: " + basePath);
            Path targetPath = Paths.get(basePath).resolve("node_1").resolve("config").resolve("analysis-ik");
            File targetDir = new File(targetPath.toUri());
            targetDir.mkdirs();

            String dir=System.getProperty("user.dir");
            File configDir = new File(Paths.get(dir).resolve("config").toUri());
            for (File file : configDir.listFiles()) {
                Files.copy(Paths.get(file.toURI()),
                        targetPath.resolve(file.getName()), StandardCopyOption.REPLACE_EXISTING);
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() throws IOException {
        // close runner
        runner.close();
        // delete all files
        runner.clean();
    }


    @Test
    public void test1() throws Exception {
        createIndex("test");
        analyzer("test");

        Thread.sleep(1000000000); // 测试动态获取远程词典的功能。
    }

    private synchronized void analyzer(String indexName) {
        List<AnalyzeResponse.AnalyzeToken> tokens = tokens(indexName, "白色的耐克鞋");
        for (AnalyzeResponse.AnalyzeToken token : tokens) {
            System.out.println(token.getTerm() + " => " + token.getType());
        }

        tokens = tokens(indexName, "鞋");
        for (AnalyzeResponse.AnalyzeToken token : tokens) {
            System.out.println(token.getTerm() + " => " + token.getType());
        }
        System.out.println();
    }

    private List<AnalyzeResponse.AnalyzeToken> tokens(String indexName, String text) {
        AnalyzeRequest analyzeRequest = new AnalyzeRequest(indexName);
        analyzeRequest.text(text);
        analyzeRequest.analyzer("ik_max_word");
        ActionFuture<AnalyzeResponse> actionFuture = runner.admin().indices().analyze(analyzeRequest);
        AnalyzeResponse response = actionFuture.actionGet(10L, TimeUnit.SECONDS);
        return response.getTokens();
    }

    private void createIndex(String indexName) throws IOException {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("remoteSynonymIndexSetting.json");
        StringWriter writer = new StringWriter();
        IOUtils.copy(is, writer);
        String indexSettings = writer.toString();
        runner.createIndex(indexName, Settings.builder().loadFromSource(indexSettings, XContentType.JSON).build());
        // wait for yellow status
        runner.ensureYellow();
    }
}
