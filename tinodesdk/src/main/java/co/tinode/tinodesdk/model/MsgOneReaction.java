package co.tinode.tinodesdk.model;

// MsgOneReaction to message.
public class MsgOneReaction {
    // Most recent (greatest) reaction ID for this content.
    public int mrrid;
    // MsgOneReaction content (emoji etc.).
    public String val;
    // Number of users who reacted with this value (count be 0 for p2p and group topics).
    public Integer count;
    // User IDs of users who reacted with this value (count be nil for channels).
    public  String[] users;

    public MsgOneReaction() {
    }

    public MsgOneReaction(int mrrid, String val, Integer count, String[] users) {
        this.mrrid = mrrid;
        this.val = val;
        this.count = count;
        this.users = users;
    }

    public MsgOneReaction(int mrrid, MsgReactClient r) {
        this.mrrid = mrrid;
        this.val = r.val;
        this.count = 1;
    }
}
