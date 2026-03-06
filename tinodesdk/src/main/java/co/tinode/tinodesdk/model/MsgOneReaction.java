package co.tinode.tinodesdk.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

// MsgOneReaction to message.
@JsonInclude(NON_DEFAULT)
public class MsgOneReaction implements Serializable {
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
