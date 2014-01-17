package com.openexchange.server.services;

import java.net.URI;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import com.openexchange.configuration.ServerConfig;
import com.openexchange.configuration.ServerConfig.Property;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.attach.AttachmentConfig;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextImpl;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.filestore.FilestoreStorage;
import com.openexchange.groupware.infostore.InfostoreConfig;
import com.openexchange.jslob.JSlob;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.mail.usersetting.UserSettingMailStorage;
import com.openexchange.session.Session;
import com.openexchange.tools.file.QuotaFileStorage;


/**
 * Unit tests for {@link SharedInfostoreJSlobTest}
 * 
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since 7.4.2
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
    InfostoreConfig.class, ServerConfig.class, AttachmentConfig.class, ContextStorage.class, QuotaFileStorage.class,
    UserSettingMailStorage.class, UserSettingMail.class, FilestoreStorage.class })
public class SharedInfostoreJSlobTest {

    @InjectMocks
    private SharedInfostoreJSlob sharedInfostoreJSlob;

    @Mock
    private Session session;

    @Mock
    private QuotaFileStorage quotaFileStorage;

    private Context context = new ContextImpl(999999);

    private int maxBodySize = 11111;

    private Long infostoreMaxUploadSize = 22222L;

    private Long attachmentMaxUploadSize = 33333L;

    private Long quotaUsage = 44444L;

    private Long maxQuota = 55555L;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        PowerMockito.mockStatic(ServerConfig.class);
        PowerMockito.when(ServerConfig.getInt((Property) Matchers.any())).thenReturn(this.maxBodySize);

        PowerMockito.mockStatic(InfostoreConfig.class);
        PowerMockito.when(InfostoreConfig.getMaxUploadSize()).thenReturn(infostoreMaxUploadSize);

        PowerMockito.mockStatic(AttachmentConfig.class);
        PowerMockito.when(AttachmentConfig.getMaxUploadSize()).thenReturn(attachmentMaxUploadSize);

        PowerMockito.mockStatic(FilestoreStorage.class);
        PowerMockito.when(FilestoreStorage.createURI(Matchers.eq(context))).thenReturn(new URI(""));

        PowerMockito.mockStatic(ContextStorage.class);
        PowerMockito.when(ContextStorage.getStorageContext(session)).thenReturn(context);

        PowerMockito.mockStatic(QuotaFileStorage.class);
        PowerMockito.when(QuotaFileStorage.getInstance((URI) Matchers.any(), Matchers.eq(context))).thenReturn(quotaFileStorage);
        PowerMockito.when(quotaFileStorage.getQuota()).thenReturn(maxQuota);
        PowerMockito.when(quotaFileStorage.getUsage()).thenReturn(quotaUsage);

        PowerMockito.mockStatic(UserSettingMailStorage.class);
        UserSettingMailStorage userSettingMailStorage = Mockito.mock(UserSettingMailStorage.class);
        PowerMockito.when(UserSettingMailStorage.getInstance()).thenReturn(userSettingMailStorage);

        UserSettingMail userSettingMail = Mockito.mock(UserSettingMail.class);
        PowerMockito.when(userSettingMailStorage.getUserSettingMail(Matchers.anyInt(), Matchers.eq(Matchers.eq(context)))).thenReturn(
            userSettingMail);
    }

    @Test
    public void testGetJSlob_fine_maxBodySizeSet() throws OXException, JSONException {
        JSlob jSlob = sharedInfostoreJSlob.getJSlob(session);

        Assert.assertEquals(this.maxBodySize, jSlob.getJsonObject().get("maxBodySize"));
    }

    @Test
    public void testGetJSlob_fine_infostoreMaxUploadSizeSet() throws OXException, JSONException {
        JSlob jSlob = sharedInfostoreJSlob.getJSlob(session);

        Assert.assertEquals(this.infostoreMaxUploadSize, jSlob.getJsonObject().get("infostoreMaxUploadSize"));
    }

    @Test
    public void testGetJSlob_fine_attachmentMaxUploadSizeSet() throws OXException, JSONException {
        JSlob jSlob = sharedInfostoreJSlob.getJSlob(session);

        Assert.assertEquals(this.attachmentMaxUploadSize, jSlob.getJsonObject().get("attachmentMaxUploadSize"));
    }

    @Test
    public void testGetJSlob_fine_infostoreQuotaSet() throws OXException, JSONException {
        JSlob jSlob = sharedInfostoreJSlob.getJSlob(session);

        Assert.assertEquals(this.maxQuota, jSlob.getJsonObject().get("infostoreQuota"));
    }

    @Test
    public void testGetJSlob_fine_infostoreUsageSet() throws OXException, JSONException {
        JSlob jSlob = sharedInfostoreJSlob.getJSlob(session);

        Assert.assertEquals(this.quotaUsage, jSlob.getJsonObject().get("infostoreUsage"));
    }

    @Test
    public void testGetJSlob_fine_attachmentQuotaSet() throws OXException, JSONException {
        JSlob jSlob = sharedInfostoreJSlob.getJSlob(session);

        Assert.assertEquals(0L, jSlob.getJsonObject().get("attachmentQuota"));
    }

    @Test
    public void testGetJSlob_fine_attachmentQuotaPerFileSet() throws OXException, JSONException {
        JSlob jSlob = sharedInfostoreJSlob.getJSlob(session);

        Assert.assertEquals(-1L, jSlob.getJsonObject().get("attachmentQuotaPerFile"));
    }
}
