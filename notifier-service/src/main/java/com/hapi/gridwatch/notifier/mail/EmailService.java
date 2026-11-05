package com.hapi.gridwatch.notifier.mail;


import com.hapi.gridwatch.notifier.dto.WorkOrderEvent;
import com.hapi.gridwatch.notifier.types.WorkOrderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final MailSender mailSender;
    private final String from;
    private final String to;

    public EmailService(MailSender mailSender, @Value("${notifier.from}") String from, @Value("${notifier.to}") String to) {
        this.mailSender = mailSender;
        this.from = from;
        this.to = to;
    }

    public void sendWorkOrder(WorkOrderEvent event) {
        String  subject = "work order " +
                event.workOrderId() +
                (event.workOrderType() == WorkOrderType.CLOSED ? " closed at " : " created at " ) +
                event.substationId();

        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setFrom(from);
        simpleMailMessage.setTo(to);
        simpleMailMessage.setSubject(subject);
        simpleMailMessage.setText(event.reason());

        mailSender.send(simpleMailMessage);

        logger.info(
                "{} Email sent for work order {}", event.workOrderType(), event.workOrderId()
        );
    }
}
