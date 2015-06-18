package org.osframework.spring.chronicle.map;

import org.springframework.core.io.FileSystemResource;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.Assert.assertNotNull;

/**
 * Unit tests for {@code ChronicleMapBuilderBean}.
 *
 * @author <a href="mailto:dave@osframework.org">Dave Joyce</a>
 * @version 0.0.1
 * @since 2015-06-17
 */
public class ChronicleMapBuilderBeanTest {

    @Test(expectedExceptions = IllegalStateException.class)
    public void testAfterPropertiesSetNoProperties() throws Exception {
        ChronicleMapBuilderBean<Integer, String> builderBean = new ChronicleMapBuilderBean<>();
        builderBean.afterPropertiesSet();
    }

    @Test
    public void testAfterPropertiesKeyAndValueOnly() throws Exception {
        ChronicleMapBuilderBean<Integer, String> builderBean = new ChronicleMapBuilderBean<>();
        builderBean.setKeyClass(Integer.class);
        builderBean.setValueClass(String.class);
        builderBean.afterPropertiesSet();
        assertNotNull(builderBean.getObject());
    }

    @Test
    public void testAfterPropertiesWithLockTimeOut() throws Exception {
        ChronicleMapBuilderBean<Integer, String> builderBean = new ChronicleMapBuilderBean<>();
        builderBean.setKeyClass(Integer.class);
        builderBean.setValueClass(String.class);
        builderBean.setLockTimeOut("500ms");
        builderBean.afterPropertiesSet();
        assertNotNull(builderBean.getObject());
    }

    @Test
    public void testAfterPropertiesWithPersistedTo() throws Exception {
        ChronicleMapBuilderBean<Integer, String> builderBean = new ChronicleMapBuilderBean<>();
        builderBean.setKeyClass(Integer.class);
        builderBean.setValueClass(String.class);

        String tmpDir = System.getProperty("java.io.tmpdir");
        File tempFile = File.createTempFile(tmpDir, "ChronicleMap");
        builderBean.setPersistedTo(new FileSystemResource(tempFile));

        builderBean.afterPropertiesSet();
        assertNotNull(builderBean.getObject());
    }

}
