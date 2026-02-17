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

    public static class MsgReactClient {
        // Message ID
        public int seq;
        // Reaction content (emoji etc.).
        public String val;

        public MsgReactClient() {}

        public MsgReactClient(int seq, String val) {
            this.seq = seq;
            this.val = val;
        }
    }

    public static class Builder_remove<Pu,Pr> {
        private final MsgClientSet<Pu,Pr> msm;

        public Builder_remove(String id, String topic) {
            msm = new MsgClientSet<>(id, topic);
        }

        public void with(MetaSetDesc<Pu,Pr> desc) {
            msm.desc = desc;
            if (desc == null) {
                msm.nulls |= MsgSetMeta.NULL_DESC;
            }
        }

        public void with(MetaSetSub sub) {
            msm.sub = sub;
            if (sub == null) {
                msm.nulls |= MsgSetMeta.NULL_SUB;
            }
        }

        public void with(String[] tags) {
            msm.tags = tags;
            if (tags == null || tags.length == 0) {
                msm.nulls |= MsgSetMeta.NULL_TAGS;
            }
        }

        public void with(Credential cred) {
            msm.cred = cred;
            if (cred == null) {
                msm.nulls |= MsgSetMeta.NULL_CRED;
            }
        }

        public void with(Map<String,Object> aux) {
            msm.aux = aux;
            if (aux == null || aux.isEmpty()) {
                msm.nulls |= MsgSetMeta.NULL_AUX;
            }
        }

        public void with(int seq, String val) {
            msm.react = new MsgReactClient(seq, val);
        }

        public MsgClientSet<Pu,Pr> build() {
            return msm;
        }
    }
}
