/*
 * Copyright Siemens AG, 2016-2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.mail;

import com.google.common.collect.Sets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.common.utils.BackendUtils;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.ClearingRequestEmailTemplate;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.users.User;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Arrays;
import java.util.IllegalFormatException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.*;

import static org.eclipse.sw360.datahandler.common.CommonUtils.isNullEmptyOrWhitespace;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;

/**
 * Provides the possiblity to send mail from SW360
 *
 * @author birgit.heydenreich@tngtech.com
 */
public class MailUtil extends BackendUtils {

    private static final Logger log = LogManager.getLogger(MailUtil.class);

    // Asynchronous mail service executor options
    private static final int MAIL_ASYNC_SEND_THREAD_LIMIT = 1;
    private static final int MAIL_ASYNC_SEND_QUEUE_LIMIT = 1000;
    private static final String NEW_CLEARING_REQUEST_EMAIL_TEMPLATE_FILE = "/NewClearingRequestEmailTemplate.html";
    private static final String UPDATE_CLEARING_REQUEST_EMAIL_TEMPLATE_FILE = "/UpdateClearingRequestEmailTemplate.html";
    private static final String UPDATE_PROJECT_WITH_CR_EMAIL_TEMPLATE_FILE = "/UpdateProjectWithCREmailTemplate.html";
    private static final String NEW_COMMENT_IN_CR_EMAIL_HTML_TEMPLATE_FILE = "/NewCommentInCREmailTemplate.html";
    private static final String CLOSED_OR_REJECTED_CR_EMAIL_HTML_TEMPLATE_FILE = "/ClosedOrRejectedCREmailTemplate.html";
    private static final String NEW_CR_EMAIL_HTML_TEMPLATE = SW360Utils.dropCommentedLine(MailUtil.class, NEW_CLEARING_REQUEST_EMAIL_TEMPLATE_FILE);
    private static final String UPDATE_CR_EMAIL_HTML_TEMPLATE = SW360Utils.dropCommentedLine(MailUtil.class, UPDATE_CLEARING_REQUEST_EMAIL_TEMPLATE_FILE);
    private static final String UPDATE_PROJECT_WITH_CR_EMAIL_HTML_TEMPLATE = SW360Utils.dropCommentedLine(MailUtil.class, UPDATE_PROJECT_WITH_CR_EMAIL_TEMPLATE_FILE);
    private static final String NEW_COMMENT_IN_CR_EMAIL_HTML_TEMPLATE = SW360Utils.dropCommentedLine(MailUtil.class, NEW_COMMENT_IN_CR_EMAIL_HTML_TEMPLATE_FILE);
    private static final String CLOSED_OR_REJECTED_CR_EMAIL_HTML_TEMPLATE = SW360Utils.dropCommentedLine(MailUtil.class, CLOSED_OR_REJECTED_CR_EMAIL_HTML_TEMPLATE_FILE);

    private static ExecutorService mailExecutor;
    private Session session;

    private String from;
    private String host;
    private String port;
    private String isAuthenticationNecessary;
    private String enableStarttls;
    private String login;
    private String password;
    private String enableSsl;
    private String enableDebug;
    private String supportMailAddress;

    public MailUtil() {
        mailExecutor = fixedThreadPoolWithQueueSize(MAIL_ASYNC_SEND_THREAD_LIMIT, MAIL_ASYNC_SEND_QUEUE_LIMIT);
        setBasicProperties();
        setSession();
    }

    private static ExecutorService fixedThreadPoolWithQueueSize(int nThreads, int queueSize) {
        // ThreadPoolExecutor.AbortPolicy is used as default which throws RejectedExecutionException
        return new ThreadPoolExecutor(nThreads, nThreads, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueSize, true));
    }

    private void setBasicProperties() {
        from = loadedProperties.getProperty("MailUtil_from", "__No_Reply__@sw360.org");
        host = loadedProperties.getProperty("MailUtil_host", "");
        port = loadedProperties.getProperty("MailUtil_port", "25");
        enableStarttls = loadedProperties.getProperty("MailUtil_enableStarttls", "false");
        enableSsl = loadedProperties.getProperty("MailUtil_enableSsl", "false");
        isAuthenticationNecessary = loadedProperties.getProperty("MailUtil_isAuthenticationNecessary", "true");
        login = loadedProperties.getProperty("MailUtil_login", "");
        password = loadedProperties.getProperty("MailUtil_password", "");
        enableDebug = loadedProperties.getProperty("MailUtil_enableDebug", "false");
        supportMailAddress = loadedProperties.getProperty("MailUtil_supportMailAddress","");
    }

    private void setSession() {
        // Get system properties
        Properties properties = System.getProperties();

        // Setup mail server
        properties.setProperty("mail.transport.protocol", "smtp");
        properties.setProperty("mail.smtp.host", host);
        properties.setProperty("mail.smtp.port", port);
        properties.setProperty("mail.smtp.auth", isAuthenticationNecessary);
        properties.setProperty("mail.smtp.starttls.enable", enableStarttls);
        properties.setProperty("mail.smtp.ssl.enable", enableSsl);

        properties.setProperty("mail.debug", enableDebug);

        if (!"false".equals(isAuthenticationNecessary)) {
            Authenticator auth = new SMTPAuthenticator(login, password);
            session = Session.getInstance(properties, auth);
        } else {
            session = Session.getDefaultInstance(properties);
        }
    }

    public void sendClearingMail(ClearingRequestEmailTemplate template, String subjectNameInPropertiesFile, Map<String, String> recipients, String... textParameters) {
        MimeMessage messageWithSubjectAndText;
        messageWithSubjectAndText = makeHtmlMessageWithSubjectAndText(template, subjectNameInPropertiesFile, textParameters);
        if (!CommonUtils.isNullOrEmptyMap(recipients)) {
            String requestingUser = recipients.get(ClearingRequest._Fields.REQUESTING_USER.toString());
            if (isMailWantedBy(requestingUser, SW360Utils.notificationPreferenceKey(SW360Constants.NOTIFICATION_CLASS_CLEARING_REQUEST, ClearingRequest._Fields.REQUESTING_USER.toString()))
                && CommonUtils.isNotNullEmptyOrWhitespace(requestingUser)) {
                sendMailWithSubjectAndText(String.join(",", recipients.values()), messageWithSubjectAndText);
            } else {
                sendMailWithSubjectAndText(recipients.get(ClearingRequest._Fields.CLEARING_TEAM.toString()), messageWithSubjectAndText);
            }
        }
    }

    public void sendMail(String recipient, String subjectNameInPropertiesFile, String textNameInPropertiesFile, String notificationClass, String roleName, String ... textParameters) {
        sendMail(recipient, subjectNameInPropertiesFile, textNameInPropertiesFile, notificationClass, roleName, true, textParameters);
    }
    public void sendMail(String recipient, String subjectNameInPropertiesFile, String textNameInPropertiesFile, String notificationClass, String roleName, boolean checkWantsNotifications, String ... textParameters) {
        sendMail(Sets.newHashSet(recipient), null, subjectNameInPropertiesFile, textNameInPropertiesFile, notificationClass, roleName, checkWantsNotifications, textParameters);
    }

    public void sendMail(Set<String> recipients, String excludedRecipient, String subjectNameInPropertiesFile, String textNameInPropertiesFile, String notificationClass, String roleName, String... textParameters) {
        sendMail(recipients, excludedRecipient, subjectNameInPropertiesFile, textNameInPropertiesFile, notificationClass, roleName, true, textParameters);
    }

    private void sendMail(Set<String> recipients, String excludedRecipient, String subjectNameInPropertiesFile, String textNameInPropertiesFile, String notificationClass, String roleName, boolean checkWantsNotifications, String... textParameters) {
        MimeMessage messageWithSubjectAndText = makeMessageWithSubjectAndText(subjectNameInPropertiesFile, textNameInPropertiesFile, textParameters);
        for (String recipient : nullToEmptySet(recipients)) {
            if(!isNullEmptyOrWhitespace(recipient)
                    && !recipient.equals(excludedRecipient)
                    && (!checkWantsNotifications || isMailWantedBy(recipient, SW360Utils.notificationPreferenceKey(notificationClass, roleName)))) {
                sendMailWithSubjectAndText(recipient, messageWithSubjectAndText);
            }
        }
    }

    private boolean isMailWantedBy(String userEmail, String notificationPreferenceKey){
        User user;
        try {
            user = (new ThriftClients()).makeUserClient().getByEmail(userEmail);
        } catch (TException e){
            log.info("Problem fetching user:" + e);
            return false;
        }
        if(user != null) {
            SW360Utils.initializeMailNotificationsPreferences(user);
            return user.isWantsMailNotification() && user.getNotificationPreferences().getOrDefault(notificationPreferenceKey, Boolean.FALSE) ;
        }
        return false;
    }

    private boolean isMailingEnabledAndValid() {
        if ("".equals(host)) {
            return false; //e-mailing is disabled
        }
        if (!"false".equals(isAuthenticationNecessary) && "".equals(login)) {
            log.error("Cannot send emails: authentication necessary, but login is not set.");
            return false;
        }
        return true;
    }

    private MimeMessage makeHtmlMessageWithSubjectAndText(ClearingRequestEmailTemplate template, String subjectKeyInPropertiesFile, String ... textParameters) {
        MimeMessage message = new MimeMessage(session);
        String mainContentFormat = "";
        String subject = loadedProperties.getProperty(subjectKeyInPropertiesFile, "");
        switch (template) {
        case UPDATED:
            mainContentFormat = UPDATE_CR_EMAIL_HTML_TEMPLATE;
            subject = String.format(subject, textParameters[1], textParameters[3]);
            break;

        case PROJECT_UPDATED:
            mainContentFormat = UPDATE_PROJECT_WITH_CR_EMAIL_HTML_TEMPLATE;
            subject = String.format(subject, textParameters[1], textParameters[2]);
            break;

        case NEW_COMMENT:
            mainContentFormat = NEW_COMMENT_IN_CR_EMAIL_HTML_TEMPLATE;
            subject = String.format(subject, textParameters[1], textParameters[2]);
            break;

        case REJECTED:
        case CLOSED:
            mainContentFormat = CLOSED_OR_REJECTED_CR_EMAIL_HTML_TEMPLATE;
            subject = String.format(subject, textParameters[0], textParameters[1], textParameters[2]);
            break;

        case NEW:
            mainContentFormat = NEW_CR_EMAIL_HTML_TEMPLATE;
            subject = String.format(subject, textParameters[1], textParameters[3]);
            break;

        default:
            break;
        }

        StringBuilder text = new StringBuilder();
        try {
            String formattedContent = String.format(mainContentFormat, (Object[]) textParameters);
            text.append(formattedContent);
        } catch (IllegalFormatException e) {
            log.error(String.format("Could not format notification email content for key %s ", subjectKeyInPropertiesFile), e);
            text.append(mainContentFormat);
        }
        try {
            message.setSubject(subject);
            message.setContent(text.toString(), "text/html");
        } catch (MessagingException mex) {
            log.error(mex.getMessage());
        }

        return message;
    }

    private MimeMessage makeMessageWithSubjectAndText(String subjectKeyInPropertiesFile, String textKeyInPropertiesFile, String ... textParameters) {
        MimeMessage message = new MimeMessage(session);
        String subject = loadedProperties.getProperty(subjectKeyInPropertiesFile, "");

        StringBuilder text = new StringBuilder();
        text.append(loadedProperties.getProperty("defaultBegin", ""));
        String mainContentFormat = loadedProperties.getProperty(textKeyInPropertiesFile, "");

        try {
            String formattedContent = String.format(mainContentFormat, (Object[]) textParameters);
            text.append(formattedContent);
        } catch (IllegalFormatException e) {
            log.error(String.format("Could not format notification email content for keys %s and %s", subjectKeyInPropertiesFile, textKeyInPropertiesFile), e);
            text.append(mainContentFormat);
        }
        text.append(loadedProperties.getProperty("defaultEnd", ""));
        if (!supportMailAddress.equals("")) {
            text.append(loadedProperties.getProperty("unsubscribeNoticeBefore", ""));
            text.append(" ");
            text.append(supportMailAddress);
            text.append(loadedProperties.getProperty("unsubscribeNoticeAfter", ""));
        }
        try {
            message.setSubject(subject);
            message.setText(text.toString());
        } catch (MessagingException mex) {
            log.error(mex.getMessage());
        }

        return message;
    }

    private void sendMailWithSubjectAndText(String recipient, MimeMessage message) {
        try {
            message.setFrom(new InternetAddress(from));
            if (recipient.indexOf(",") > 0) {
                message.setRecipients(Message.RecipientType.TO, recipient);
            } else {
                message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            }
            if (isMailingEnabledAndValid()) {
                sendMailAsync(message);
            } else {
                writeMessageToLog(message);
            }
        } catch (MessagingException mex) {
            log.error(mex.getMessage(), mex);
        }
    }

    private void writeMessageToLog(MimeMessage message) {
        try {
            log.info(String.format("E-Mail message dumped to log, because mailing is not configured [correctly]:\n"+
                            "From: %s\n"+
                            "To: %s\n"+
                            "Subject: %s\n"+
                            "Text: %s\n",
                    Arrays.toString(message.getFrom()),
                    Arrays.toString(message.getRecipients(Message.RecipientType.TO)),
                    message.getSubject(),
                    message.getContent()
            ));
        } catch (MessagingException | IOException e) {
            log.error("Cannot dump E-mail message to log", e);
        }
    }

    private void sendMailAsync(MimeMessage message) {
        try {
            mailExecutor.submit(createMailTask(message));
        } catch (RejectedExecutionException e) {
            log.error("Max queue size of asynchronous mail service executor reached", e);
        }
    }

    private Runnable createMailTask(MimeMessage message) {
        Runnable mailTask = () -> {
            try {
                String recipient = Arrays.toString(message.getRecipients(Message.RecipientType.TO));
                log.info("Send asynchronous E-Mail to recipient " + recipient);
                Transport.send(message);
                log.info("Sent asynchronous message to " + recipient + " successfully");
            } catch (MessagingException e) {
                log.error("Could not sent E-Mail notification via SMTP " + host, e);
            }
        };
        return mailTask;
    }

    private class SMTPAuthenticator extends Authenticator {
        private PasswordAuthentication authentication;

        public SMTPAuthenticator(String login, String password) {
            authentication = new PasswordAuthentication(login, password);
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return authentication;
        }
    }
}
