
package com.openexchange.server.services;

import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import java.net.URI;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import com.openexchange.configuration.ServerConfig;
import com.openexchange.configuration.ServerConfig.Property;
import com.openexchange.exception.OXException;
import com.openexchange.filestore.FileStorages;
import com.openexchange.filestore.Info;
import com.openexchange.filestore.QuotaFileStorageService;
import com.openexchange.groupware.attach.AttachmentConfig;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextImpl;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.filestore.FilestoreStorage;
import com.openexchange.groupware.infostore.InfostoreConfig;
import com.openexchange.groupware.userconfiguration.UserPermissionBits;
import com.openexchange.jslob.JSlob;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.mail.usersetting.UserSettingMailStorage;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;

/**
 * Unit tests for {@link SharedInfostoreJSlobTest}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since 7.4.2
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ InfostoreConfig.class, ServerConfig.class, AttachmentConfig.class, ContextStorage.class, UserSettingMailStorage.class, UserSettingMail.class, FilestoreStorage.class, FileStorages.class, ServerSessionAdapter.class })
public class SharedInfostoreJSlobTest {

    @InjectMocks
    private SharedInfostoreJSlob sharedInfostoreJSlob;

    @Mock
    private ServerSession session;

    //    @Mock
    //    private QuotaFileStorage quotaFileStorage;
    @Mock
    private com.openexchange.filestore.QuotaFileStorage quotaFileStorage;

    @Mock
    private UserPermissionBits permissionBits;

    private final Context context = new ContextImpl(999999);

    private final int maxBodySize = 11111;

    private final Long infostoreMaxUploadSize = L(22222L);

    private final Long attachmentMaxUploadSize = L(33333L);

    private final Long quotaUsage = L(44444L);

    private final Long maxQuota = L(55555L);

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        PowerMockito.mockStatic(ServerSessionAdapter.class);
        PowerMockito.when(ServerSessionAdapter.valueOf((com.openexchange.session.Session) ArgumentMatchers.any())).thenReturn(session);

        PowerMockito.when(session.getContext()).thenReturn(context);
        PowerMockito.when(session.getUserPermissionBits()).thenReturn(permissionBits);

        PowerMockito.when(B(permissionBits.hasInfostore())).thenReturn(B(true));
        PowerMockito.when(B(permissionBits.hasWebMail())).thenReturn(B(true));

        PowerMockito.mockStatic(ServerConfig.class);
        PowerMockito.when(I(ServerConfig.getInt((Property) ArgumentMatchers.any()))).thenReturn(I(this.maxBodySize));

        PowerMockito.mockStatic(InfostoreConfig.class);
        PowerMockito.when(L(InfostoreConfig.getMaxUploadSize())).thenReturn(infostoreMaxUploadSize);

        PowerMockito.mockStatic(AttachmentConfig.class);
        PowerMockito.when(L(AttachmentConfig.getMaxUploadSize())).thenReturn(attachmentMaxUploadSize);

        PowerMockito.mockStatic(FilestoreStorage.class);
        PowerMockito.when(FilestoreStorage.createURI(ArgumentMatchers.eq(context))).thenReturn(new URI(""));

        QuotaFileStorageService qfsService = PowerMockito.mock(QuotaFileStorageService.class);
        Mockito.when(qfsService.getQuotaFileStorage(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.any(Info.class))).thenReturn(quotaFileStorage);

        PowerMockito.mockStatic(FileStorages.class);
        Mockito.when(FileStorages.getQuotaFileStorageService()).thenReturn(qfsService);

        PowerMockito.when(L(quotaFileStorage.getQuota())).thenReturn(maxQuota);
        PowerMockito.when(L(quotaFileStorage.getUsage())).thenReturn(quotaUsage);

        PowerMockito.mockStatic(UserSettingMailStorage.class);
        UserSettingMailStorage userSettingMailStorage = Mockito.mock(UserSettingMailStorage.class);
        PowerMockito.when(UserSettingMailStorage.getInstance()).thenReturn(userSettingMailStorage);

        UserSettingMail userSettingMail = Mockito.mock(UserSettingMail.class);
        PowerMockito.when(userSettingMailStorage.getUserSettingMail(ArgumentMatchers.anyInt(), ArgumentMatchers.eq(ArgumentMatchers.eq(context)))).thenReturn(userSettingMail);
    }

    @Test
    public void testGetJSlob_fine_maxBodySizeSet() throws OXException, JSONException {
        JSlob jSlob = sharedInfostoreJSlob.getJSlob(session);

        Assert.assertEquals(I(this.maxBodySize), jSlob.getJsonObject().get("maxBodySize"));
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

        Assert.assertEquals(L(-1L), jSlob.getJsonObject().get("attachmentQuota"));
    }

    @Test
    public void testGetJSlob_fine_attachmentQuotaPerFileSet() throws OXException, JSONException {
        JSlob jSlob = sharedInfostoreJSlob.getJSlob(session);

        Assert.assertEquals(L(-1L), jSlob.getJsonObject().get("attachmentQuotaPerFile"));
    }
}
