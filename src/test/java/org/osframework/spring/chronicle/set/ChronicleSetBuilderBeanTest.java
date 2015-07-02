package org.osframework.spring.chronicle.set;

import org.osframework.spring.chronicle.map.ChronicleMapBuilderBean;
import org.springframework.core.io.FileSystemResource;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.Assert.assertNotNull;

/**
 * Unit tests for {@code ChronicleSetBuilderBean}.
 *
 * @author <a href="mailto:dave@osframework.org">Dave Joyce</a>
 * @version 0.0.1
 */
public class ChronicleSetBuilderBeanTest {

    @Test(expectedExceptions = IllegalStateException.class)
    public void testAfterPropertiesSetNoProperties() throws Exception {
        ChronicleSetBuilderBean<Integer> builderBean = new ChronicleSetBuilderBean<>();
        builderBean.afterPropertiesSet();
    }

    @Test
    public void testAfterPropertiesKeyOnly() throws Exception {
        ChronicleSetBuilderBean<Integer> builderBean = new ChronicleSetBuilderBean<>();
        builderBean.setKeyClass(Integer.class);
        builderBean.afterPropertiesSet();
        assertNotNull(builderBean.getObject());
    }

    @Test
    public void testAfterPropertiesWithLockTimeOut() throws Exception {
        ChronicleSetBuilderBean<Integer> builderBean = new ChronicleSetBuilderBean<>();
        builderBean.setKeyClass(Integer.class);
        builderBean.setLockTimeOut("500ms");
        builderBean.afterPropertiesSet();
        assertNotNull(builderBean.getObject());
    }

    @Test
    public void testAfterPropertiesWithPersistedTo() throws Exception {
        ChronicleSetBuilderBean<Integer> builderBean = new ChronicleSetBuilderBean<>();
        builderBean.setKeyClass(Integer.class);

        String tmpDir = System.getProperty("java.io.tmpdir");
        File tempFile = File.createTempFile(tmpDir, "ChronicleSet");
        builderBean.setMaxEntries(100);
        builderBean.setPersistedTo(new FileSystemResource(tempFile));

        builderBean.afterPropertiesSet();
        assertNotNull(builderBean.getObject());
    }

}
