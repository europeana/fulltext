/*
 * Copyright 2007-2018 The Europeana Foundation
 *
 *  Licenced under the EUPL, Version 1.1 (the "Licence") and subsequent versions as approved
 *  by the European Commission;
 *  You may not use this work except in compliance with the Licence.
 *
 *  You may obtain a copy of the Licence at:
 *  http://joinup.ec.europa.eu/software/page/eupl
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under
 *  the Licence is distributed on an "AS IS" basis, without warranties or conditions of
 *  any kind, either express or implied.
 *  See the Licence for the specific language governing permissions and limitations under
 *  the Licence.
 */

package eu.europeana.fulltext.api.service;

import eu.europeana.fulltext.api.config.FTSettings;
import eu.europeana.fulltext.api.entity.AnnoPage;
import eu.europeana.fulltext.api.entity.Annotation;
import eu.europeana.fulltext.api.entity.Resource;
import eu.europeana.fulltext.api.entity.Target;
import eu.europeana.fulltext.api.model.FullTextResource;
import eu.europeana.fulltext.api.model.v2.*;
import eu.europeana.fulltext.api.model.v3.AnnotationPageV3;
import eu.europeana.fulltext.api.model.v3.AnnotationV3;
import eu.europeana.fulltext.api.model.v3.AnnotationBodyV3;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Optional;

import static eu.europeana.fulltext.api.config.FTDefinitions.*;

/**
 * This class contains the methods for mapping Annotation / AnnoPage Mongo bean objects to IIIF v2 / v3 fulltext
 * resource objects.
 *
 * NOTE while a given value for the 'motivation' field in the Annotation class will be stored as such in Mongo, it is
 * for now not displayed in the output. Instead, the values as specified in the EDM 2 IIIF mapping document are used;
 * they are for now hard-coded in this class for both the V2 and V3 version of the output JSON.
 *
 * Created by luthien on 18/06/2018.
 */
@Component
public class EDM2IIIFMapping {

    private static FTSettings fts;

    private static final String V2_MOTIVATION = "sc:painting";
    private static final String V3_MOTIVATION = "transcribing";

    private static final Logger LOG = LogManager.getLogger(EDM2IIIFMapping.class);

    @Autowired
    private EDM2IIIFMapping(FTSettings fts) {
        EDM2IIIFMapping.fts = fts;
    }

    static AnnotationPageV2 getAnnotationPageV2(AnnoPage annoPage, boolean includeContext){
        AnnotationPageV2 annPage = new AnnotationPageV2(getAnnoPageIdUrl(annoPage), includeContext);
        annPage.setResources(getAnnotationV2Array(annoPage));
        return annPage;
    }

    private static AnnotationV2[] getAnnotationV2Array(AnnoPage annoPage){
        ArrayList<AnnotationV2> annoArrayList = new ArrayList<>();
        for (Annotation ftAnno : annoPage.getAns()){
            annoArrayList.add(getAnnotationV2(annoPage, ftAnno, false));
        }
        return annoArrayList.toArray(new AnnotationV2[0]);
    }

    private static AnnotationV2 getAnnotationV2(AnnoPage annoPage, Annotation annotation, boolean addContext){
        String       resourceIdUrl  = getResourceIdUrl(annoPage, annotation);
        AnnotationV2 ann            = new AnnotationV2(getAnnotationIdUrl(annoPage, annotation));
        if (addContext){
            ann.setContext(new String[]{MEDIA_TYPE_IIIF_V2, MEDIA_TYPE_EDM_JSONLD});
        }
        ann.setMotivation(StringUtils.isNotBlank(annotation.getMotiv()) ? annotation.getMotiv() : V2_MOTIVATION);
        ann.setDcType(expandDCType(annotation.getDcType()));
        ann.setOn(getFTTargetArray(annoPage, annotation));
        if (StringUtils.isNotBlank(annotation.getLang())){
            AnnotationFullBodyV2 anb = new AnnotationFullBodyV2(resourceIdUrl);
            anb.setFull(getResourceIdBaseUrl(annoPage));
            anb.setLanguage(annotation.getLang());
            ann.setResource(anb);
        } else {
            AnnotationBodyV2 anb = new AnnotationBodyV2(resourceIdUrl);
            ann.setResource(anb);
        }
        return ann;
    }

    static AnnotationPageV3 getAnnotationPageV3(AnnoPage annoPage, boolean includeContext){
        AnnotationPageV3 annPage = new AnnotationPageV3(getAnnoPageIdUrl(annoPage), includeContext);
        annPage.setItems(getAnnotationV3Array(annoPage));
        return annPage;
    }

    private static AnnotationV3[] getAnnotationV3Array(AnnoPage annoPage){
        ArrayList<AnnotationV3> annoArrayList = new ArrayList<>();
        for (Annotation ftAnno : annoPage.getAns()){
            annoArrayList.add(getAnnotationV3(annoPage, ftAnno, false));
        }
        return annoArrayList.toArray(new AnnotationV3[0]);
    }

    private static AnnotationV3 getAnnotationV3(AnnoPage annoPage, Annotation annotation, boolean addContext){
        String       body = getResourceIdUrl(annoPage, annotation);
        AnnotationV3 ann  = new AnnotationV3(getAnnotationIdUrl(annoPage, annotation));
        AnnotationBodyV3 anb;
        if (addContext){
            ann.setContext(new String[]{MEDIA_TYPE_IIIF_V3, MEDIA_TYPE_EDM_JSONLD});
        }
        ann.setMotivation(StringUtils.isNotBlank(annotation.getMotiv()) ? annotation.getMotiv() : V3_MOTIVATION);
        ann.setDcType(expandDCType(annotation.getDcType()));
        if (StringUtils.isNotBlank(annotation.getLang())){
            anb = new AnnotationBodyV3(body, V3_ANNO_BODY_TYPE);
            anb.setSource(getResourceIdBaseUrl(annoPage));
            anb.setLanguage(annotation.getLang());
        } else {
            anb = new AnnotationBodyV3(body);
        }
        ann.setBody(anb);
        ann.setTarget(getFTTargetArray(annoPage, annotation));
        return ann;
    }

    static AnnotationV3 getSingleAnnotationV3(AnnoPage annoPage, String annoId, boolean includeContext){
        Annotation           annotation;
        Optional<Annotation> maybe = annoPage.getAns().stream().filter(o -> o.getAnId().equals(annoId)).findFirst();
        // NOTE this shouldn't fail because in that case the annoPage would not have been found in the first place
        return maybe.map(annotation1 -> getAnnotationV3(annoPage, annotation1, includeContext)).orElse(null);
    }

    static AnnotationV2 getSingleAnnotationV2(AnnoPage annoPage, String annoId, boolean includeContext){
        Annotation           annotation;
        Optional<Annotation> maybe = annoPage.getAns().stream().filter(o -> o.getAnId().equals(annoId)).findFirst();
        // NOTE this shouldn't fail because in that case the annoPage would not have been found in the first place
        return maybe.map(annotation1 -> getAnnotationV2(annoPage, annotation1, includeContext)).orElse(null);
    }

    private static String[] getFTTargetArray(AnnoPage annoPage, Annotation annotation){
        ArrayList<String> ftTargetURLList = new ArrayList<>();
        for (Target target : annotation.getTgs()){
            ftTargetURLList.add(annoPage.getTgtId() + "#xywh="
                                + target.getX() + ","
                                + target.getY() + ","
                                + target.getW() + ","
                                + target.getH());
        }
        return ftTargetURLList.toArray(new String[0]);
    }

    static FullTextResource getFullTextResource(Resource resource, boolean includeContext){
        return new FullTextResource(fts.getResourceBaseUrl()
                                    + resource.getDsId() + "/"
                                    + resource.getLcId() + "/"
                                    + resource.getId(),
                                    resource.getLang(),
                                    resource.getValue(),
                                    includeContext);
    }

    private static String getResourceIdUrl(AnnoPage annoPage, Annotation annotation){
        return getResourceIdBaseUrl(annoPage) + "#char=" + annotation.getFrom() + "," + annotation.getTo();
    }

    private static String getResourceIdBaseUrl(AnnoPage annoPage){
        return fts.getResourceBaseUrl() + annoPage.getDsId() + "/" + annoPage.getLcId() + "/" + annoPage.getRes().getId();
    }

    private static String getAnnoPageIdUrl(AnnoPage annoPage){
        return fts.getAnnoPageBaseUrl() + annoPage.getDsId() + "/" +
               annoPage.getLcId() + fts.getAnnoPageDirectory() + annoPage.getPgId();
    }

    private static String getAnnotationIdUrl(AnnoPage annoPage, Annotation annotation){
        return fts.getAnnotationBaseUrl() + annoPage.getDsId() + "/" + annoPage.getLcId() + fts.getAnnotationDirectory() + annotation.getAnId();
    }

    private static String expandDCType(String dcTypeCode){
        String dcType = "";
        switch (StringUtils.upperCase(dcTypeCode)) {
            case "P":
                dcType = "Page";
                break;
            case "B":
                dcType = "Block";
                break;
            case "L":
                dcType = "Line";
                break;
            case "W":
                dcType = "Word";
                break;
            default:
                dcType = "undefined";
                break;
        }
        return dcType;
    }
}
