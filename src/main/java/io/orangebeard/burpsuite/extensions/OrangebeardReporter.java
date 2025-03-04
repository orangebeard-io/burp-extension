package io.orangebeard.burpsuite.extensions;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.extension.Extension;
import burp.api.montoya.extension.ExtensionUnloadingHandler;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.scanner.Scanner;
import burp.api.montoya.scanner.audit.AuditIssueHandler;
import burp.api.montoya.scanner.audit.issues.AuditIssue;
import io.orangebeard.client.OrangebeardProperties;
import io.orangebeard.client.entity.alerting.AlertRunStatus;
import io.orangebeard.client.entity.alerting.Tool;
import io.orangebeard.client.entity.alerting.security.Evidence;
import io.orangebeard.client.entity.alerting.security.FinishSecurityAlertRun;
import io.orangebeard.client.entity.alerting.security.ReportSecurityAlert;
import io.orangebeard.client.entity.alerting.security.StartSecurityAlertRun;
import io.orangebeard.client.v3.OrangebeardAsyncV3Client;

import java.time.ZonedDateTime;
import java.util.UUID;

import static io.orangebeard.burpsuite.extensions.util.Converters.toEvidence;
import static io.orangebeard.burpsuite.extensions.util.Converters.toReportSecurityAlert;

@SuppressWarnings(value = "unused")
public class OrangebeardReporter implements BurpExtension {

    private OrangebeardProperties config;
    private OrangebeardAsyncV3Client orangebeardClient;
    private UUID alertRunUUID;
    private Logging burpLogging;

    @Override
    public void initialize(MontoyaApi montoyaApi) {
        burpLogging = montoyaApi.logging();

        if(System.getProperty("orangebeard") == null || !System.getProperty("orangebeard").equals("true")) {
            burpLogging.raiseInfoEvent("Orangebeard plugin is present, but reporting is not enabled.");
            return;
        }

        burpLogging.raiseInfoEvent("Orangebeard reporting enabled.");

        config = new OrangebeardProperties();
        orangebeardClient = new OrangebeardAsyncV3Client(config);

        Scanner scanner = montoyaApi.scanner();
        Extension extension = montoyaApi.extension();

        extension.setName("Orangebeard reporter");
        scanner.registerAuditIssueHandler(new OrangebeardAuditIssueHandler());
        extension.registerUnloadingHandler(new OrangebeardUnloadingHandler());
    }

    private class OrangebeardAuditIssueHandler implements AuditIssueHandler {
        public OrangebeardAuditIssueHandler() {
            if (config.getTestRunUUID() != null) {
                alertRunUUID = config.getTestRunUUID();
            } else {
                StartSecurityAlertRun startRun = new StartSecurityAlertRun(
                        config.getTestSetName(),
                        config.getDescription(),
                        Tool.BURPSUITE,
                        ZonedDateTime.now(),
                        config.getAttributes()
                );
                alertRunUUID = orangebeardClient.startAlertRun(startRun);
            }
        }

        @Override
        public void handleNewAuditIssue(AuditIssue auditIssue) {
            try {
                String url = "";
                Evidence evidence = null;
                if (!auditIssue.requestResponses().isEmpty()) {
                    url = auditIssue.requestResponses().get(auditIssue.requestResponses().size() - 1).request().url();
                    HttpRequestResponse lastRequest = auditIssue.requestResponses().get(auditIssue.requestResponses().size() - 1);

                    evidence = toEvidence(lastRequest);
                }

                ReportSecurityAlert alert = toReportSecurityAlert(alertRunUUID, url, auditIssue, evidence);
                orangebeardClient.reportAlert(alert);
            } catch (Exception e) {
                burpLogging.raiseErrorEvent("Failed to report issue to Orangebeard: " + e.getMessage());
                System.err.println(e.getMessage());
            }
        }
    }

    private class OrangebeardUnloadingHandler implements ExtensionUnloadingHandler {

        @Override
        public void extensionUnloaded() {
            burpLogging.raiseInfoEvent("Finishing Orangebeard alert run.");
            orangebeardClient.finishAlertRun(new FinishSecurityAlertRun(alertRunUUID, AlertRunStatus.COMPLETED, ZonedDateTime.now()));
        }
    }
}
