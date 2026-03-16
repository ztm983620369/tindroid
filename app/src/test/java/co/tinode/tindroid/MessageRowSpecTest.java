package co.tinode.tindroid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Date;

public class MessageRowSpecTest {

    @Test
    public void privateIncomingMessage_usesLeftSideWithoutName() {
        MessageRowSpec spec = MessageRowSpec.resolve(false, false, false, new Date(1000), null);

        assertEquals(MessageRowSpec.Side.LEFT, spec.getSide());
        assertTrue(spec.showDateDivider());
        assertFalse(spec.showSenderName());
    }

    @Test
    public void privateOutgoingMessage_usesRightSideWithoutName() {
        MessageRowSpec spec = MessageRowSpec.resolve(true, false, false, new Date(1000), new Date(2000));

        assertEquals(MessageRowSpec.Side.RIGHT, spec.getSide());
        assertFalse(spec.showSenderName());
    }

    @Test
    public void groupIncomingMessage_showsSenderName() {
        MessageRowSpec spec = MessageRowSpec.resolve(false, true, false, new Date(1000), new Date(1000));

        assertEquals(MessageRowSpec.Side.LEFT, spec.getSide());
        assertTrue(spec.showSenderName());
    }

    @Test
    public void groupOutgoingMessage_hidesSenderName() {
        MessageRowSpec spec = MessageRowSpec.resolve(true, true, false, new Date(1000), new Date(1000));

        assertEquals(MessageRowSpec.Side.RIGHT, spec.getSide());
        assertFalse(spec.showSenderName());
    }

    @Test
    public void channelIncomingMessage_hidesSenderName() {
        MessageRowSpec spec = MessageRowSpec.resolve(false, true, true, new Date(1000), new Date(1000));

        assertFalse(spec.showSenderName());
    }

    @Test
    public void dateDivider_showsWhenOlderMessageIsDifferentDay() {
        Date current = new Date(2L * 24L * 60L * 60L * 1000L);
        Date older = new Date(1L * 24L * 60L * 60L * 1000L);

        MessageRowSpec spec = MessageRowSpec.resolve(false, false, false, current, older);

        assertTrue(spec.showDateDivider());
    }

    @Test
    public void dateDivider_hidesWhenOlderMessageIsSameDay() {
        Date current = new Date(2L * 24L * 60L * 60L * 1000L + 1000L);
        Date older = new Date(2L * 24L * 60L * 60L * 1000L + 2000L);

        MessageRowSpec spec = MessageRowSpec.resolve(false, false, false, current, older);

        assertFalse(spec.showDateDivider());
    }
}
