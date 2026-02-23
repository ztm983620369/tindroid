package co.tinode.tinodesdk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

/**
 * Metadata update packet: description, subscription, tags, credentials.
 * Topic/user description, new object & new subscriptions only
 * - Desc *MsgSetDesc `json:"desc,omitempty"`
 * Subscription parameters
 * - Sub *MsgSetSub `json:"sub,omitempty"`
 * Indexable tags for user discovery
 * - Tags []string `json:"tags,omitempty"`
 * Update to account credentials.
 * - Cred *MsgCredClient `json:"cred,omitempty"`
 * Update auxiliary data
 * - Aux map[string]any
 * Add reaction to message
 * - React *MsgReactClient `json:"react,omitempty"`
 */
@JsonInclude(NON_DEFAULT)
public class MsgClientSet<Pu,Pr> implements Serializable {
    // Keep track of NULL assignments to fields.
    @JsonIgnore
    int nulls = 0;

    public String id;
    public String topic;

    public MetaSetDesc<Pu,Pr> desc;
    public MetaSetSub sub;
    public String[] tags;
    public Credential cred;
    public Map<String, Object> aux;
    public MsgReactClient react;

    public MsgClientSet() {}

    public MsgClientSet(String id, String topic, MsgSetMeta<Pu,Pr> meta) {
        this(id, topic, meta.desc, meta.sub, meta.tags, meta.cred, meta.aux, meta.react);
        nulls = meta.nulls;
    }

    protected MsgClientSet(String id, String topic) {
        this.id = id;
        this.topic = topic;
    }

    protected MsgClientSet(String id, String topic, MetaSetDesc<Pu, Pr> desc,
                           MetaSetSub sub, String[] tags, Credential cred,
                           Map<String, Object> aux, MsgReactClient react) {
        this.id = id;
        this.topic = topic;
        this.desc = desc;
        this.sub = sub;
        this.tags = tags;
        this.cred = cred;
        this.aux = aux;
        this.react = react;
    }
}
