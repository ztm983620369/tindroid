package co.tinode.tinodesdk.model;

public class MsgReactClient {
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
