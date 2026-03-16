package co.tinode.tindroid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import co.tinode.tinodesdk.model.Drafty;

public class ComposerEventTest {

    @Test
    public void topicStateChanged_carriesPermissionFlags() {
        ComposerEvent event = ComposerEvent.topicStateChanged(true, false, true);

        assertEquals(ComposerEvent.Type.TOPIC_STATE_CHANGED, event.type);
        assertTrue(event.boolValue);
        assertFalse(event.boolValue2);
        assertTrue(event.boolValue3);
        assertNull(event.anchor);
    }

    @Test
    public void beginEditing_preservesOriginalQuoteAndSequence() {
        Drafty quote = Drafty.parse("quote");

        ComposerEvent event = ComposerEvent.beginEditing("original", quote, 25);

        assertEquals(ComposerEvent.Type.BEGIN_EDITING, event.type);
        assertEquals("original", event.original);
        assertSame(quote, event.primaryDraft);
        assertEquals(25, event.intValue);
    }

    @Test
    public void beginForward_preservesSenderAndContent() {
        Drafty sender = Drafty.parse("sender");
        Drafty content = Drafty.parse("content");

        ComposerEvent event = ComposerEvent.beginForward(sender, content);

        assertEquals(ComposerEvent.Type.BEGIN_FORWARD, event.type);
        assertSame(sender, event.primaryDraft);
        assertSame(content, event.secondaryDraft);
    }

    @Test
    public void textChanged_keepsTypedContent() {
        ComposerEvent event = ComposerEvent.textChanged("hello");

        assertEquals(ComposerEvent.Type.TEXT_CHANGED, event.type);
        assertEquals("hello", event.text.toString());
    }

    @Test
    public void hideTray_keepsReason() {
        ComposerEvent event = ComposerEvent.hideTray("trayFile", null);

        assertEquals(ComposerEvent.Type.HIDE_TRAY, event.type);
        assertEquals("trayFile", event.reason);
    }
}
