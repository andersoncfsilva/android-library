package com.urbanairship.push;

import android.content.Intent;

import com.urbanairship.AirshipConfigOptions;
import com.urbanairship.BaseTestCase;
import com.urbanairship.TestApplication;
import com.urbanairship.TestRequest;
import com.urbanairship.http.RequestFactory;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowIntent;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

public class NamedUserTest extends BaseTestCase {

    private final String fakeNamedUserId = "fake-named-user-id";
    private final String fakeToken = "AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE";

    private AirshipConfigOptions mockAirshipConfigOptions;
    private NamedUser namedUser;
    private TestRequest testRequest;

    @Before
    public void setUp() {
        mockAirshipConfigOptions = Mockito.mock(AirshipConfigOptions.class);
        testRequest = new TestRequest();

        RequestFactory mockRequestFactory = Mockito.mock(RequestFactory.class);
        when(mockRequestFactory.createRequest(anyString(), any(URL.class))).thenReturn(testRequest);

        when(mockAirshipConfigOptions.getAppKey()).thenReturn("appKey");
        when(mockAirshipConfigOptions.getAppSecret()).thenReturn("appSecret");

        TestApplication.getApplication().setOptions(mockAirshipConfigOptions);

        namedUser = new NamedUser(TestApplication.getApplication().preferenceDataStore);
    }

    /**
     * Test set valid ID (associate).
     */
    @Test
    public void testSetIDValid() {
        ShadowApplication application = Shadows.shadowOf(RuntimeEnvironment.application);
        application.clearStartedServices();

        namedUser.setId(fakeNamedUserId);

        Intent startedIntent = ShadowApplication.getInstance().getNextStartedService();
        assertEquals("Intent action should be clearing pending named user tags",
                PushService.ACTION_CLEAR_PENDING_NAMED_USER_TAGS, startedIntent.getAction());

        startedIntent = ShadowApplication.getInstance().getNextStartedService();
        assertEquals("Intent action should be to update named user",
                PushService.ACTION_UPDATE_NAMED_USER, startedIntent.getAction());
        assertEquals("Named user ID should be set", fakeNamedUserId, namedUser.getId());
    }

    /**
     * Test set invalid ID.
     */
    @Test
    public void testSetIDInvalid() {
        String currentNamedUserId = namedUser.getId();

        namedUser.setId("     ");
        assertEquals("Named user ID should not have changed", currentNamedUserId, namedUser.getId());
    }

    /**
     * Test set null ID (disassociate).
     */
    @Test
    public void testSetIDNull() {
        ShadowApplication application = Shadows.shadowOf(RuntimeEnvironment.application);
        application.clearStartedServices();

        namedUser.setId(null);

        Intent startedIntent = ShadowApplication.getInstance().getNextStartedService();
        assertEquals("Intent action should be clearing pending named user tags",
                PushService.ACTION_CLEAR_PENDING_NAMED_USER_TAGS, startedIntent.getAction());

        startedIntent = ShadowApplication.getInstance().getNextStartedService();
        assertEquals("Intent action should be to update named user",
                PushService.ACTION_UPDATE_NAMED_USER, startedIntent.getAction());
        assertNull("Named user ID should be null", namedUser.getId());
    }

    /**
     * Test when IDs match, don't update named user.
     */
    @Test
    public void testIdsMatchNoUpdate() {
        namedUser.setId(fakeNamedUserId);
        String changeToken = namedUser.getChangeToken();
        assertEquals("Named user ID should match", fakeNamedUserId, namedUser.getId());

        namedUser.setId(fakeNamedUserId);
        assertEquals("Change token should not change", changeToken, namedUser.getChangeToken());
    }

    /**
     * Test force update changes the current token and starts the service.
     */
    @Test
    public void testForceUpdate() {
        String changeToken = namedUser.getChangeToken();

        ShadowApplication application = Shadows.shadowOf(RuntimeEnvironment.application);
        application.clearStartedServices();

        namedUser.forceUpdate();

        ShadowIntent intent = Shadows.shadowOf(application.peekNextStartedService());
        assertEquals("Intent action should be to update named user",
                intent.getAction(), PushService.ACTION_UPDATE_NAMED_USER);
        assertNotSame("Change token should have changed", changeToken, namedUser.getChangeToken());
    }

    /**
     * Test update change token.
     */
    @Test
    public void testUpdateChangeToken() {
        String changeToken = namedUser.getChangeToken();
        namedUser.updateChangeToken();
        assertNotSame("Change token should have changed", changeToken, namedUser.getChangeToken());
    }

    /**
     * Test set last updated token.
     */
    @Test
    public void testSetLastUpdatedToken() {
        namedUser.setId(fakeNamedUserId);
        String lastUpdatedToken = namedUser.getLastUpdatedToken();

        namedUser.setLastUpdatedToken(fakeToken);
        assertNotSame("Last updated token should not match", namedUser.getLastUpdatedToken(), lastUpdatedToken);
        assertEquals("Last updated token should match", fakeToken, namedUser.getLastUpdatedToken());
    }

    /**
     * Test disassociateNamedUserIfNull clears the named user ID when it is null.
     */
    @Test
    public void testDisassociateNamedUserNullId() {
        namedUser.setId(null);
        namedUser.disassociateNamedUserIfNull();
        assertNull("Named user ID should be null", namedUser.getId());
    }

    /**
     * Test disassociateNamedUserIfNull does not clear named user ID, when it is not null.
     */
    @Test
    public void testDisassociateNamedUserNonNullId() {
        namedUser.setId(fakeNamedUserId);
        namedUser.disassociateNamedUserIfNull();
        assertEquals("Named user ID should remain the same", fakeNamedUserId, namedUser.getId());
    }

    /**
     * Test editTagGroups apply starts the update named user tags service.
     */
    @Test
    public void testStartUpdateNamedUserTagsService() {

        namedUser.editTagGroups()
                 .addTag("tagGroup", "tag1")
                 .addTag("tagGroup", "tag2")
                 .addTag("tagGroup", "tag3")
                 .removeTag("tagGroup", "tag3")
                 .removeTag("tagGroup", "tag4")
                 .removeTag("tagGroup", "tag5")
                 .apply();

        Intent startedIntent = ShadowApplication.getInstance().getNextStartedService();
        assertEquals("Expect Update Named User Tags Service", PushService.ACTION_UPDATE_NAMED_USER_TAGS, startedIntent.getAction());
    }

    /**
     * Test editTagGroups apply does not start the service when addTags and removeTags are empty.
     */
    @Test
    public void testEmptyAddTagsRemoveTags() {

        namedUser.editTagGroups().apply();

        Intent startedIntent = ShadowApplication.getInstance().peekNextStartedService();
        assertNull("Update named user tags service should not have started", startedIntent);
    }

    /**
     * Test set pending tag groups.
     */
    @Test
    public void testPendingTagGroups() {
        Set<String> addTags = new HashSet<>();
        addTags.add("tag1");
        addTags.add("tag2");
        addTags.add("tag3");

        Map<String, Set<String>> pendingAddTags = new HashMap<>();
        pendingAddTags.put("tagGroup", addTags);

        Set<String> removeTags = new HashSet<>();
        removeTags.add("tag3");
        removeTags.add("tag4");
        removeTags.add("tag5");

        Map<String, Set<String>> pendingRemoveTags = new HashMap<>();
        pendingRemoveTags.put("tagGroup", removeTags);

        namedUser.setPendingTagGroupsChanges(pendingAddTags, pendingRemoveTags);

        Assert.assertEquals("Pending add tags should match", pendingAddTags, namedUser.getPendingAddTagGroups());
        Assert.assertEquals("Pending remove tags should match", pendingRemoveTags, namedUser.getPendingRemoveTagGroups());
    }

    /**
     * Test clear pending tag groups.
     */
    @Test
    public void testClearPendingTagGroups() {
        Map<String, Set<String>> emptyTags = new HashMap<>();

        namedUser.setPendingTagGroupsChanges(null, null);

        Assert.assertEquals("Pending add tags should be empty", emptyTags, namedUser.getPendingAddTagGroups());
        Assert.assertEquals("Pending remove tags should be empty", emptyTags, namedUser.getPendingRemoveTagGroups());
    }

    /**
     * Test startUpdateService starts the update named user service.
     */
    @Test
    public void testStartUpdateService() {

        namedUser.startUpdateService();

        Intent startedIntent = ShadowApplication.getInstance().getNextStartedService();
        assertEquals("Expect Update Named User Service", PushService.ACTION_UPDATE_NAMED_USER, startedIntent.getAction());
    }

    /**
     * Test startUpdateTagsService starts the update named user tags service.
     */
    @Test
    public void testStartUpdateTagsService() {

        namedUser.startUpdateTagsService();

        Intent startedIntent = ShadowApplication.getInstance().getNextStartedService();
        assertEquals("Expect Update Named User Tags Service", PushService.ACTION_UPDATE_NAMED_USER_TAGS, startedIntent.getAction());
    }

    /**
     * Test startClearPendingTagsService starts the clear named user tags service.
     */
    @Test
    public void testStartClearPendingTagsService() {
        namedUser.startClearPendingTagsService();

        Intent startedIntent = ShadowApplication.getInstance().getNextStartedService();
        assertEquals("Expect Clear Pending Tags Service", PushService.ACTION_CLEAR_PENDING_NAMED_USER_TAGS, startedIntent.getAction());
    }
}
