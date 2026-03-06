package co.tinode.tinodesdk.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

@JsonInclude(NON_DEFAULT)
public class MsgReactClient implements Serializable {
    // Message ID
    public int seq;
    // Reaction content (emoji etc.).
    public String val;

    public MsgReactClient() {
    }

    public MsgReactClient(int seq, String val) {
        this.seq = seq;
        this.val = val;
    }
}
