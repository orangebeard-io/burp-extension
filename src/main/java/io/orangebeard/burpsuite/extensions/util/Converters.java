package io.orangebeard.burpsuite.extensions.util;

import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.api.montoya.scanner.audit.issues.AuditIssueConfidence;
import burp.api.montoya.scanner.audit.issues.AuditIssueSeverity;
import io.orangebeard.client.entity.Attribute;
import io.orangebeard.client.entity.alerting.Severity;
import io.orangebeard.client.entity.alerting.security.Confidence;
import io.orangebeard.client.entity.alerting.security.Evidence;
import io.orangebeard.client.entity.alerting.security.ReportSecurityAlert;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Converters {

    private static final String KB_BASE_URL = "https://portswigger.net/kb/issues/";

    private Converters() {
    }

    public static Evidence toEvidence(HttpRequestResponse reqResp) {
        return new Evidence(
                reqResp.request().url(),
                reqResp.request().method(),
                reqResp.response().statusCode(),
                reqResp.request().headers().stream()
                        .collect(Collectors.toMap(HttpHeader::name, HttpHeader::value, (existingValue, newValue) -> newValue)),
                reqResp.request().bodyToString().replace("\u0000", ""),
                reqResp.response().headers().stream()
                        .collect(Collectors.toMap(HttpHeader::name, HttpHeader::value, (existingValue, newValue) -> newValue)),
                reqResp.response().bodyToString().replace("\u0000", ""),
                null
        );
    }

    public static ReportSecurityAlert toReportSecurityAlert(UUID alertRunUUID, String url, AuditIssue issue, Evidence evidence) {
        String hexTypeIndex = String.format("%08x", issue.definition().typeIndex());
        Set<Attribute> attrs = new HashSet<>();
        attrs.add(new Attribute("KBURL", KB_BASE_URL + hexTypeIndex));

        return new ReportSecurityAlert(
                alertRunUUID,
                "burp_" + issue.definition().typeIndex(),
                url + "||" + issue.detail(),
                issue.name(),
                issue.remediation() + "\n\n" + issue.definition().background(),
                attrs,
                mapConfidence(issue.confidence()),
                evidence,
                mapSeverity(issue.severity())
        );
    }

    private static Severity mapSeverity(AuditIssueSeverity burpSeverity) {
        return switch (burpSeverity) {
            case HIGH -> Severity.HIGH;
            case MEDIUM -> Severity.MEDIUM;
            case LOW -> Severity.LOW;
            case INFORMATION -> Severity.INFO;
            case FALSE_POSITIVE -> Severity.FALSE_POSITIVE;
        };
    }

    private static Confidence mapConfidence(AuditIssueConfidence burpConfidence) {
        return switch (burpConfidence) {
            case CERTAIN -> Confidence.CERTAIN;
            case FIRM -> Confidence.FIRM;
            case TENTATIVE -> Confidence.TENTATIVE;
        };
    }
}
