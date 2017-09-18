package com.ppm.integration.agilesdk.connector.octane.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by canaud on 9/9/2017.
 */
public class OctaneUtils {

    public static void sortReleases(List<Release> releases) {
        Collections.sort(releases, new Comparator<Release>() {
            @Override
            public int compare(Release o1, Release o2) {
                if (o1 == null || o1.getId() == null || o1.startDatetime == null) return -1;
                if (o2 == null || o2.getId() == null || o2.startDatetime == null) return 1;
                return o1.startDatetime.compareTo(o2.startDatetime);
            }
        });
    }

    public static List<Sprint> getReleaseSprints(String releaseId, List<Sprint> allSprints) {
        List<Sprint> sprints = new ArrayList<>();

        for (Sprint sprint : allSprints) {
            if (releaseId.equals(sprint.releaseId)) {
                sprints.add(sprint);
            }
        }

        sortSprints(sprints);

        return sprints;
    }

    public static void sortSprints(List<Sprint> sprints) {
        Collections.sort(sprints, new Comparator<Sprint>() {
            @Override
            public int compare(Sprint o1, Sprint o2) {
                if (o1 == null || o1.getId() == null || o1.sprintStart == null) return -1;
                if (o2 == null || o2.getId() == null || o2.sprintStart == null) return 1;
                return o1.sprintStart.compareTo(o2.sprintStart);
            }
        });
    }

    public static void sortWorkItemsByName(List<GenericWorkItem> workItems) {
        Collections.sort(workItems, new Comparator<GenericWorkItem>() {
            @Override
            public int compare(GenericWorkItem o1, GenericWorkItem o2) {
                if (o1 == null || o1.getName() == null) return -1;
                if (o2 == null || o2.getName() == null) return 1;
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
    }
}
