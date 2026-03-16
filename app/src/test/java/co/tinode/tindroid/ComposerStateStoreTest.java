package co.tinode.tindroid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import co.tinode.tinodesdk.model.Drafty;

public class ComposerStateStoreTest {

    @Test
    public void quotedState_setsEditMetadataAndClearsForwarding() {
        ComposerStateStore store = new ComposerStateStore();
        Drafty sender = Drafty.parse("sender");
        Drafty content = Drafty.parse("content");
        Drafty quote = Drafty.parse("quote");

        store.setForwardState(sender, content);
        store.setQuotedState(UiUtils.MsgAction.EDIT, quote, 42);

        assertTrue(store.isEditing());
        assertTrue(store.hasQuotedContent());
        assertEquals(42, store.getQuotedSeqId());
        assertSame(quote, store.getQuote());
        assertFalse(store.hasForwardContent());
        assertNull(store.getForwardSender());
        assertNull(store.getContentToForward());
    }

    @Test
    public void forwardState_clearsQuotedStateAndMarksForwardMode() {
        ComposerStateStore store = new ComposerStateStore();
        Drafty quote = Drafty.parse("quote");
        Drafty sender = Drafty.parse("sender");
        Drafty content = Drafty.parse("content");

        store.setQuotedState(UiUtils.MsgAction.REPLY, quote, 7);
        store.setForwardState(sender, content);

        assertEquals(UiUtils.MsgAction.FORWARD, store.getTextAction());
        assertFalse(store.isEditing());
        assertFalse(store.hasQuotedContent());
        assertTrue(store.hasForwardContent());
        assertSame(sender, store.getForwardSender());
        assertSame(content, store.getContentToForward());
    }

    @Test
    public void clearQuotedState_resetsOnlyQuotedFields() {
        ComposerStateStore store = new ComposerStateStore();
        Drafty quote = Drafty.parse("quote");

        store.setQuotedState(UiUtils.MsgAction.REPLY, quote, 11);
        store.clearQuotedState();

        assertEquals(UiUtils.MsgAction.NONE, store.getTextAction());
        assertFalse(store.hasQuotedContent());
        assertEquals(-1, store.getQuotedSeqId());
        assertNull(store.getQuote());
    }

    @Test
    public void clearForwardState_resetsForwardFields() {
        ComposerStateStore store = new ComposerStateStore();
        Drafty sender = Drafty.parse("sender");
        Drafty content = Drafty.parse("content");

        store.setForwardState(sender, content);
        store.clearForwardState();

        assertEquals(UiUtils.MsgAction.NONE, store.getTextAction());
        assertFalse(store.hasForwardContent());
        assertNull(store.getForwardSender());
        assertNull(store.getContentToForward());
    }

    @Test
    public void clearAll_resetsEveryComposerStateField() {
        ComposerStateStore store = new ComposerStateStore();
        store.setForwardState(Drafty.parse("sender"), Drafty.parse("content"));
        store.clearAll();

        assertEquals(UiUtils.MsgAction.NONE, store.getTextAction());
        assertFalse(store.hasForwardContent());
        assertFalse(store.hasQuotedContent());
        assertEquals(-1, store.getQuotedSeqId());
        assertNull(store.getQuote());
        assertNull(store.getForwardSender());
        assertNull(store.getContentToForward());
    }

    @Test
    public void quotedReplyState_marksReplyWithoutEditMode() {
        ComposerStateStore store = new ComposerStateStore();

        store.setQuotedState(UiUtils.MsgAction.REPLY, Drafty.parse("quote"), 15);

        assertEquals(UiUtils.MsgAction.REPLY, store.getTextAction());
        assertFalse(store.isEditing());
        assertTrue(store.hasQuotedContent());
        assertEquals(15, store.getQuotedSeqId());
    }

    @Test
    public void clearQuotedState_doesNotRemoveForwardPayload() {
        ComposerStateStore store = new ComposerStateStore();
        store.setForwardState(Drafty.parse("sender"), Drafty.parse("content"));
        store.clearQuotedState();

        assertEquals(UiUtils.MsgAction.FORWARD, store.getTextAction());
        assertTrue(store.hasForwardContent());
        assertEquals("sender", store.getForwardSender().toString());
        assertEquals("content", store.getContentToForward().toString());
    }
}
