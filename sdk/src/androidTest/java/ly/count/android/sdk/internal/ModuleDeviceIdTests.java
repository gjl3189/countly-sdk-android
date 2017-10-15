package ly.count.android.sdk.internal;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;

import java.net.MalformedURLException;
import java.util.List;

import ly.count.android.sdk.Config;

import static android.support.test.InstrumentationRegistry.getContext;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class ModuleDeviceIdTests {
    private Config config = null;
    private InternalConfig internalConfig = null;
    private Core core = null;
    private ModuleDeviceId module = null;
    private Module dummy = null;
    private Utils utils = null;
    private Context ctx = null;

    @Test(expected = IllegalArgumentException.class)
    public void checkStrictAdvertisingId() throws Exception {
        utils = Mockito.spy(new Utils());
        Utils.reflectiveSetField(Utils.class, "utils", utils);
        doReturn(Boolean.FALSE).when(utils)._reflectiveClassExists(ModuleDeviceId.ADVERTISING_ID_CLIENT_CLASS_NAME);

        InternalConfig config = TestingUtilityInternal.setupLogs(null);
        config.setDeviceIdFallbackAllowed(false);
        config.setDeviceIdStrategy(Config.DeviceIdStrategy.ADVERTISING_ID);
        ModuleDeviceId module = new ModuleDeviceId();
        module.init(config);
    }

    @Before
    public void beforeEachTest() throws Exception {
        ctx = new ContextImpl(getContext());
        Core.purgeInternalStorage(ctx, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkStrictInstanceId() throws Exception {
        utils = Mockito.spy(new Utils());
        Utils.reflectiveSetField(Utils.class, "utils", utils);
        doReturn(Boolean.FALSE).when(utils)._reflectiveClassExists(ModuleDeviceId.INSTANCE_ID_CLASS_NAME);

        InternalConfig config = TestingUtilityInternal.setupLogs(null);
        config.setDeviceIdFallbackAllowed(false);
        config.setDeviceIdStrategy(Config.DeviceIdStrategy.INSTANCE_ID);
        ModuleDeviceId module = new ModuleDeviceId();
        module.init(config);
    }

    @Test
    public void checkNotStrictAdvertisingId() throws Exception {
        utils = Mockito.spy(new Utils());
        Utils.reflectiveSetField(Utils.class, "utils", utils);
        doReturn(Boolean.TRUE).when(utils)._reflectiveClassExists(ModuleDeviceId.ADVERTISING_ID_CLIENT_CLASS_NAME);

        InternalConfig config = TestingUtilityInternal.setupLogs(null);
        config.setDeviceIdStrategy(Config.DeviceIdStrategy.ADVERTISING_ID);
        ModuleDeviceId module = new ModuleDeviceId();
        module.init(config);
        Assert.assertTrue(true);
    }

    @Test
    public void checkNotStrictInstanceId() throws Exception {
        utils = Mockito.spy(new Utils());
        Utils.reflectiveSetField(Utils.class, "utils", utils);
        doReturn(Boolean.TRUE).when(utils)._reflectiveClassExists(ModuleDeviceId.INSTANCE_ID_CLASS_NAME);

        InternalConfig config = TestingUtilityInternal.setupLogs(null);
        config.setDeviceIdStrategy(Config.DeviceIdStrategy.INSTANCE_ID);
        ModuleDeviceId module = new ModuleDeviceId();
        module.init(config);
        Assert.assertTrue(true);
    }

    @Test
    public void checkLimitedContext() throws Exception {
        String deviceId = "123dev";
        config = TestingUtilityInternal.setupConfig();
        config.setCustomDeviceId(deviceId);
        init(false);

        core.onLimitedContextAcquired(getContext());

        Config.DID did = internalConfig.getDeviceId(Config.DeviceIdRealm.DEVICE_ID);
        Assert.assertNull(did);
        Mockito.verify(dummy, never()).onDeviceId(ctx, did, null);

        internalConfig = Storage.read(ctx, internalConfig);
        Assert.assertNull(internalConfig);
    }

    @Test
    public void checkCustomId_fresh() throws Exception {
        String deviceId = "123dev";
        config = TestingUtilityInternal.setupConfig();
        config.setCustomDeviceId(deviceId);
        init(false);

        core.onContextAcquired(TestingUtilityInternal.mockApplication(getContext()));

        Config.DID did = internalConfig.getDeviceId(Config.DeviceIdRealm.DEVICE_ID);
        Assert.assertNotNull(did);
        Assert.assertTrue(did.strategy == Config.DeviceIdStrategy.CUSTOM_ID);
        Assert.assertEquals(deviceId, did.id);
        Mockito.verify(dummy, times(1)).onDeviceId(isA(ctx.getClass()), eq(did), isNull(Config.DID.class));

        internalConfig = Storage.read(ctx, internalConfig);
        Assert.assertNotNull(internalConfig);
        did = internalConfig.getDeviceId(Config.DeviceIdRealm.DEVICE_ID);
        Assert.assertNotNull(did);
        Assert.assertTrue(did.strategy == Config.DeviceIdStrategy.CUSTOM_ID);
        Assert.assertEquals(deviceId, did.id);
    }

    @Test
    public void checkCustomId_legacy() throws Exception {
        String deviceId = "123dev";
        String legacyId = "123legacy";
        config = TestingUtilityInternal.setupConfig();
        config.setCustomDeviceId(deviceId);
        init(false);

        getContext().getSharedPreferences(Legacy.PREFERENCES, android.content.Context.MODE_PRIVATE).edit().putString(Legacy.KEY_ID_ID, legacyId).commit();

        core.onContextAcquired(TestingUtilityInternal.mockApplication(getContext()));

        Config.DID did = internalConfig.getDeviceId(Config.DeviceIdRealm.DEVICE_ID);
        Assert.assertNotNull(did);
        Assert.assertTrue(did.strategy == Config.DeviceIdStrategy.CUSTOM_ID);
        Assert.assertEquals(legacyId, did.id);
        Mockito.verify(dummy, times(1)).onDeviceId(isA(ctx.getClass()), eq(did), eq(did));

        internalConfig = Storage.read(ctx, internalConfig);
        Assert.assertNotNull(internalConfig);
        did = internalConfig.getDeviceId(Config.DeviceIdRealm.DEVICE_ID);
        Assert.assertNotNull(did);
        Assert.assertTrue(did.strategy == Config.DeviceIdStrategy.CUSTOM_ID);
        Assert.assertEquals(legacyId, did.id);
    }

    @Test
    public void checkAdvId_fresh() throws Exception {
        android.content.Context context = getContext();
        ModuleDeviceId.AdvIdInfo.deviceId = "123adv";
        config = TestingUtilityInternal.setupConfig();
        config.setDeviceIdStrategy(Config.DeviceIdStrategy.ADVERTISING_ID);
        config.enableTestMode();
        config.setLoggingLevel(Config.LoggingLevel.DEBUG);
        init(false);

        doReturn(Boolean.TRUE).when(utils)._reflectiveClassExists(ModuleDeviceId.ADVERTISING_ID_CLIENT_CLASS_NAME);
        doReturn(new ModuleDeviceId.AdvIdInfo()).when(utils)._reflectiveCallStrict(eq(ModuleDeviceId.ADVERTISING_ID_CLIENT_CLASS_NAME), ArgumentMatchers.isNull(), eq("getAdvertisingIdInfo"), eq(android.content.Context.class), isA(android.content.Context.class));

        core.onContextAcquired(TestingUtilityInternal.mockApplication(context));
        Tasks tasks = Utils.reflectiveGetField(module, "tasks");
        tasks.await();

        Config.DID did = internalConfig.getDeviceId();
        Assert.assertNotNull(did);
        Assert.assertEquals(ModuleDeviceId.AdvIdInfo.deviceId, did.id);
        Mockito.verify(dummy, times(1)).onDeviceId(isA(ctx.getClass()), eq(did), isNull(Config.DID.class));

        internalConfig = Storage.read(ctx, internalConfig);
        Assert.assertNotNull(internalConfig);
        did = internalConfig.getDeviceId(Config.DeviceIdRealm.DEVICE_ID);
        Assert.assertNotNull(did);
        Assert.assertEquals(ModuleDeviceId.AdvIdInfo.deviceId, did.id);
    }

    @Test
    public void checkAdvId_legacy() throws Exception {
        android.content.Context context = getContext();
        ModuleDeviceId.AdvIdInfo.deviceId = "123adv";
        String legacyId = "123legacy";
        config = TestingUtilityInternal.setupConfig();
        config.setDeviceIdStrategy(Config.DeviceIdStrategy.ADVERTISING_ID);
        config.enableTestMode();
        config.setLoggingLevel(Config.LoggingLevel.DEBUG);
        init(false);

        getContext().getSharedPreferences(Legacy.PREFERENCES, android.content.Context.MODE_PRIVATE).edit().putString(Legacy.KEY_ID_ID, legacyId).commit();

        doReturn(Boolean.TRUE).when(utils)._reflectiveClassExists(ModuleDeviceId.ADVERTISING_ID_CLIENT_CLASS_NAME);
        doReturn(new ModuleDeviceId.AdvIdInfo()).when(utils)._reflectiveCall(eq(ModuleDeviceId.ADVERTISING_ID_CLIENT_CLASS_NAME), ArgumentMatchers.isNull(), eq("getAdvertisingIdInfo"), isA(android.content.Context.class));

        core.onContextAcquired(TestingUtilityInternal.mockApplication(context));

        Assert.assertNull(Utils.reflectiveGetField(module, "tasks"));

        Config.DID did = internalConfig.getDeviceId();
        Assert.assertNotNull(did);
        Assert.assertEquals(legacyId, did.id);
        Mockito.verify(dummy, times(1)).onDeviceId(isA(ctx.getClass()), eq(did), eq(did));

        internalConfig = Storage.read(ctx, internalConfig);
        Assert.assertNotNull(internalConfig);
        did = internalConfig.getDeviceId(Config.DeviceIdRealm.DEVICE_ID);
        Assert.assertNotNull(did);
        Assert.assertEquals(legacyId, did.id);
    }

    @Test
    public void checkAdvId_Fallback() throws Exception {
        android.content.Context context = getContext();
        ModuleDeviceId.AdvIdInfo.deviceId = "123adv";
        config = TestingUtilityInternal.setupConfig();
        config.setDeviceIdStrategy(Config.DeviceIdStrategy.ADVERTISING_ID);
        config.enableTestMode();
        config.setLoggingLevel(Config.LoggingLevel.DEBUG);
        init(false);

        doReturn(Boolean.FALSE).when(utils)._reflectiveClassExists(ModuleDeviceId.ADVERTISING_ID_CLIENT_CLASS_NAME);

        core.onContextAcquired(TestingUtilityInternal.mockApplication(context));

        Tasks tasks = Utils.reflectiveGetField(module, "tasks");
        tasks.await();

        Config.DID did = internalConfig.getDeviceId();
        Assert.assertNotNull(did);
        Assert.assertFalse(did.id.equals(ModuleDeviceId.AdvIdInfo.deviceId));
        Assert.assertEquals(did.strategy, Config.DeviceIdStrategy.OPEN_UDID);
        Mockito.verify(dummy, times(1)).onDeviceId(isA(ctx.getClass()), eq(did), isNull(Config.DID.class));

        internalConfig = Storage.read(ctx, internalConfig);
        Assert.assertNotNull(internalConfig);
        did = internalConfig.getDeviceId(Config.DeviceIdRealm.DEVICE_ID);
        Assert.assertNotNull(did);
        Assert.assertEquals(did.strategy, Config.DeviceIdStrategy.OPEN_UDID);
    }

    @Test
    public void checkInstId_fresh() throws Exception {
        android.content.Context context = getContext();
        ModuleDeviceId.InstIdInstance.deviceId = "123inst";
        config = TestingUtilityInternal.setupConfig();
        config.setDeviceIdStrategy(Config.DeviceIdStrategy.INSTANCE_ID);
        config.enableTestMode();
        config.setLoggingLevel(Config.LoggingLevel.DEBUG);
        init(false);

        doReturn(Boolean.TRUE).when(utils)._reflectiveClassExists(ModuleDeviceId.INSTANCE_ID_CLASS_NAME);
        doReturn(new ModuleDeviceId.InstIdInstance()).when(utils)._reflectiveCall(eq(ModuleDeviceId.INSTANCE_ID_CLASS_NAME), ArgumentMatchers.isNull(), eq("getInstance"));

        core.onContextAcquired(TestingUtilityInternal.mockApplication(context));
        Tasks tasks = Utils.reflectiveGetField(module, "tasks");
        tasks.await();

        Config.DID did = internalConfig.getDeviceId();
        Assert.assertNotNull(did);
        Assert.assertEquals(ModuleDeviceId.InstIdInstance.deviceId, did.id);
        Mockito.verify(dummy, times(1)).onDeviceId(isA(ctx.getClass()), eq(did), isNull(Config.DID.class));

        internalConfig = Storage.read(ctx, internalConfig);
        Assert.assertNotNull(internalConfig);
        did = internalConfig.getDeviceId(Config.DeviceIdRealm.DEVICE_ID);
        Assert.assertNotNull(did);
        Assert.assertEquals(ModuleDeviceId.InstIdInstance.deviceId, did.id);
    }

    @Test
    public void checkInstId_legacy() throws Exception {
        android.content.Context context = getContext();
        ModuleDeviceId.InstIdInstance.deviceId = "123inst";
        String legacyId = "123legacy";
        config = TestingUtilityInternal.setupConfig();
        config.setDeviceIdStrategy(Config.DeviceIdStrategy.INSTANCE_ID);
        config.enableTestMode();
        config.setLoggingLevel(Config.LoggingLevel.DEBUG);
        init(false);

        getContext().getSharedPreferences(Legacy.PREFERENCES, android.content.Context.MODE_PRIVATE).edit().putString(Legacy.KEY_ID_ID, legacyId).commit();

        doReturn(Boolean.TRUE).when(utils)._reflectiveClassExists(ModuleDeviceId.INSTANCE_ID_CLASS_NAME);
        doReturn(new ModuleDeviceId.InstIdInstance()).when(utils)._reflectiveCall(eq(ModuleDeviceId.INSTANCE_ID_CLASS_NAME), ArgumentMatchers.isNull(), eq("getInstance"), isA(android.content.Context.class));

        core.onContextAcquired(TestingUtilityInternal.mockApplication(context));

        Assert.assertNull(Utils.reflectiveGetField(module, "tasks"));

        Config.DID did = internalConfig.getDeviceId();
        Assert.assertNotNull(did);
        Assert.assertEquals(legacyId, did.id);
        Mockito.verify(dummy, times(1)).onDeviceId(isA(ctx.getClass()), eq(did), eq(did));

        internalConfig = Storage.read(ctx, internalConfig);
        Assert.assertNotNull(internalConfig);
        did = internalConfig.getDeviceId(Config.DeviceIdRealm.DEVICE_ID);
        Assert.assertNotNull(did);
        Assert.assertEquals(legacyId, did.id);
    }

    @Test
    public void checkInstId_Fallback() throws Exception {
        android.content.Context context = getContext();
        ModuleDeviceId.InstIdInstance.deviceId = "123inst";
        config = TestingUtilityInternal.setupConfig();
        config.setDeviceIdStrategy(Config.DeviceIdStrategy.INSTANCE_ID);
        config.enableTestMode();
        config.setLoggingLevel(Config.LoggingLevel.DEBUG);
        init(false);

        doReturn(Boolean.FALSE).when(utils)._reflectiveClassExists(ModuleDeviceId.INSTANCE_ID_CLASS_NAME);

        core.onContextAcquired(TestingUtilityInternal.mockApplication(context));

        Tasks tasks = Utils.reflectiveGetField(module, "tasks");
        tasks.await();

        Config.DID did = internalConfig.getDeviceId();
        Assert.assertNotNull(did);
        Assert.assertFalse(did.id.equals(ModuleDeviceId.AdvIdInfo.deviceId));
        Assert.assertEquals(did.strategy, Config.DeviceIdStrategy.OPEN_UDID);
        Mockito.verify(dummy, times(1)).onDeviceId(isA(ctx.getClass()), eq(did), isNull(Config.DID.class));

        internalConfig = Storage.read(ctx, internalConfig);
        Assert.assertNotNull(internalConfig);
        did = internalConfig.getDeviceId(Config.DeviceIdRealm.DEVICE_ID);
        Assert.assertNotNull(did);
        Assert.assertEquals(did.strategy, Config.DeviceIdStrategy.OPEN_UDID);
    }

    @Test
    public void checkRequestsTransform() throws Exception {
        Utils.reflectiveSetField(ModuleDeviceId.class, "testSleep", 2000L);

        android.content.Context context = getContext();
        config = TestingUtilityInternal.setupConfig()
                .enableTestMode()
                .setLoggingLevel(Config.LoggingLevel.DEBUG)
                .setSendUpdateEachSeconds(5);
        init(false);

        List<SessionImpl> sessions = Utils.reflectiveGetField(core, "sessions");

        // fire up device id acquiring
        core.onContextAcquired(TestingUtilityInternal.mockApplication(context));
        Assert.assertNull(internalConfig.getDeviceId());

        // also make ModuleSessions begin a session
        Assert.assertEquals(0, sessions.size());
        core.module(ModuleSessions.class).onActivityStarted(ctx);
        Assert.assertEquals(1, sessions.size());

        // ensure session is written
        List<Long> ids = Storage.list(ctx, SessionImpl.getStoragePrefix());
        Assert.assertEquals(1, ids.size());
        SessionImpl session = Storage.read(ctx, new SessionImpl(ctx, ids.get(0)));
        Assert.assertNotNull(session);

        // there must be only single begin request
        ids = Storage.list(ctx, Request.getStoragePrefix());
        Assert.assertEquals(1, ids.size());
        Request request = Storage.read(ctx, new Request(ids.get(0)));
        Assert.assertNotNull(request);
        Assert.assertTrue(request.params.toString().contains("begin_session=1"));
        // .. and it shouldn't have an id yet - it generation takes 2 seconds
        Assert.assertFalse(request.params.toString().contains(Params.PARAM_DEVICE_ID));

        // now sleep update time for ModuleSessions to create update request
        Thread.sleep(1000 * internalConfig.getSendUpdateEachSeconds());
        // ... and ensure session was updated and request was created
        ids = Storage.list(ctx, SessionImpl.getStoragePrefix());
        Assert.assertEquals(1, ids.size());
        session = Storage.read(ctx, new SessionImpl(ctx, ids.get(0)));
        Assert.assertNotNull(session);
        Assert.assertNotNull(session.updated);

        // in the meantime, since it takes 2 seconds for ModuleDeviceId to get an id while we slept 5 seconds,
        // there must be an id already
        Assert.assertNotNull(internalConfig.getDeviceId());
        String idString = internalConfig.getDeviceId().id;

        // now we should have begin & update requests with ids filled
        ids = Storage.list(ctx, Request.getStoragePrefix());
        Assert.assertEquals(2, ids.size());
        request = Storage.read(ctx, request);
        Assert.assertNotNull(request);
        Assert.assertTrue(request.params.toString().contains("device_id=" + idString));
        request = Storage.read(ctx, new Request(ids.get(1)));
        Assert.assertNotNull(request);
        Assert.assertTrue(request.params.toString().contains("device_id=" + idString));

        // ----------------- now changing device id --------------------
        Core.instance.login(ctx.getContext(), "did");

        // let's wait for tasks to finish pushing requests and such
        Tasks deviceIdTasks = Utils.reflectiveGetField(module, "tasks");
        deviceIdTasks.await();

        // there must be: begin, update, end from first session + id change request + begin from second session
        ids = Storage.list(ctx, Request.getStoragePrefix());
        Assert.assertEquals(5, ids.size());

        // first begin
        request = Storage.read(ctx, request);
        Assert.assertNotNull(request);
        Assert.assertTrue(request.params.toString().contains("device_id=" + idString));

        // first update
        request = Storage.read(ctx, new Request(ids.get(1)));
        Assert.assertNotNull(request);
        Assert.assertTrue(request.params.toString().contains("device_id=" + idString));

        // first end
        request = Storage.read(ctx, new Request(ids.get(2)));
        Assert.assertNotNull(request);
        Log.e("read request " + request.params);
        Assert.assertTrue(request.params.toString().contains("end_session=1"));
        Assert.assertTrue(request.params.toString().contains("device_id=" + idString));
        Assert.assertFalse(request.params.toString().contains("old_device_id="));

        // id change
        request = Storage.read(ctx, new Request(ids.get(3)));
        Assert.assertNotNull(request);
        Assert.assertTrue(request.params.toString().contains("device_id=did"));
        Assert.assertTrue(request.params.toString().contains("old_device_id=" + idString));

        // second begin
        request = Storage.read(ctx, new Request(ids.get(4)));
        Assert.assertNotNull(request);
        Assert.assertTrue(request.params.toString().contains("device_id=did"));
        Assert.assertTrue(request.params.toString().contains("begin_session=1"));
    }

    private void init(boolean mockModule) throws MalformedURLException {
        if (config == null) {
            config = TestingUtilityInternal.setupConfig();
            config.setLoggingLevel(Config.LoggingLevel.DEBUG);
        }
        config.enableTestMode();
        if (core == null) {
            core = Core.initForApplication(config, getContext());
        }
        dummy = Mockito.mock(Module.class);
        List<Module> list = Whitebox.getInternalState(core, "modules");
        if (module == null) {
            for (Module m : list) {
                if (m instanceof ModuleDeviceId) {
                    module = (ModuleDeviceId) m;
                    break;
                }
            }
            if (mockModule) {
                list.remove(module);
                module = Mockito.spy(module);
                list.add(1, module);
            }
        }
        list.add(dummy);
        internalConfig = Utils.reflectiveGetField(core, "config");
        new Log().init(internalConfig);
        utils = Mockito.spy(new Utils());
        Utils.reflectiveSetField(Utils.class, "utils", utils);

        Assert.assertNotNull(config);
        Assert.assertNotNull(internalConfig);
        Assert.assertNotNull(core);
        Assert.assertNotNull(module);
        Assert.assertNotNull(dummy);
        Assert.assertNotNull(utils);
    }
}
