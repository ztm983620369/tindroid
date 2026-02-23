package co.tinode.tinodesdk.model;

public class MsgReactions {
    public int seq;
    public MsgOneReaction[] reacts;

    public MsgReactions() {}

    public MsgReactions(int mrrid, MsgReactClient r) {
        this.seq = r.seq;
        reacts = new MsgOneReaction[1];
        reacts[0] = new MsgOneReaction(mrrid, r);
    }
}
