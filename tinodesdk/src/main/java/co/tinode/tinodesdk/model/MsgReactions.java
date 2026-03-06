package co.tinode.tinodesdk.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

@JsonInclude(NON_DEFAULT)
public class MsgReactions implements Serializable {
    public int seq;
    public MsgOneReaction[] reacts;

    public MsgReactions() {}

    public MsgReactions(int mrrid, MsgReactClient r) {
        this.seq = r.seq;
        reacts = new MsgOneReaction[1];
        reacts[0] = new MsgOneReaction(mrrid, r);
    }
}
