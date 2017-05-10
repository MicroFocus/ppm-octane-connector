package com.ppm.integration.agilesdk.connector.octane;

import com.ppm.integration.agilesdk.ValueSet;
import java.util.Random;

/**
 * Created by lutian on 2016/11/9.
 */
public class CommonParameters {
    private static ValueSet values = new ValueSet();

    public static ValueSet getDefaultValueSet() {
        return initValueSet();
    }

    private static ValueSet initValueSet() {
        values.put(OctaneConstants.KEY_BASE_URL, "http://myd-vm10632.hpeswlab.net:8081");
        values.put(OctaneConstants.KEY_USERNAME, "sa@nga");
        values.put(OctaneConstants.KEY_PASSWORD, "Welcome1");
        values.put(OctaneConstants.KEY_ENABLE_CSRF, "true");

        values.put(OctaneConstants.KEY_SHAREDSPACE, "default_shared_space");
        values.put(OctaneConstants.KEY_WORKSPACE, "default_workspace");
        values.put(OctaneConstants.KEY_RELEASE, "AOS");
        values.put(OctaneConstants.KEY_SHAREDSPACEID, "1001");
        values.put(OctaneConstants.KEY_WORKSPACEID, "1002");
        values.put(OctaneConstants.KEY_RELEASEID, "1002");

        values.put(OctaneConstants.KEY_PROXY_HOST, "web-proxy.sgp.hp.com");
        values.put(OctaneConstants.KEY_PROXY_PORT, "8080");

        values.put(OctaneConstants.APP_CLIENT_ID, "test0508_dwp7j1qyy9lw7i8v6d3e504mk");
        values.put(OctaneConstants.APP_CLIENT_SECRET, "$261432961c33569Q");
        
        values.put(OctaneConstants.KEY_EPIC_ENTITY_NAME, "epic name");
        values.put(OctaneConstants.KEY_EPIC_ENTITY_TYPE, "epic");
        values.put(OctaneConstants.KEY_WORKITEM_PARENT_ID, "1001");
        values.put(OctaneConstants.KEY_WORKITEM_PARENT_TYPE, "work_item_root");
        values.put(OctaneConstants.KEY_PHASE_LOGICNAME_ID, "1019");
        values.put(OctaneConstants.KEY_PHASE_LOGICNAME_TYPE, "phase");
        
        values.put(OctaneConstants.KEY_WORKITEM_SUBTYPE, "epic");
        values.put(OctaneConstants.KEY_PHASE_LOGICNAME, "phase.story.new");

        values.put(OctaneConstants.CREATE_EPIC_IN_WORKSPACE_RESPONSE_JSONDATA, "{\"total_count\":1,\"data\":[{\"type\":\"epic\",\"creation_time\":\"2017-05-09T07:47:31Z\",\"parent\":{\"type\":\"work_item\",\"subtype\":\"work_item_root\",\"name\":\"Backlog\",\"id\":\"1001\"},\"logical_name\":\"dwp7j12mekv68sn5oke8k04mk\",\"attachments\":{\"total_count\":0,\"data\":[]},\"actual_story_points\":null,\"version_stamp\":2,\"ordering\":null,\"description\":null,\"workspace_id\":1002,\"path\":\"0000000001BJ\",\"num_comments\":0,\"features\":0,\"wsjf_score\":null,\"item_origin\":null,\"children\":{\"total_count\":0,\"data\":[]},\"wsjf_cod\":null,\"items_in_releases\":\"\",\"committers\":null,\"feature_count\":null,\"id\":\"1549\",\"ancestors\":{\"total_count\":2,\"data\":[{\"type\":\"work_item\",\"subtype\":\"work_item_root\",\"name\":\"Backlog\",\"id\":\"1001\"},{\"type\":\"work_item\",\"parent\":{\"type\":\"work_item_root\",\"name\":\"Backlog\",\"id\":\"1001\"},\"subtype\":\"epic\",\"name\":\"epic name\",\"id\":\"1549\"}],\"exceeds_total_count\":false},\"last_modified\":\"2017-05-09T07:47:32Z\",\"logical_path\":\"Backlog\",\"owner\":null,\"phase\":{\"type\":\"phase\",\"logical_name\":\"phase.epic.new\",\"name\":\"New\",\"index\":0,\"id\":\"1019\"},\"rroe\":null,\"has_attachments\":false,\"has_children\":false,\"epic_type\":null,\"author\":{\"type\":\"workspace_user\",\"full_name\":\"test0508\",\"name\":\"test0508_dwp7j1qyy9lw7i8v6d3e504mk\",\"id\":\"1018\"},\"story_points\":null,\"global_text_search_result\":null,\"user_tags\":{\"total_count\":0,\"data\":[]},\"commit_count\":null,\"commit_files\":null,\"has_comments\":false,\"time_criticality\":null,\"name\":\"epic name\",\"job_size\":null,\"commits\":{\"total_count\":0,\"data\":[]},\"progress\":\"{\\\"themeId\\\":0,\\\"featuresCountDone\\\":0,\\\"featuresCountTotal\\\":0,\\\"storiesCountDone\\\":0,\\\"storiesCountTotal\\\":0,\\\"defectsCountDone\\\":0,\\\"defectsCountTotal\\\":0,\\\"storiesSumDone\\\":0,\\\"storiesSumTotal\\\":0,\\\"defectsSumDone\\\":0,\\\"defectsSumTotal\\\":0,\\\"plannedStoryPoints\\\":0}\",\"business_value\":null,\"original_id\":null}],\"exceeds_total_count\":false}");
        values.put(OctaneConstants.GET_EPIC_PHASE_JSONDATA, "{\"total_count\":1,\"data\":[{\"type\":\"phase\",\"logical_name\":\"phase.story.new\",\"id\":\"1027\"}],\"exceeds_total_count\":false}");
        values.put(OctaneConstants.GET_EPIC_PARENT_JSONDATA, "{\"total_count\":8,\"data\":[{\"type\":\"work_item\",\"logical_name\":\"odzwjg3ypwoonikv9z6k1r7ky\",\"subtype\":\"epic\",\"id\":\"1112\"},{\"type\":\"work_item\",\"logical_name\":\"6xk7j7p9vw72pcxw5lv3gjy89\",\"subtype\":\"epic\",\"id\":\"1111\"},{\"type\":\"work_item\",\"logical_name\":\"odzwjg9ynme31tgvne3x6r7ky\",\"subtype\":\"epic\",\"id\":\"1114\"},{\"type\":\"work_item\",\"logical_name\":\"z7eprp1donmn9b86ze7lpr6v3\",\"subtype\":\"epic\",\"id\":\"1115\"},{\"type\":\"work_item\",\"logical_name\":\"1qypr87zkz6k6tgxgy4oz0zwd\",\"subtype\":\"epic\",\"id\":\"1547\"},{\"type\":\"work_item\",\"logical_name\":\"z7eprpzp5gndmsw6dlezej6v3\",\"subtype\":\"epic\",\"id\":\"1548\"},{\"type\":\"work_item\",\"logical_name\":\"dwp7j12mekv68sn5oke8k04mk\",\"subtype\":\"epic\",\"id\":\"1549\"},{\"type\":\"work_item\",\"logical_name\":\"q6w2j689zdeeki4vpmzgqr3lx\",\"subtype\":\"epic\",\"id\":\"1113\"}],\"exceeds_total_count\":false}");
        return values;
    }

    public static String genRandomNumber(int digitNumber) {
        StringBuilder num = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < digitNumber; i++) {
            num.append(random.nextInt(10));
        }
        return num.toString();
    }
}
