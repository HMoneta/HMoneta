package fan.summer.hmoneta.service.unifi.dto.SiteManager;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * UniFi Site Manager 主机列表响应
 *
 * @author phoebej
 * @version 1.00
 * @Date 2026/1/18
 */
@Data
public class SiteManagerHostResp {
    private List<HostData> data;
    private Integer httpStatusCode;
    private String traceId;

    @Data
    public static class HostData {
        private String id;
        private String hardwareId;
        private String type;
        private String ipAddress;
        private Boolean owner;
        private Boolean isBlocked;
        private String registrationTime;
        private String lastConnectionStateChange;
        private String latestBackupTime;
        private UserData userData;
        private ReportedState reportedState;
    }

    @Data
    public static class UserData {
        private List<String> apps;
        private List<ConsoleGroupMember> consoleGroupMembers;
        private List<String> controllers;
        private String email;
        private Features features;
        private String fullName;
        private String localId;
        private Map<String, List<String>> permissions;
        private String role;
        private String roleId;
        private String status;
    }

    @Data
    public static class ConsoleGroupMember {
        private String mac;
        private String role;
        private RoleAttributes roleAttributes;
        private Integer sysId;
    }

    @Data
    public static class RoleAttributes {
        private Map<String, ApplicationAccess> applications;
        private List<String> candidateRoles;
        private String connectedState;
        private String connectedStateLastChanged;
    }

    @Data
    public static class ApplicationAccess {
        private Boolean owned;
        private Boolean required;
        private Boolean supported;
    }

    @Data
    public static class Features {
        private Boolean deviceGroups;
        private Floorplan floorplan;
        private Boolean manageApplications;
        private Boolean notifications;
        private Webrtc webrtc;
    }

    @Data
    public static class Floorplan {
        private Boolean canEdit;
        private Boolean canView;
    }

    @Data
    public static class Webrtc {
        private Boolean iceRestart;
        private Boolean mediaStreams;
        private Boolean mediaStreamsAV1;
        private Boolean mediaStreamsH265;
        private Boolean twoWayAudio;
    }

    @Data
    public static class ReportedState {
        private String anonid;
        private List<AppInfo> apps;
        private AutoUpdate autoUpdate;
        private List<String> availableChannels;
        private List<String> consolesOnSameLocalNetwork;
        private String controllerUuid;
        private List<Controller> controllers;
        private Object deviceErrorCode;
        private String deviceState;
        private Long deviceStateLastChanged;
        private String directConnectDomain;
        private DeviceFeatures features;
        private FirmwareUpdate firmwareUpdate;
        private Hardware hardware;
        private Integer hostType;
        private String hostname;
        private InternetIssues5min internetIssues5min;
        private String ip;
        private List<String> ipAddrs;
        private Boolean isStacked;
        private Location location;
        private String mac;
        private Integer mgmtPort;
        private String name;
        private String releaseChannel;
        private String state;
        private String timezone;
        private Uidb uidb;
        private List<String> unadoptedUnifiOSDevices;
        private String version;
        private List<Wan> wans;
    }

    @Data
    public static class AppInfo {
        private String controllerStatus;
        private AppFeatures features;
        private List<String> flags;
        private IdentityFeatures identityFeatures;
        private IdentityState identityState;
        private Boolean isConfigured;
        private Boolean isInstalled;
        private Boolean isRunning;
        private String name;
        private Integer port;
        private List<String> prefetch;
        private Integer swaiVersion;
        private String type;
        private String uiIcon;
        private String uiVersion;
        private String version;
    }

    @Data
    public static class AppFeatures {
        private Boolean apiIntegration;
        private Boolean assignmentFlags;
        private Boolean driveUIComponentForUserConsolidation;
        private Boolean fetchServiceStatusFromAccess;
        private Boolean identityCertificateDistribution;
        private Boolean supportAPIGetUcsUpdateConfig;
        private Boolean supportAPIKeysMigration;
        private Boolean supportFeatures;
        private Boolean upgradeUcs;
        private Boolean userConsolidation;
        private Boolean userConsolidationViaGlobalFunc;
    }

    @Data
    public static class IdentityFeatures {
        private Integer hostingIdentityHubScore;
        private Boolean standard;
        private Boolean ucs;
        private Boolean ucsAgent;
        private Boolean ucsRemoteAccessViaUOS;
        private Boolean unifiedAdminsUsersPage;
    }

    @Data
    public static class IdentityState {
        private Boolean enterpriseActivated;
        private Boolean migrationNeeded;
        private String migrationStatus;
        private String migrationStatusMessage;
        private Boolean organizationManaged;
        private Boolean standardActivated;
        private Boolean ucsActivated;
        private Boolean ucsAgentActivated;
        private String ucsAgentVersion;
        private Boolean ucsSetuped;
        private String ucsVersion;
        private Boolean usersOrAdminsExist;
    }

    @Data
    public static class AutoUpdate {
        private Boolean includeApplications;
        private Object preferencesPrompt;
        private UpdateSchedule schedule;
    }

    @Data
    public static class UpdateSchedule {
        private String frequency;
        private Integer hour;
    }

    @Data
    public static class Controller {
        private Boolean abridged;
        private String controllerStatus;
        private Fabric fabric;
        private ControllerFeatures features;
        private Map<String, Object> handledRequirements;
        private Boolean initialDeviceListSynced;
        private String installState;
        private List<IntegrationApi> integrationApis;
        private Boolean isConfigured;
        private Boolean isInstalled;
        private Boolean isRunning;
        private String name;
        private Integer port;
        private List<String> prefetch;
        private String releaseChannel;
        private Boolean required;
        private String state;
        private String status;
        private String statusMessage;
        private Integer swaiVersion;
        private String type;
        private String uiVersion;
        private List<String> unadoptedDevices;
        private Boolean updatable;
        private Object updateAvailable;
        private Integer updateProgress;
        private UpdateSchedule updateSchedule;
        private String version;
        private String versionRaw;
        private String httpBridgeUri;
        private String tcpBridgeUri;
        private String wsBridgeUri;
        private Boolean isGeofencingEnabled;
        private Boolean limitedMode;
        private Integer restorePercentage;
        private List<String> syslogCategories;
    }

    @Data
    public static class Fabric {
        private Long configLastUpdated;
    }

    @Data
    public static class ControllerFeatures {
        private Boolean alarmManager;
        private Boolean fabricSnapshot;
        private String armMode;
        private Boolean stackable;
    }

    @Data
    public static class IntegrationApi {
        private String apiDocsLocation;
        private String name;
        private String version;
    }

    @Data
    public static class DeviceFeatures {
        private Boolean alarmManager;
        private Alarms alarms;
        private Boolean apiIntegration;
        private Applications applications;
        private Boolean captiveProxy;
        private Cloud cloud;
        private Boolean cloudBackup;
        private CoLocated coLocated;
        private Boolean customSmtpServer;
        private DeviceList deviceList;
        private Boolean directRemoteConnection;
        private Boolean hasBezel;
        private Boolean hasGateway;
        private Boolean hasLCM;
        private Boolean hasLED;
        private Identity identity;
        private InfoApis infoApis;
        private Boolean isAutomaticFailoverAvailable;
        private Boolean mfa;
        private Boolean mspBridgeModesSupported;
        private Boolean multiplePoolsSupport;
        private Boolean netInAppBackupSupport;
        private Boolean notifications;
        private Boolean sharedTokens;
        private Boolean snmpConfig;
        private Boolean supportForm;
        private Boolean syslog;
        private Boolean teleport;
        private String teleportState;
        private Boolean uidService;
        private Updates updates;
        private Boolean upsPairing;
    }

    @Data
    public static class Alarms {
        private Boolean triggersWithPrecondition;
    }

    @Data
    public static class Applications {
        private Access access;
    }

    @Data
    public static class Access {
        private Boolean mspPlayback;
    }

    @Data
    public static class Cloud {
        private Boolean applicationEvents;
        private Boolean applicationEventsHttp;
        private Boolean ucp4GuestConnection;
        private Boolean ucp4OrganizationConnection;
    }

    @Data
    public static class CoLocated {
        private Boolean primary;
        private Boolean shadow;
    }

    @Data
    public static class DeviceList {
        private Boolean autolinkDevices;
        private Boolean partialUpdates;
        private Boolean ucp4Events;
    }

    @Data
    public static class Identity {
        private Integer hostingIdentityHubScore;
        private Boolean standard;
        private Boolean ucs;
        private Boolean ucsAgent;
        private Boolean ucsRemoteAccessViaUOS;
        private Boolean unifiedAdminsUsersPage;
    }

    @Data
    public static class InfoApis {
        private Boolean firmwareUpdate;
    }

    @Data
    public static class Updates {
        private Boolean applicationReleaseChannels;
        private Boolean applicationSchedules;
    }

    @Data
    public static class FirmwareUpdate {
        private String latestAvailableVersion;
    }

    @Data
    public static class Hardware {
        private String bom;
        private String cpuId;
        private String debianCodename;
        private String firmwareVersion;
        private Integer hwrev;
        private String mac;
        private String name;
        private String qrid;
        private String reboot;
        private String serialno;
        private String shortname;
        private String subtype;
        private Integer sysid;
        private String upgrade;
        private String uuid;
    }

    @Data
    public static class InternetIssues5min {
        private List<Period> periods;
    }

    @Data
    public static class Period {
        private Integer index;
    }

    @Data
    public static class Location {
        private String lat;
        private String longVal;
        private String radius;
        private String text;
    }

    @Data
    public static class Uidb {
        private String guid;
        private String id;
        private UidbImages images;
    }

    @Data
    public static class UidbImages {
        private String blueprint;
        private String blueprintDark;
        private String defaultImg;
        private String mobileConnection;
        private String mobileInternetConnected;
        private String mobileNoInternet;
        private String nopadding;
        private String rack;
        private String topology;
    }

    @Data
    public static class Wan {
        private String associatedInterface;
        private Boolean enabled;
        private String interfaceName;
        private String ipv4;
        private String mac;
        private Boolean plugged;
        private Integer port;
        private String type;
    }
}
