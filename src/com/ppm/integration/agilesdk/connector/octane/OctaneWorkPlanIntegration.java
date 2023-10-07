package com.ppm.integration.agilesdk.connector.octane;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.ppm.integration.agilesdk.connector.jira.JIRAServiceProvider;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAAgileEntity;
import com.ppm.integration.agilesdk.connector.octane.model.*;
import com.ppm.integration.agilesdk.connector.octane.model.workplan.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.hp.ppm.integration.model.WorkplanMapping;
import com.hp.ppm.user.service.api.UserService;
import com.kintana.core.util.mlu.DateFormatter;
import com.mercury.itg.core.ContextFactory;
import com.mercury.itg.core.impl.SpringContainerFactory;
import com.mercury.itg.core.model.Context;
import com.mercury.itg.core.user.impl.UserImpl;
import com.ppm.integration.agilesdk.FunctionIntegration;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.client.OctaneClientHelper;
import com.ppm.integration.agilesdk.connector.octane.client.OctaneEntityDropdown;
import com.ppm.integration.agilesdk.pm.ExternalTask;
import com.ppm.integration.agilesdk.pm.ExternalWorkPlan;
import com.ppm.integration.agilesdk.pm.LinkedTaskAgileEntityInfo;
import com.ppm.integration.agilesdk.pm.WorkPlanIntegration;
import com.ppm.integration.agilesdk.pm.WorkPlanIntegrationContext;
import com.ppm.integration.agilesdk.provider.LocalizationProvider;
import com.ppm.integration.agilesdk.provider.Providers;
import com.ppm.integration.agilesdk.ui.CheckBox;
import com.ppm.integration.agilesdk.ui.DatePicker;
import com.ppm.integration.agilesdk.ui.DynamicDropdown;
import com.ppm.integration.agilesdk.ui.Field;
import com.ppm.integration.agilesdk.ui.FieldAppearance;
import com.ppm.integration.agilesdk.ui.LineBreaker;
import com.ppm.integration.agilesdk.ui.LineHr;
import com.ppm.integration.agilesdk.ui.Link;
import com.ppm.integration.agilesdk.ui.NumberText;
import com.ppm.integration.agilesdk.ui.PlainText;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

public class OctaneWorkPlanIntegration extends WorkPlanIntegration implements FunctionIntegration {


    private final Logger logger = Logger.getLogger(this.getClass());

    protected static final UserService userService = ((UserService) SpringContainerFactory.getBean("userAdminService"));
    
    @Override public List<Field> getMappingConfigurationFields(WorkPlanIntegrationContext context, ValueSet values) {

        GregorianCalendar start = context.currentTask().getSchedule().getScheduledStart().toGregorianCalendar();
        GregorianCalendar finish = context.currentTask().getSchedule().getScheduledEnd().toGregorianCalendar();

        final LocalizationProvider l10n = Providers.getLocalizationProvider(OctaneIntegrationConnector.class);

        return Arrays.asList(new Field[] {
        		new Link("auth", "TASK_MAPPING_AUTHENTICATION_LINK", "defaultValue", "block", true, "javascript:void(0);",
                        "openSSOLink()"),
        		
        		new PlainText(OctaneConstants.KEY_SSO_COOKIE,"SSO_C","","none",true),
                new LineBreaker(),
                
                new OctaneEntityDropdown(OctaneConstants.KEY_SHAREDSPACEID, "OCTANE_SHARESPACE", "block", true) {
                    @Override public List<String> getDependencies() {
                        return Arrays.asList(new String[] {OctaneConstants.KEY_BASE_URL, OctaneConstants.APP_CLIENT_ID,
                                OctaneConstants.APP_CLIENT_SECRET, OctaneConstants.KEY_SSO_COOKIE});
                    }

                    @Override public List<Option> fetchDynamicalOptions(ValueSet values) {
                        ClientPublicAPI client = ClientPublicAPI.getClient(values);                        
                        if (!values.isAllSet(OctaneConstants.APP_CLIENT_ID, OctaneConstants.APP_CLIENT_SECRET,
                                OctaneConstants.KEY_BASE_URL, OctaneConstants.KEY_SSO_COOKIE)) {
                            return null;
                        }
                        
                        List<SharedSpace> sharedSpaces;
                        try {
                        	String cookies = values.get(OctaneConstants.KEY_SSO_COOKIE);
                        	if (cookies != null && !cookies.isEmpty()) {
                        		client.setSSOCookies(cookies);
                        	}
                            sharedSpaces = client.getSharedSpaces();
                            List<Option> options = new ArrayList<Option>(sharedSpaces.size());
                            for (SharedSpace sd : sharedSpaces) {
                                options.add(new Option(sd.id, sd.name));
                            }
                            client.signOut(values);
                            return options;
                        } catch (Exception e) {
                            logger.error("Error occured when getting Mapping config fields, returning null", e);
                        }
                        return null;
                    }
                },
                new OctaneEntityDropdown(OctaneConstants.KEY_WORKSPACEID, "OCTANE_WORKSPACE", "block", true) {
                    @Override public List<String> getDependencies() {
                        return Arrays.asList(new String[] {OctaneConstants.KEY_BASE_URL, OctaneConstants.APP_CLIENT_ID,
                                OctaneConstants.APP_CLIENT_SECRET, OctaneConstants.KEY_SHAREDSPACEID, OctaneConstants.KEY_SSO_COOKIE});
                    }

                    @Override public List<Option> fetchDynamicalOptions(ValueSet values) {

                        ClientPublicAPI client = ClientPublicAPI.getClient(values);
                        if (!values.isAllSet(OctaneConstants.KEY_SHAREDSPACEID)) {
                            return null;
                        }
                        List<WorkSpace> workSpaces;
                        try {
                        	String cookies = values.get(OctaneConstants.KEY_SSO_COOKIE);
                        	if (cookies != null && !cookies.isEmpty()) {
                        		client.setSSOCookies(cookies);
                        	}
                            workSpaces = client.getWorkSpaces(Integer.parseInt(values.get(OctaneConstants.KEY_SHAREDSPACEID)));
                            List<Option> options = new ArrayList<Option>(workSpaces.size());
                            for (WorkSpace w : workSpaces) {
                                options.add(new Option(w.id, w.name));
                            }
                            client.signOut(values);
                            return options;
                        } catch (Exception e) {
                            logger.error("Error occured when getting Mapping config fields, returning null", e);
                        }
                        return null;

                    }
                },

                new LineBreaker(),
                new LineHr(),
                new LineBreaker(),


                new DynamicDropdown(OctaneConstants.KEY_IMPORT_SELECTION, "IMPORT_SELECTION",
                        OctaneConstants.IMPORT_SELECTION_RELEASE, "", false) {

                    @Override
                    public List<String> getDependencies() {
                        return new ArrayList<String>();
                    }

                    @Override
                    public List<String> getStyleDependencies() {
                        return Arrays.asList(new String[] { OctaneConstants.KEY_IS_CREATE_RELEASE });
                    }

                    @Override
                    public FieldAppearance getFieldAppearance(ValueSet values) {
                        return getCreateReleaseImportSelectionFieldsAppearance(values);
                    }

                    @Override
                    public List<Option> getDynamicalOptions(ValueSet values) {

                        List<Option> optionList = new ArrayList<>();

                        Option option0 = new Option(OctaneConstants.IMPORT_SELECTION_FEATURE, l10n.getConnectorText("IMPORT_SELECTION_FEATURE"));
                        Option option1 = new Option(OctaneConstants.IMPORT_SELECTION_EPIC, l10n.getConnectorText("IMPORT_SELECTION_EPIC"));
                        Option option2 = new Option(OctaneConstants.IMPORT_SELECTION_RELEASE, l10n.getConnectorText("IMPORT_SELECTION_RELEASE"));

                        optionList.add(option0);
                        optionList.add(option1);
                        optionList.add(option2);

                        return optionList;
                    }

                },
                new DynamicDropdown(OctaneConstants.KEY_IMPORT_SELECTION_DETAILS, "IMPORT_SELECTION_DETAILS", "", false) {

                    @Override
                    public List<String> getDependencies() {

                        return Arrays.asList(
                                new String[] {OctaneConstants.KEY_SHAREDSPACEID, OctaneConstants.KEY_WORKSPACEID, OctaneConstants.KEY_IMPORT_SELECTION, OctaneConstants.KEY_SSO_COOKIE});
                    }

                    @Override
                    public FieldAppearance getFieldAppearance(ValueSet values) {
                        return getCreateReleaseImportSelectionFieldsAppearance(values);
                    }

                    @Override
                    public List<String> getStyleDependencies() {
                        return Arrays.asList(new String[] { OctaneConstants.KEY_IS_CREATE_RELEASE });
                    }

                    @Override
                    public List<Option> getDynamicalOptions(ValueSet values) {

                        String importSelection = values.get(OctaneConstants.KEY_IMPORT_SELECTION);

                        ClientPublicAPI client = ClientPublicAPI.getClient(values);
                        
                        String cookies = values.get(OctaneConstants.KEY_SSO_COOKIE);
                    	if (cookies != null && !cookies.isEmpty()) {
                    		client.setSSOCookies(cookies);
                    	}                       

                        List<Option> options = new ArrayList<>();
                        switch (importSelection) {
                            case OctaneConstants.IMPORT_SELECTION_FEATURE:
                                List<SimpleEntity> features =
                                        client.getAllFeatures();
                                options = new ArrayList<Option>(features.size());
                                for (SimpleEntity feature : features) {
                                    options.add(new Option(feature.getId(), feature.getName() + " (" + feature.getId() + ")"));
                                }
                                return options;
                            case OctaneConstants.IMPORT_SELECTION_EPIC:
                                List<EpicAttr> epics =
                                        client.getAllEpics();
                                options = new ArrayList<Option>(epics.size());
                                for (EpicAttr epic : epics) {
                                    options.add(new Option(epic.getId(), epic.getName() + " (" + epic.getId() + ")"));
                                }
                                return options;
                            case OctaneConstants.IMPORT_SELECTION_RELEASE:
                                List<Release> releases =
                                        client.getAllReleases();
                                options = new ArrayList<Option>(releases.size());
                                for (Release r : releases) {
                                    options.add(new Option(r.id, r.name));
                                }
                                return options;
                        }
                        client.signOut(values);
                        return options;
                    }
                },

                new LineBreaker(),
                new LineHr(),
                new LineBreaker(),

                // Release Creation
                new CheckBox(OctaneConstants.KEY_IS_CREATE_RELEASE,"IS_CREATE_RELEASE","block",false),
                new LineBreaker(),
                new PlainText(OctaneConstants.KEY_NEW_RELEASE_NAME,"NEW_RELEASE_NAME",context.currentTask().getName(),"block",false)
                {
                    @Override
                    public List<String> getStyleDependencies() {
                        return Arrays.asList(new String[] { OctaneConstants.KEY_IS_CREATE_RELEASE });
                    }

                    @Override
                    public FieldAppearance getFieldAppearance(ValueSet values) {
                        return getCreateReleaseRequiredFieldsAppearance(values);

                    }

                },
                new PlainText(OctaneConstants.KEY_NEW_RELEASE_DESCRIPTION,"NEW_RELEASE_DESCRIPTION","","block",false)
                {
                    @Override
                    public List<String> getStyleDependencies() {
                        return Arrays.asList(new String[] { OctaneConstants.KEY_IS_CREATE_RELEASE });
                    }

                    @Override
                    public FieldAppearance getFieldAppearance(ValueSet values) {
                        return getCreateReleaseNotRequiredFieldsAppearance(values);

                    }
                },
                new LineBreaker(),
                new DatePicker(OctaneConstants.KEY_NEW_RELEASE_START_DATE,"NEW_RELEASE_START_DATE",new SimpleDateFormat(getUserDateformat()).format(start.getTime()),"block",false)
                {
                    @Override
                    public List<String> getStyleDependencies() {
                        return Arrays.asList(new String[] { OctaneConstants.KEY_IS_CREATE_RELEASE });
                    }

                    @Override
                    public FieldAppearance getFieldAppearance(ValueSet values) {
                        return getCreateReleaseRequiredFieldsAppearance(values);

                    }
                },
                new DatePicker(OctaneConstants.KEY_NEW_RELEASE_END_DATE,"NEW_RELEASE_END_DATE",new SimpleDateFormat(getUserDateformat()).format(finish.getTime()),"block",false)
                {
                    @Override
                    public List<String> getStyleDependencies() {
                        return Arrays.asList(new String[] { OctaneConstants.KEY_IS_CREATE_RELEASE });
                    }

                    @Override
                    public FieldAppearance getFieldAppearance(ValueSet values) {
                        return getCreateReleaseRequiredFieldsAppearance(values);

                    }
                },
                new DynamicDropdown(OctaneConstants.KEY_NEW_RELEASE_SCRUM_KANBAN,"KEY_NEW_RELEASE_SCRUM_KANBAN",OctaneConstants.NEW_RELEASE_SCRUM, "", false)
                {
                    @Override
                    public List<String> getDependencies() {
                        return new ArrayList<String>();
                    }

                    @Override
                    public List<String> getStyleDependencies() {
                        return Arrays.asList(new String[] { OctaneConstants.KEY_IS_CREATE_RELEASE });
                    }

                    @Override
                    public FieldAppearance getFieldAppearance(ValueSet values) {
                        return getCreateReleaseRequiredFieldsAppearance(values);
                    }

                    @Override
                    public List<Option> getDynamicalOptions(ValueSet values) {

                        List<Option> optionList = new ArrayList<>();

                        Option option1 = new Option(OctaneConstants.NEW_RELEASE_SCRUM, l10n.getConnectorText("NEW_RELEASE_SCRUM"));
                        Option option2 = new Option(OctaneConstants.NEW_RELEASE_KANBAN, l10n.getConnectorText("NEW_RELEASE_KANBAN"));

                        optionList.add(option1);
                        optionList.add(option2);

                        return optionList;
                    }
                },
                new NumberText(OctaneConstants.KEY_NEW_RELEASE_SPRINT_DURATION,"NEW_RELEASE_SPRINT_DURATION","14","block",false)
                {
                    @Override
                    public List<String> getStyleDependencies() {
                        return Arrays.asList(new String[] { OctaneConstants.KEY_IS_CREATE_RELEASE, OctaneConstants.KEY_NEW_RELEASE_SCRUM_KANBAN});
                    }

                    @Override
                    public FieldAppearance getFieldAppearance(ValueSet values) {

                        String releaseType = values.get(OctaneConstants.KEY_NEW_RELEASE_SCRUM_KANBAN);
                        if (OctaneConstants.NEW_RELEASE_SCRUM.equals(releaseType)) {
                            return getCreateReleaseRequiredFieldsAppearance(values);
                        } else  {
                            // If we create use Kanban release, this field is always disabled.
                            return new FieldAppearance("disabled", "required");
                        }
                    }
                },


                new LineBreaker(),
                new LineHr(),
                new LineBreaker(),

                new DynamicDropdown(OctaneConstants.KEY_IMPORT_GROUPS, "IMPORT_GROUPS",
                        OctaneConstants.GROUP_BACKLOG_STRUCTURE, "", true) {

                    @Override
                    public List<String> getDependencies() {
                        return new ArrayList<String>();
                    }


                    @Override
                    public List<Option> getDynamicalOptions(ValueSet values) {

                        List<Option> optionList = new ArrayList<>();

                        Option option1 = new Option(OctaneConstants.GROUP_RELEASE, l10n.getConnectorText("GROUP_RELEASE"));
                        Option option2 = new Option(OctaneConstants.GROUP_BACKLOG_STRUCTURE, l10n.getConnectorText("GROUP_BACKLOG_STRUCTURE"));

                        optionList.add(option1);
                        optionList.add(option2);

                        return optionList;
                    }

                },
                new DynamicDropdown(OctaneConstants.KEY_PERCENT_COMPLETE, "PERCENT_COMPLETE_CHOICE",
                        OctaneConstants.PERCENT_COMPLETE_STORY_POINTS, "", true) {

                    @Override
                    public List<String> getDependencies() {
                        return new ArrayList<String>();
                    }

                    @Override
                    public List<Option> getDynamicalOptions(ValueSet values) {

                        List<Option> optionList = new ArrayList<>();

                        Option option1 = new Option(OctaneConstants.PERCENT_COMPLETE_WORK, l10n.getConnectorText("PERCENT_COMPLETE_WORK"));
                        Option option2 = new Option(OctaneConstants.PERCENT_COMPLETE_STORY_POINTS, l10n.getConnectorText("PERCENT_COMPLETE_STORY_POINTS"));
                        Option option3 = new Option(OctaneConstants.PERCENT_COMPLETE_ITEMS_COUNT, l10n.getConnectorText("PERCENT_COMPLETE_ITEMS_COUNT"));


                        optionList.add(option1);
                        optionList.add(option2);
                        optionList.add(option3);

                        return optionList;
                    }

                },

                new DynamicDropdown(OctaneConstants.KEY_EFFORT_IMPORT, "EFFORT_IMPORT_CHOICE",
                        OctaneConstants.EFFORT_GROUP_IN_WORKITEM_OWNER, "", true) {

                    @Override
                    public List<String> getDependencies() {
                        return new ArrayList<String>();
                    }

                    @Override
                    public List<Option> getDynamicalOptions(ValueSet values) {

                        List<Option> optionList = new ArrayList<>();

                        Option option1 = new Option(OctaneConstants.EFFORT_GROUP_IN_WORKITEM_OWNER, l10n.getConnectorText("EFFORT_GROUP_IN_WORKITEM_OWNER"));
                        Option option2 = new Option(OctaneConstants.EFFORT_GROUP_IN_WORKITEM_TASKS_OWNERS, l10n.getConnectorText("EFFORT_GROUP_IN_WORKITEM_TASKS_OWNERS"));
                        Option option3 = new Option(OctaneConstants.EFFORT_NO_IMPORT, l10n.getConnectorText("EFFORT_NO_IMPORT"));


                        optionList.add(option1);
                        optionList.add(option2);
                        optionList.add(option3);

                        return optionList;
                    }

                },


                new LineBreaker(),
                new CheckBox(OctaneConstants.KEY_IMPORT_ITEM_STORIES,"IMPORT_ITEM_STORIES","block",true),
                new CheckBox(OctaneConstants.KEY_IMPORT_ITEM_DEFECTS,"IMPORT_ITEM_DEFECTS","block",true),
                new CheckBox(OctaneConstants.KEY_IMPORT_ITEM_QUALITY_STORIES,"IMPORT_ITEM_QUALITY_STORIES","block",false),
                new LineBreaker(),
                new CheckBox(OctaneConstants.KEY_SHOW_ITEMS_AS_TASKS,"SHOW_ITEMS_AS_TASKS","block",true),
                new LineBreaker()
        });
    }

    private String getUserDateformat(){
        try {
            Context ctx = ContextFactory.getThreadContext();
            UserImpl currentUser = (UserImpl)ctx.get(Context.USER);
            Long userID=currentUser.getUserId();
            if(userID!=null){
                return userService.findUserRegionalById(userID.intValue()).getShortDateFormat();
            }
        } catch (Exception e) {
            logger.error("Create new Release getUserDateformt fail:", e);
        }
        return null;
    }

    private FieldAppearance getCreateReleaseRequiredFieldsAppearance(ValueSet values)
    {
        String isCreateRelease = values.get(OctaneConstants.KEY_IS_CREATE_RELEASE);
        if ("false".equals(isCreateRelease)) {
            return new FieldAppearance("disabled", "required");
        } else if ("true".equals(isCreateRelease)) {
            return new FieldAppearance("required", "disabled");
        }

        return null;

    }

    private FieldAppearance getCreateReleaseNotRequiredFieldsAppearance(ValueSet values)
    {
        String isCreateRelease = values.get(OctaneConstants.KEY_IS_CREATE_RELEASE);
        if ("false".equals(isCreateRelease)) {
            return new FieldAppearance("disabled", "");
        } else if ("true".equals(isCreateRelease)) {
            return new FieldAppearance("", "disabled");
        }

        return null;
    }

    private FieldAppearance getCreateReleaseImportSelectionFieldsAppearance(ValueSet values)
    {
        String isCreateRelease = values.get(OctaneConstants.KEY_IS_CREATE_RELEASE);
        if ("false".equals(isCreateRelease)) {
            return new FieldAppearance("required", "disabled");
        } else if ("true".equals(isCreateRelease)) {
            return new FieldAppearance("disabled", "required");
        }

        return null;
    }


    @Override
    public WorkplanMapping linkTaskWithExternal(WorkPlanIntegrationContext context, WorkplanMapping workplanMapping, ValueSet values) {

        // If needed, We must create the new release in Octane and update the configuration with the ID.

        boolean isCreateRelease = values.getBoolean(OctaneConstants.KEY_IS_CREATE_RELEASE, false);

        if (isCreateRelease) {

            // Let's create the new release in Octane and update the config with new release info.
            ClientPublicAPI client = ClientPublicAPI.getClient(values);

            Date startDate, endDate = null;

            try {
                startDate = DateFormatter.parseDateTime(values.get(OctaneConstants.KEY_NEW_RELEASE_START_DATE));
                endDate = DateFormatter.parseDateTime(values.get(OctaneConstants.KEY_NEW_RELEASE_END_DATE));
            } catch (ParseException e) {
                throw new RuntimeException("Error while parsing date value", e);
            }

            Integer daysPerSprint = (StringUtils.isBlank(values.get(OctaneConstants.KEY_NEW_RELEASE_SPRINT_DURATION)) ? null : Integer.parseInt(values.get(OctaneConstants.KEY_NEW_RELEASE_SPRINT_DURATION)));

            if (OctaneConstants.NEW_RELEASE_KANBAN.equals(values.get(OctaneConstants.KEY_NEW_RELEASE_SCRUM_KANBAN))) {
                // We actually have to set this value to null to create a Kanban release.
                daysPerSprint = null;
            } else {
                if (daysPerSprint == null || daysPerSprint.intValue() < 1) {
                    // If we create a Scrum release, we have to pass a value for the number of days per sprint.
                    daysPerSprint = new Integer(14);
                }
            }

            Release release = client.createRelease(values.get(OctaneConstants.KEY_NEW_RELEASE_NAME), values.get(OctaneConstants.KEY_NEW_RELEASE_DESCRIPTION), startDate, endDate, daysPerSprint);
            client.signOut(values);
            updateNewReleaseInformationInWorkplanMapping(workplanMapping, release);

        } else {
            removeNewReleaseInfoFromWorkplanMapping(workplanMapping);
        }

        trimLongTextFieldsFromWorkplanMapping(workplanMapping);

        return workplanMapping;
    }

    /**
     * If some text fields are too long (such as description), it'll exceed the VARCHAR2(4000) size  of config JSON when stored to PPMIC_WORKPLAN_MAPPING.
     * So we need to truncate text fields that are potentially long.
     * This will not affect the created release in Octane, since truncation occurs AFTER the release is created.
     */
    private void trimLongTextFieldsFromWorkplanMapping(WorkplanMapping workplanMapping) {

        // Update the display config JSon
        String displayConfigJson = workplanMapping.getConfigDisplayJson();
        if(displayConfigJson != null) {
            JSONObject json = (JSONObject)JSONSerializer.toJSON(displayConfigJson);
            JSONArray oldConfig = json.getJSONArray("config");
            JSONArray newConfig = new JSONArray();
            for (int i = 0; i < oldConfig.size(); i++) {
                JSONObject entry = oldConfig.getJSONObject(i);
                String label = entry.getString("label");

                if (OctaneConstants.KEY_NEW_RELEASE_NAME.equalsIgnoreCase(label)
                        || OctaneConstants.KEY_NEW_RELEASE_DESCRIPTION.equalsIgnoreCase(label)) {
                    String text = trim(entry.getString("text"));
                    entry.put("text", text);
                }

                newConfig.add(entry);
            }

            json.put("config", newConfig);
            workplanMapping.setConfigDisplayJson(json.toString());
        }

        // Update the real config JSon
        String configJson = workplanMapping.getConfigJson();
        if(configJson != null) {
            JSONObject json = (JSONObject) JSONSerializer.toJSON(configJson);
            json.put(OctaneConstants.KEY_NEW_RELEASE_NAME, trim(json.getString(OctaneConstants.KEY_NEW_RELEASE_NAME)));
            json.put(OctaneConstants.KEY_NEW_RELEASE_DESCRIPTION, trim(json.getString(OctaneConstants.KEY_NEW_RELEASE_DESCRIPTION)));
            workplanMapping.setConfigJson(json.toString());
        }
    }


    private String trim(String text) {
        if (text == null) {
            return null;
        }

        if (text.length() > 50) {
            return text.substring(0, 47) + "...";
        }

        return text;
    }

    /**
     * This method is called if we didn't created a new release upon sync - it will remove all "new release" fields from the info recap.
     */
    private void removeNewReleaseInfoFromWorkplanMapping(WorkplanMapping workplanMapping) {
        String displayConfigJson = workplanMapping.getConfigDisplayJson();
        if(displayConfigJson != null) {
            JSONObject json = (JSONObject) JSONSerializer.toJSON(displayConfigJson);
            JSONArray oldConfig = json.getJSONArray("config");
            JSONArray newConfig = new JSONArray();
            for (int i = 0 ; i < oldConfig.size() ; i++) {
                JSONObject entry = oldConfig.getJSONObject(i);
                String label = entry.getString("label");

                if (OctaneConstants.KEY_NEW_RELEASE_START_DATE.equalsIgnoreCase(label)
                        || OctaneConstants.KEY_NEW_RELEASE_START_DATE.equalsIgnoreCase(label)
                        || OctaneConstants.KEY_NEW_RELEASE_END_DATE.equalsIgnoreCase(label)
                        || OctaneConstants.KEY_NEW_RELEASE_SPRINT_DURATION.equalsIgnoreCase(label)
                        || OctaneConstants.KEY_NEW_RELEASE_NAME.equalsIgnoreCase(label)
                        || OctaneConstants.KEY_IS_CREATE_RELEASE.equalsIgnoreCase(label)
                        || OctaneConstants.KEY_NEW_RELEASE_DESCRIPTION.equalsIgnoreCase(label)) {
                    continue;
                }

                newConfig.add(entry);
            }

            json.put("config", newConfig);
            workplanMapping.setConfigDisplayJson(json.toString());
        }
    }

    private void updateNewReleaseInformationInWorkplanMapping(WorkplanMapping workplanMapping, Release newRelease) {

        LocalizationProvider l10n = Providers.getLocalizationProvider(OctaneIntegrationConnector.class);

        //update mapping Release in ConfigJson & ConfigDisplayJson: We must select the newly created release info.
        String configJson = workplanMapping.getConfigJson();
        if(configJson != null) {
            JSONObject json = (JSONObject) JSONSerializer.toJSON(configJson);
            json.put(OctaneConstants.KEY_IMPORT_SELECTION, OctaneConstants.IMPORT_SELECTION_RELEASE);
            json.put(OctaneConstants.KEY_IMPORT_SELECTION_DETAILS, newRelease.getId());
            workplanMapping.setConfigJson(json.toString());
        }
        String displayConfigJson = workplanMapping.getConfigDisplayJson();
        if(displayConfigJson != null) {
            JSONObject json = (JSONObject) JSONSerializer.toJSON(displayConfigJson);
            JSONArray config = json.getJSONArray("config");
            for (int i = 0 ; i < config.size() ; i++) {
                JSONObject entry = config.getJSONObject(i);
                String label = entry.getString("label");
                if (OctaneConstants.KEY_IMPORT_SELECTION.equalsIgnoreCase(label)) {
                    entry.put("text", l10n.getConnectorText("IMPORT_SELECTION_RELEASE"));
                } else if (OctaneConstants.KEY_IMPORT_SELECTION_DETAILS.equalsIgnoreCase(label)) {
                    entry.put("text", newRelease.getId());
                } else if (OctaneConstants.KEY_NEW_RELEASE_NAME.equalsIgnoreCase(label)) {
                    entry.put("text", newRelease.getName());
                }
            }
            workplanMapping.setConfigDisplayJson(json.toString());
        }
    }

    @Override
    public ExternalWorkPlan getExternalWorkPlan(WorkPlanIntegrationContext wpiContext, ValueSet values) {

        ClientPublicAPI client = ClientPublicAPI.getClient(values);

        setBackwardCompatibleParameters(values);

        WorkplanContext wpContext = new WorkplanContext();

        wpContext.wpiContext = wpiContext;

        wpContext.percentComplete = values.get(OctaneConstants.KEY_PERCENT_COMPLETE);

        wpContext.phases = client.getAllPhases();

        wpContext.usersEmails = client.getAllWorkspaceUsers();

        wpContext.showItemsAsTasks = values.getBoolean(OctaneConstants.KEY_SHOW_ITEMS_AS_TASKS, false);

        wpContext.effortMode = values.get(OctaneConstants.KEY_EFFORT_IMPORT);


        // Get the backlog data. It's either one Epic or one Release

        final List<GenericWorkItem> workItems = new ArrayList<>();

        Set<String> itemTypes = new HashSet<>(3);

        if (values.getBoolean(OctaneConstants.KEY_IMPORT_ITEM_STORIES, false)) {
            itemTypes.add("^story^");
        }
        if (values.getBoolean(OctaneConstants.KEY_IMPORT_ITEM_DEFECTS, false)) {
            itemTypes.add("^defect^");
        }
        if (values.getBoolean(OctaneConstants.KEY_IMPORT_ITEM_QUALITY_STORIES, false)) {
            itemTypes.add("^quality_story^");
        }

        if (OctaneConstants.GROUP_BACKLOG_STRUCTURE.equals(values.get(OctaneConstants.KEY_IMPORT_GROUPS))) {
            itemTypes.add("^feature^");
            itemTypes.add("^epic^");
        }


        switch(values.get(OctaneConstants.KEY_IMPORT_SELECTION)) {

            case OctaneConstants.IMPORT_SELECTION_RELEASE:
                workItems.addAll(client.getReleaseWorkItems(Integer.parseInt(values.get(OctaneConstants.KEY_IMPORT_SELECTION_DETAILS)), itemTypes));
                break;

            case OctaneConstants.IMPORT_SELECTION_EPIC:
                workItems.addAll(client.getEpicWorkItems(Integer.parseInt(values.get(OctaneConstants.KEY_IMPORT_SELECTION_DETAILS)), itemTypes));
                break;

            case OctaneConstants.IMPORT_SELECTION_FEATURE:
                workItems.addAll(client.getFeatureWorkItems(Integer.parseInt(values.get(OctaneConstants.KEY_IMPORT_SELECTION_DETAILS)), itemTypes));
                break;
        }

        switch (values.get(OctaneConstants.KEY_EFFORT_IMPORT)) {
            case OctaneConstants.EFFORT_NO_IMPORT:
                // We should not include any effort information.
                workItems.stream().filter(wi -> wi.isDefectOrStory()).forEach(wi -> wi.removeEffort());
                break;
            case OctaneConstants.EFFORT_GROUP_IN_WORKITEM_TASKS_OWNERS:
                // We should first remove all effort from work items, as we'll get the effort through the Tasks.
                workItems.stream().filter(wi -> wi.isDefectOrStory()).forEach(wi -> wi.removeEffort());
                // We now load tasks for all the defect/stories work items -
                // how exactly they will be represented in the work plan will be handled in logic of External tasks, based on context.
                loadWorkItemsTasks(workItems.stream().filter(wi -> wi.isDefectOrStory()).collect(Collectors.toList()), client, wpContext);
                break;
            default: // case OctaneConstants.EFFORT_GROUP_IN_WORKITEM_OWNER:
                // There is nothing special to do, hours are already in the work items and will be reported as the work item owner.
                break;
        }


        final List<ExternalTask> rootTasks = new ArrayList<>();

        List<Sprint> sprints = client.getAllSprints();

        wpContext.setSprints(sprints);

        switch(values.get(OctaneConstants.KEY_IMPORT_GROUPS)) {
            case OctaneConstants.GROUP_RELEASE:

                // Group by Release / Sprint / Type

                List<Release> releases = client.getAllReleases();

                Map<String,Release> releasesMap = new HashMap<>();

                for (Release release : releases) {
                    releasesMap.put(release.getId(), release);
                }

                Map<String,Sprint> sprintsMap = new HashMap<>();

                for (Sprint sprint : sprints) {
                    sprintsMap.put(sprint.getId(), sprint);
                }

                // We're building a data structure that will mimic work plan, minus work item types (we'll do that later).
                final Map<String, Map<String, List<GenericWorkItem>>> workItemsPerReleaseIdAndSprintId = new HashMap<>();

                for (GenericWorkItem wi : workItems) {

                    if (!wi.isDefectOrStory()) {
                        // We don't want to keep epics and features in the release backlog.
                        // Also, since they don't have Release (Epic)  or Sprint (Epic/Feature) set, they'll create
                        // No Sprint/No Release tasks in WP even when not needed.
                        continue;
                    }

                    String releaseId = wi.getReleaseId();
                    String sprintId = wi.getSprintId();

                    Map<String, List<GenericWorkItem>> sprintWorkItems = workItemsPerReleaseIdAndSprintId.get(releaseId);

                    if (sprintWorkItems == null) {
                        sprintWorkItems = new HashMap<>();
                        workItemsPerReleaseIdAndSprintId.put(releaseId, sprintWorkItems);
                    }

                    List<GenericWorkItem> itemsInSprint = sprintWorkItems.get(sprintId);

                    if (itemsInSprint == null) {
                        itemsInSprint = new ArrayList<>();
                        sprintWorkItems.put(sprintId, itemsInSprint);
                    }

                    itemsInSprint.add(wi);
                }

                // First level is Release
                List <Release> sortedReleases = getSortedReleases(workItemsPerReleaseIdAndSprintId.keySet(), releasesMap);

                for (Release release : sortedReleases) {
                    rootTasks.add(WorkDrivenPercentCompleteExternalTask.forSummaryTask(new OctaneReleaseExternalTask(
                            release, workItemsPerReleaseIdAndSprintId.get(release.getId()), wpContext)));
                }

                break;
            case OctaneConstants.GROUP_BACKLOG_STRUCTURE:

                // Group by Backlog / Epic / Feature / Type

                // We need to first retrieve the missing Epics & Features, if any.
                Set<String> retrievedIds = new HashSet<>();
                for (GenericWorkItem wi : workItems) {
                    retrievedIds.add(wi.getId());
                }

                Set<String> missingIds = new HashSet<>();
                for (GenericWorkItem wi : workItems) {
                    if (!retrievedIds.contains(wi.getParentId()) && !wi.isInBacklog()) {
                        missingIds.add(wi.getParentId());
                    }
                }

                workItems.addAll(client.getWorkItemsByIds(missingIds));

                Map<String, List<GenericWorkItem>> epicsFeatures = new HashMap<>();

                Map<String, List<GenericWorkItem>> featuresItems = new HashMap<>();

                List<GenericWorkItem> featuresInBacklog = new ArrayList<>();

                List<GenericWorkItem> itemsInBacklog = new ArrayList<>();

                List<GenericWorkItem> epics = new ArrayList<>();

                for (GenericWorkItem wi : workItems) {
                    if (wi.isEpic()) {
                        epics.add(wi);
                    } else if (wi.isFeature()) {
                        if (wi.isInBacklog()) {
                            featuresInBacklog.add(wi);
                        } else {
                            List<GenericWorkItem> features = epicsFeatures.get(wi.getParentId());
                            if (features == null) {
                                features = new ArrayList<>();
                                epicsFeatures.put(wi.getParentId(), features);
                            }
                            features.add(wi);
                        }
                    } else {
                        // Backlog Item
                        if (wi.isInBacklog()) {
                            itemsInBacklog.add(wi);
                        } else {
                            List<GenericWorkItem> items = featuresItems.get(wi.getParentId());
                            if (items == null) {
                                items = new ArrayList<>();
                                featuresItems.put(wi.getParentId(), items);
                            }
                            items.add(wi);
                        }
                    }
                }

                if (OctaneConstants.IMPORT_SELECTION_FEATURE.equals(values.get(OctaneConstants.KEY_IMPORT_SELECTION))) {
                    // Import a single Feature, so we don't care about the parent Epic or Backlog.
                    GenericWorkItem feature = null;
                    if (!featuresInBacklog.isEmpty()) {
                        feature = featuresInBacklog.get(0);

                    } else {
                        // One Feature in One Epic
                        String epicId = epics.isEmpty() ? null : epics.get(0).getId();
                        feature = (epicId != null && epicsFeatures.get(epicId) == null) ? null : epicsFeatures.get(epicId).get(0);
                    }

                    if (feature != null ) {
                        rootTasks.add(WorkDrivenPercentCompleteExternalTask.forSummaryTask(new OctaneFeatureExternalTask(feature, featuresItems.get(feature.getId()), wpContext)));
                    }
                } else {
                    // Standard import with Epics/Backlog included.

                    // We always start with Backlog tasks
                    if (!featuresInBacklog.isEmpty() || !itemsInBacklog.isEmpty()) {
                        String parentId = null;
                        if (!itemsInBacklog.isEmpty()) {
                            parentId = itemsInBacklog.get(0).getId();
                        }
                        rootTasks.add(WorkDrivenPercentCompleteExternalTask.forSummaryTask(new OctaneRootBacklogExternalTask(featuresInBacklog, itemsInBacklog, featuresItems, wpContext, parentId)));
                    }

                    // Then the Epics / Features / Backlog Items hierarchy.
                    OctaneUtils.sortWorkItemsByName(epics);

                    for (GenericWorkItem epic : epics) {
                        rootTasks.add(WorkDrivenPercentCompleteExternalTask.forSummaryTask(new OctaneEpicExternalTask(epic, epicsFeatures.get(epic.getId()), featuresItems, wpContext)));
                    }
                }

                break;
        }

        client.signOut(values);

        return new ExternalWorkPlan() {

            @Override
            public List<ExternalTask> getRootTasks() {
                return rootTasks;
            }
        };

    }

    private void loadWorkItemsTasks(List<GenericWorkItem> workItems, ClientPublicAPI client, WorkplanContext wpContext) {
        Map<String, GenericWorkItem> workItemsById = workItems.stream().collect(Collectors.toMap(GenericWorkItem::getId, Function.identity()));

        List<OctaneTask> tasks = client.getTasksByWorkItemIds(workItemsById.keySet());

        for (OctaneTask task: tasks) {
            GenericWorkItem wi = workItemsById.get(task.getStoryId());
            if (wi != null) {
                wi.addTask(task);
            }
        }
    }


    /* Older versions of the Octane Connector don't have so many values ; we'll set default values when they are not set */
    private void setBackwardCompatibleParameters(ValueSet values) {
        if (values.get(OctaneConstants.KEY_PERCENT_COMPLETE) == null) {
            values.put(OctaneConstants.KEY_PERCENT_COMPLETE, OctaneConstants.PERCENT_COMPLETE_WORK);
        }

        if (values.get(OctaneConstants.KEY_IMPORT_SELECTION) == null) {
            values.put(OctaneConstants.KEY_IMPORT_SELECTION, OctaneConstants.IMPORT_SELECTION_RELEASE);
        }

        // In first versions of connector, we only import a Release ID.
        if (values.get(OctaneConstants.KEY_IMPORT_SELECTION_DETAILS) == null) {
            values.put(OctaneConstants.KEY_IMPORT_SELECTION_DETAILS, values.get(OctaneConstants.KEY_RELEASEID));
        }

        // Old connector was grouping stuff by release/sprint, so we keep this as default.
        if(values.get(OctaneConstants.KEY_IMPORT_GROUPS) == null) {
            values.put(OctaneConstants.KEY_IMPORT_GROUPS, OctaneConstants.GROUP_RELEASE);
        }

        if (!values.getBoolean(OctaneConstants.KEY_IMPORT_ITEM_STORIES, false) && !values.getBoolean(OctaneConstants.KEY_IMPORT_ITEM_DEFECTS, false) && !values.getBoolean(OctaneConstants.KEY_IMPORT_ITEM_QUALITY_STORIES, false)) {
            // You shouldn't import nothing, doesn't make sense. Let's import at least User stories, it's the default now & only way in the old connector.
            values.put(OctaneConstants.KEY_IMPORT_ITEM_STORIES, "true");
        }

        if (StringUtils.isBlank(values.get(OctaneConstants.KEY_SHOW_ITEMS_AS_TASKS))) {
            // If this parameter was not set (in old plugin), default value was to insert stories as tasks in the work plan.
            values.put(OctaneConstants.KEY_SHOW_ITEMS_AS_TASKS, "true");
        }

        if (StringUtils.isBlank(values.get(OctaneConstants.KEY_EFFORT_IMPORT))) {
            // If this parameter was not set (in old plugin), default value was to import effort from task and record it under the story/defect owner.
            values.put(OctaneConstants.KEY_EFFORT_IMPORT, OctaneConstants.EFFORT_GROUP_IN_WORKITEM_OWNER);
        }
    }

    private List<Release> getSortedReleases(Set<String> releaseIds, Map<String, Release> releasesMap) {
        List<Release> releases = new LinkedList<>();

        boolean needsToCreateNullRelease = false;

        for (String releaseId : releaseIds) {

            Release release = releasesMap.get(releaseId);

            if (release == null) {
                needsToCreateNullRelease = true;
            } else {
                releases.add(release);
            }
        }

        if (needsToCreateNullRelease) {
            Release nullRelease = new Release();
            nullRelease.setName(Providers.getLocalizationProvider(OctaneIntegrationConnector.class).getConnectorText("WORKPLAN_NO_RELEASE_DEFINED_TASK_NAME"));
            releases.add(0, nullRelease);
        }

        OctaneUtils.sortReleases(releases);

        return releases;
    }

    public LinkedTaskAgileEntityInfo getAgileEntityInfoFromMappingConfiguration(ValueSet values) {
        LinkedTaskAgileEntityInfo info = new LinkedTaskAgileEntityInfo();

        if (values != null) {

            info.setProjectId(values.get(OctaneConstants.KEY_WORKSPACEID));

            String importSelection = values.get(OctaneConstants.KEY_IMPORT_SELECTION);
            String importSelectionDetails = values.get(OctaneConstants.KEY_IMPORT_SELECTION_DETAILS);
            switch (importSelection) {
                case OctaneConstants.IMPORT_SELECTION_EPIC:
                    info.setEpicId(importSelectionDetails);
                    break;
                case OctaneConstants.IMPORT_SELECTION_RELEASE:
                    info.setReleaseId(importSelectionDetails);
                    break;
                case OctaneConstants.IMPORT_SELECTION_FEATURE:
                    info.setFeatureId(importSelectionDetails);
                    break;
            }
        }

        return info;
    }

//    @Override
    public boolean supportTimesheetingAgainstExternalWorkPlan() {
        return true;
    }
    
    @Override
    public String getSSOAuthenticationCookie(ValueSet values, String identifier) {
    	ClientPublicAPI clientP = OctaneClientHelper.setupClientPublicAPI(values);
        return clientP.getSSOAuthenticationCookies(identifier);
    }
}
