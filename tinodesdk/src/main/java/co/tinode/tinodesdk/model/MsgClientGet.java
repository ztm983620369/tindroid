package co.tinode.tinodesdk.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

/**
 * Metadata query packet
 * 	Id    string `json:"id,omitempty"`
 *  Topic string `json:"topic"`
 *  What string `json:"what"`
 *  Desc *MsgGetOpts `json:"desc,omitempty"`
 *  Sub *MsgGetOpts `json:"sub,omitempty"`
 *  Data *MsgBrowseOpts `json:"data,omitempty"`
 *  React *MsgBrowseOpts `json:"react,omitempty"`
 */
@JsonInclude(NON_DEFAULT)
public class MsgClientGet implements Serializable {
    public String id;
    public String topic;
    public String what;

    public MetaGetDesc desc;
    public MetaGetSub sub;
    public MetaGetData data;
    public MetaGetData react;

    public MsgClientGet() {}

    public MsgClientGet(String id, String topic, MsgGetMeta query) {
        this.id = id;
        this.topic = topic;
        this.what = query.what;
        this.desc = query.desc;
        this.sub = query.sub;
        this.data = query.data;
        this.react = query.react;
    }
}
