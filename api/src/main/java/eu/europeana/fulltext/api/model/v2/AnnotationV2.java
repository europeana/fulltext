package eu.europeana.fulltext.api.model.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import eu.europeana.fulltext.api.model.AnnotationWrapper;
import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldType;

import java.io.Serializable;

/**
 * Created by luthien on 14/06/2018.
 */
//@JsonldType(value = "oa:Annotation") // commenting this out works for property ordering #EA-1310
@JsonPropertyOrder({"context", "id", "type", "motivation", "dcType", "resource", "on"})
public class AnnotationV2 extends JsonLdId implements Serializable, AnnotationWrapper {

    private static final long serialVersionUID = 7120324589144279826L;

    @JsonProperty("@context")
    // note that we only set context for a single annotation and not for an array of annotations part of an annotationpage
    private String[] context;

    @JsonProperty("@type")
    private String type = "oa:Annotation";

    private String              motivation;
    private String              dcType;
    private AnnotationBodyV2    resource;
    private String[]            on;

    public AnnotationV2(String id) {
        super(id);
    }

    public String[] getContext() {
        return context;
    }

    public void setContext(String[] context) {
        this.context = context;
    }

    public String getMotivation() {
        return motivation;
    }

    public String getDcType() {
        return dcType;
    }

    public void setDcType(String dcType) {
        this.dcType = dcType;
    }

    public void setMotivation(String motivation) {
        this.motivation = motivation;
    }

    public AnnotationBodyV2 getResource() {
        return resource;
    }

    public void setResource(AnnotationBodyV2 resource) {
        this.resource = resource;
    }

    public String[] getOn() {
        return on;
    }

    public void setOn(String[] on) {
        this.on = on;
    }
}

